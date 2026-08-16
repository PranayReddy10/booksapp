package com.jntuh.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.jntuh.item.TaskItem;
import com.jntuh.service.AlarmReceiver;

import java.util.Calendar;
import java.util.List;

/**
 * Turns a {@link TaskItem} into a real OS alarm, and back again.
 *
 * <p>Deliberately AlarmManager and not WorkManager: WorkManager batches and defers under
 * Doze, so a 6:00 AM reminder can land at 6:40 or not at all. {@code setAlarmClock} is
 * Doze-exempt and survives battery saver, which is the whole point of the feature.
 * WorkManager still runs a daily sweep as a safety net — see ReminderSweepWorker.
 */
public final class ReminderScheduler {

    private static final String TAG = "ReminderScheduler";

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_IS_ALARM = "is_alarm";

    private ReminderScheduler() {
    }

    private static AlarmManager alarmManager(Context context) {
        return (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * The row id is the request code, so re-scheduling a task always replaces its own
     * alarm rather than stacking a second one.
     */
    private static PendingIntent pendingIntent(Context context, TaskItem task, int flags) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_FIRE);
        intent.putExtra(EXTRA_TASK_ID, task.getId());
        intent.putExtra(EXTRA_IS_ALARM, task.isAlarm());
        // A distinct data URI keeps PendingIntents for different tasks from colliding,
        // since extras alone are not part of the equality check.
        intent.setData(android.net.Uri.parse("jntuhtask://" + task.getId()));

        int mutability = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE : 0;
        return PendingIntent.getBroadcast(context.getApplicationContext(),
                (int) task.getId(), intent, flags | mutability);
    }

    /**
     * Schedules (or re-schedules) the reminder for a task. Silently does nothing when the
     * task is done, has no reminder, or its trigger time has already passed.
     */
    public static void schedule(Context context, TaskItem task) {
        cancel(context, task);

        if (task.isDone() || task.isDeleted() || !task.wantsReminder()) {
            return;
        }

        long triggerAt = task.getTriggerAt();
        if (triggerAt <= System.currentTimeMillis()) {
            // A repeating task whose current slot has passed rolls forward to the next one.
            if (task.isRepeating()) {
                long next = nextOccurrence(task.getDueAt(), task.getRepeatMode());
                if (next > 0) {
                    task.setDueAt(next);
                    triggerAt = task.getTriggerAt();
                } else {
                    return;
                }
            } else {
                return;
            }
        }

        scheduleAt(context, task, triggerAt);
    }

    /** Schedules the given task at an explicit time — also used by snooze. */
    public static void scheduleAt(Context context, TaskItem task, long triggerAt) {
        AlarmManager am = alarmManager(context);
        if (am == null) return;

        PendingIntent pi = pendingIntent(context, task, PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            if (task.isAlarm() && AlarmPermissions.canScheduleExact(context)) {
                // setAlarmClock is the strongest guarantee the platform offers: exempt
                // from Doze, shows the system alarm icon, and survives battery saver.
                Intent showIntent = new Intent(context, com.jntuh.books.TasksActivity.class);
                int mutability = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_IMMUTABLE : 0;
                PendingIntent showPi = PendingIntent.getActivity(context, (int) task.getId(),
                        showIntent, PendingIntent.FLAG_UPDATE_CURRENT | mutability);
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, showPi), pi);

            } else if (AlarmPermissions.canScheduleExact(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }

            } else {
                // No exact-alarm permission: still fire, just without a precision promise.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            }
        } catch (SecurityException e) {
            // Permission revoked between the check and the call — degrade instead of crash.
            Log.w(TAG, "Exact alarm denied, falling back to inexact", e);
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context context, TaskItem task) {
        AlarmManager am = alarmManager(context);
        if (am == null) return;
        PendingIntent pi = pendingIntent(context, task, PendingIntent.FLAG_NO_CREATE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    public static void cancel(Context context, long taskId) {
        TaskItem stub = new TaskItem();
        stub.setId(taskId);
        cancel(context, stub);
    }

    /** Pushes a task's reminder out by its snooze interval. */
    public static void snooze(Context context, TaskItem task) {
        long minutes = task.getSnoozeMinutes() > 0 ? task.getSnoozeMinutes() : 10;
        scheduleAt(context, task, System.currentTimeMillis() + minutes * 60_000L);
    }

    /**
     * Rebuilds every alarm from the database. Alarms do not survive a reboot, a force-stop
     * or an app update, so this runs on BOOT_COMPLETED, MY_PACKAGE_REPLACED, time changes,
     * and once a day from the sweep worker.
     */
    public static void rescheduleAll(Context context) {
        DatabaseHandler db = new DatabaseHandler(context.getApplicationContext());
        List<TaskItem> tasks = db.getTasksToSchedule();
        for (TaskItem task : tasks) {
            long before = task.getDueAt();
            schedule(context, task);
            // schedule() may have rolled a lapsed repeating task forward; persist that.
            if (task.getDueAt() != before) {
                db.updateTask(task);
            }
        }
        Log.d(TAG, "Rescheduled " + tasks.size() + " reminders");
    }

    /**
     * The next due time strictly after now for a repeating task, or 0 when the rule cannot
     * advance. Uses Calendar so DST shifts and month lengths are handled by the platform.
     */
    public static long nextOccurrence(long from, int repeatMode) {
        if (repeatMode == TaskItem.REPEAT_ONCE) {
            return 0;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(from);
        long now = System.currentTimeMillis();

        // Guard against a pathological loop if the clock is far ahead of a stale task.
        int guard = 0;
        while (cal.getTimeInMillis() <= now && guard++ < 4000) {
            switch (repeatMode) {
                case TaskItem.REPEAT_DAILY:
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case TaskItem.REPEAT_WEEKDAYS:
                    do {
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                    } while (isWeekend(cal));
                    break;
                case TaskItem.REPEAT_WEEKLY:
                    cal.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case TaskItem.REPEAT_MONTHLY:
                    cal.add(Calendar.MONTH, 1);
                    break;
                default:
                    return 0;
            }
        }
        return cal.getTimeInMillis();
    }

    private static boolean isWeekend(Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    /**
     * Advances a repeating task past the occurrence that just fired and re-arms it.
     * Returns false when the task is finished (one-shot).
     */
    public static boolean advanceAfterFire(Context context, DatabaseHandler db, TaskItem task) {
        if (!task.isRepeating()) {
            return false;
        }
        long next = nextOccurrence(task.getDueAt(), task.getRepeatMode());
        if (next <= 0) {
            return false;
        }
        task.setDueAt(next);
        task.setDone(false);
        task.setCompletedAt(0);
        db.updateTask(task);
        schedule(context, task);
        return true;
    }
}

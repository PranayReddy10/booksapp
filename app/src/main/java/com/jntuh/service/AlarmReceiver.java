package com.jntuh.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.jntuh.item.TaskItem;
import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.ReminderScheduler;
import com.jntuh.util.TaskNotifier;

/**
 * Fired by AlarmManager at a task's reminder time. Runs on the main thread with only a
 * few seconds of budget, so it does the minimum: read the task, post the notification,
 * start the ringer for alarm-type reminders, and roll a repeating task to its next slot.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    public static final String ACTION_FIRE = "com.jntuh.books.action.REMINDER_FIRE";

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1);
        if (taskId < 0) {
            return;
        }

        DatabaseHandler db = new DatabaseHandler(context.getApplicationContext());
        TaskItem task = db.getTask(taskId);

        // Deleted or already ticked off between scheduling and firing.
        if (task == null || task.isDone() || task.isDeleted() || !task.wantsReminder()) {
            return;
        }

        if (task.isAlarm()) {
            // The service posts the alarm notification itself, as its foreground
            // notification. If it cannot start, fall back to the reminder channel —
            // that one carries its own sound and will at least be audible.
            if (!startRinger(context, task)) {
                Log.w(TAG, "Ringer refused to start, degrading to a plain reminder");
                TaskNotifier.showReminder(context, task);
            }
        } else {
            TaskNotifier.showReminder(context, task);
        }

        // Queue up the next occurrence so a repeating task never misses a beat.
        ReminderScheduler.advanceAfterFire(context, db, task);
    }

    private boolean startRinger(Context context, TaskItem task) {
        try {
            Intent service = new Intent(context, AlarmSoundService.class);
            service.setAction(AlarmSoundService.ACTION_START);
            service.putExtra(ReminderScheduler.EXTRA_TASK_ID, task.getId());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Could not start AlarmSoundService", e);
            return false;
        }
    }
}

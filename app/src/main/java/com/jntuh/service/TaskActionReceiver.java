package com.jntuh.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.jntuh.books.R;
import com.jntuh.item.TaskItem;
import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.ReminderScheduler;
import com.jntuh.util.TaskNotifier;

/**
 * Handles the Done / Snooze / Dismiss buttons on a reminder, so the common case never
 * requires opening the app.
 */
public class TaskActionReceiver extends BroadcastReceiver {

    public static final String ACTION_DONE = "com.jntuh.books.action.TASK_DONE";
    public static final String ACTION_SNOOZE = "com.jntuh.books.action.TASK_SNOOZE";
    public static final String ACTION_DISMISS = "com.jntuh.books.action.TASK_DISMISS";

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1);
        String action = intent.getAction();
        if (taskId < 0 || action == null) {
            return;
        }

        DatabaseHandler db = new DatabaseHandler(context.getApplicationContext());
        TaskItem task = db.getTask(taskId);
        if (task == null) {
            return;
        }

        stopRinger(context);
        TaskNotifier.cancel(context, taskId);

        switch (action) {
            case ACTION_DONE:
                markDone(context, db, task);
                Toast.makeText(context, R.string.task_marked_done, Toast.LENGTH_SHORT).show();
                break;

            case ACTION_SNOOZE:
                ReminderScheduler.snooze(context, task);
                Toast.makeText(context,
                        context.getString(R.string.task_snoozed, task.getSnoozeMinutes()),
                        Toast.LENGTH_SHORT).show();
                break;

            case ACTION_DISMISS:
            default:
                // Nothing to persist: the notification is gone and any repeat was already
                // rolled forward when the alarm fired.
                break;
        }
    }

    /**
     * A repeating task is never "finished" — completing it just moves it to its next
     * occurrence. A one-shot task gets flagged done and keeps its completion timestamp
     * so the progress card can count it.
     */
    public static void markDone(Context context, DatabaseHandler db, TaskItem task) {
        if (task.isRepeating()) {
            long next = ReminderScheduler.nextOccurrence(task.getDueAt(), task.getRepeatMode());
            if (next > 0) {
                task.setDueAt(next);
                task.setDone(false);
                task.setCompletedAt(System.currentTimeMillis());
                db.updateTask(task);
                ReminderScheduler.schedule(context, task);
                return;
            }
        }
        task.setDone(true);
        task.setCompletedAt(System.currentTimeMillis());
        db.updateTask(task);
        ReminderScheduler.cancel(context, task);
    }

    private void stopRinger(Context context) {
        try {
            Intent stop = new Intent(context, AlarmSoundService.class);
            stop.setAction(AlarmSoundService.ACTION_STOP);
            context.startService(stop);
        } catch (Exception ignored) {
            // Service was not running — nothing to silence.
        }
    }
}

package com.jntuh.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.jntuh.books.AlarmRingActivity;
import com.jntuh.books.R;
import com.jntuh.books.TasksActivity;
import com.jntuh.item.TaskItem;
import com.jntuh.service.TaskActionReceiver;

/**
 * Builds and posts the two kinds of reminder.
 *
 * <p>The alarm channel is deliberately <em>silent</em>: audio belongs to
 * {@link com.jntuh.service.AlarmSoundService}, which can loop and be stopped by the
 * ring screen. Letting the channel play a sound too would double it up.
 */
public final class TaskNotifier {

    public static final String CHANNEL_REMINDER = "task_reminder";
    public static final String CHANNEL_ALARM = "task_alarm";

    private TaskNotifier() {
    }

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel reminder = new NotificationChannel(CHANNEL_REMINDER,
                context.getString(R.string.task_channel_reminder), NotificationManager.IMPORTANCE_HIGH);
        reminder.setDescription(context.getString(R.string.task_channel_reminder_desc));
        reminder.enableVibration(true);
        reminder.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(reminder);

        NotificationChannel alarm = new NotificationChannel(CHANNEL_ALARM,
                context.getString(R.string.task_channel_alarm), NotificationManager.IMPORTANCE_HIGH);
        alarm.setDescription(context.getString(R.string.task_channel_alarm_desc));
        alarm.setSound(null, null);          // AlarmSoundService owns the audio
        alarm.enableVibration(false);        // ...and the vibration
        alarm.setBypassDnd(true);
        alarm.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(alarm);
    }

    private static int flags() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private static PendingIntent openTasks(Context context, TaskItem task) {
        Intent intent = new Intent(context, TasksActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(TasksActivity.EXTRA_FOCUS_TASK_ID, task.getId());
        return PendingIntent.getActivity(context, (int) task.getId(), intent, flags());
    }

    private static PendingIntent action(Context context, TaskItem task, String actionName, int offset) {
        Intent intent = new Intent(context, TaskActionReceiver.class);
        intent.setAction(actionName);
        intent.putExtra(ReminderScheduler.EXTRA_TASK_ID, task.getId());
        intent.setData(Uri.parse("jntuhtask://" + actionName + "/" + task.getId()));
        return PendingIntent.getBroadcast(context, (int) task.getId() + offset, intent, flags());
    }

    /** The ordinary heads-up reminder. */
    public static void showReminder(Context context, TaskItem task) {
        ensureChannels(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_task_bell)
                .setContentTitle(task.getTitle())
                .setContentText(subtitle(context, task))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(subtitle(context, task)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openTasks(context, task))
                .addAction(0, context.getString(R.string.task_action_done),
                        action(context, task, TaskActionReceiver.ACTION_DONE, 100_000))
                .addAction(0, context.getString(R.string.task_action_snooze),
                        action(context, task, TaskActionReceiver.ACTION_SNOOZE, 200_000));

        post(context, (int) task.getId(), builder.build());
    }

    /**
     * The alarm-style reminder. When the OS allows a full-screen intent this takes over the
     * lockscreen; on Android 14+ without the grant it degrades to a heads-up notification
     * that still wakes the screen once tapped, while the service keeps ringing either way.
     *
     * <p>Returned rather than posted because {@link com.jntuh.service.AlarmSoundService}
     * uses it as its foreground notification — posting here as well would show two.
     */
    public static Notification buildAlarm(Context context, TaskItem task) {
        ensureChannels(context);

        Intent ringIntent = new Intent(context, AlarmRingActivity.class);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ringIntent.putExtra(ReminderScheduler.EXTRA_TASK_ID, task.getId());
        PendingIntent ringPi = PendingIntent.getActivity(context,
                (int) task.getId() + 300_000, ringIntent, flags());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALARM)
                .setSmallIcon(R.drawable.ic_task_alarm)
                .setContentTitle(task.getTitle())
                .setContentText(subtitle(context, task))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(ringPi)
                .addAction(0, context.getString(R.string.task_action_snooze),
                        action(context, task, TaskActionReceiver.ACTION_SNOOZE, 200_000))
                .addAction(0, context.getString(R.string.task_action_dismiss),
                        action(context, task, TaskActionReceiver.ACTION_DISMISS, 400_000));

        if (AlarmPermissions.canUseFullScreen(context)) {
            builder.setFullScreenIntent(ringPi, true);
        }

        return builder.build();
    }

    private static void post(Context context, int id, Notification notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification);
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS refused on Android 13+; nothing else to do here.
        }
    }

    public static void cancel(Context context, long taskId) {
        NotificationManagerCompat.from(context).cancel((int) taskId);
    }

    /** Notes if the user wrote any, otherwise the category label. */
    private static String subtitle(Context context, TaskItem task) {
        if (!TextUtils.isEmpty(task.getNotes())) {
            return task.getNotes();
        }
        if (!TextUtils.isEmpty(task.getBookTitle())) {
            return task.getBookTitle();
        }
        return context.getString(com.jntuh.item.TaskCategory.labelOf(task.getCategory()));
    }
}

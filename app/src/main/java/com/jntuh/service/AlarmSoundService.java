package com.jntuh.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;

import android.content.pm.ServiceInfo;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

import com.jntuh.item.TaskItem;
import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.MyApplication;
import com.jntuh.util.ReminderScheduler;
import com.jntuh.util.TaskNotifier;

/**
 * Keeps an alarm-style reminder ringing until the user deals with it.
 *
 * <p>The audio lives in a foreground service rather than in {@link com.jntuh.books.AlarmRingActivity}
 * so it survives the activity being denied (Android 14+ full-screen-intent restrictions) or
 * swiped away, and so it keeps playing while the screen is off.
 *
 * <p>Plays on the alarm stream, which stays audible when the phone is on vibrate. Stops
 * itself after {@link #AUTO_STOP_MS} so a missed alarm cannot ring the battery flat.
 */
public class AlarmSoundService extends Service {

    private static final String TAG = "AlarmSoundService";

    public static final String ACTION_START = "com.jntuh.books.action.RING_START";
    public static final String ACTION_STOP = "com.jntuh.books.action.RING_STOP";

    /** Give up after two minutes, the same as the platform clock app. */
    private static final long AUTO_STOP_MS = 120_000L;

    private static final long[] VIBRATE_PATTERN = {0, 600, 800};

    private MediaPlayer player;
    private Vibrator vibrator;
    private final Handler autoStop = new Handler(Looper.getMainLooper());
    private long ringingTaskId = -1;

    /** Lets the ring screen ask whether it is still meant to be showing. */
    public static boolean isRinging;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_STOP : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopEverything();
            return START_NOT_STICKY;
        }

        long taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1);
        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        TaskItem task = taskId >= 0 ? db.getTask(taskId) : null;

        if (task == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        ringingTaskId = taskId;

        // Must reach startForeground within a few seconds of being started, so this comes
        // before any media setup that could throw.
        if (!startInForeground(task)) {
            // Nothing below would survive without the foreground promotion, and the alarm
            // channel is silent by design, so hand the user off to the reminder channel —
            // that one carries its own sound. Better a plain audible notification than an
            // alarm that fails quietly.
            TaskNotifier.showReminder(this, task);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!isRinging) {
            isRinging = true;
            startAudio();
            startVibration();
            autoStop.postDelayed(this::stopEverything, AUTO_STOP_MS);
        }

        return START_STICKY;
    }

    private boolean startInForeground(TaskItem task) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                : 0;
        try {
            ServiceCompat.startForeground(this, (int) task.getId(),
                    TaskNotifier.buildAlarm(this, task), type);
            return true;
        } catch (Exception e) {
            // Android 12+ refuses a foreground start from the background outside the
            // exact-alarm exemption; Android 14+ can also reject the declared type.
            Log.e(TAG, "startForeground refused", e);
            return false;
        }
    }

    private void startAudio() {
        Uri tone = pickTone();
        if (tone == null) {
            return;
        }
        try {
            player = new MediaPlayer();
            player.setDataSource(this, tone);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            Log.e(TAG, "Could not play the alarm tone", e);
            releasePlayer();
        }
    }

    /**
     * The tone the user picked in settings, falling back to the system alarm tone and then
     * to the notification tone — a device with no alarm tone at all still gets a noise.
     */
    private Uri pickTone() {
        String saved = MyApplication.getInstance() == null
                ? null : MyApplication.getInstance().getAlarmToneUri();
        if (!TextUtils.isEmpty(saved)) {
            return Uri.parse(saved);
        }
        Uri tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (tone == null) {
            tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return tone;
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        // Honour the user's choice to keep the phone completely silent.
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio != null && audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, 0));
            } else {
                vibrator.vibrate(VIBRATE_PATTERN, 0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Vibration unavailable", e);
        }
    }

    private void stopEverything() {
        isRinging = false;
        autoStop.removeCallbacksAndMessages(null);
        releasePlayer();
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (ringingTaskId >= 0) {
            TaskNotifier.cancel(this, ringingTaskId);
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (IllegalStateException ignored) {
                // Already torn down.
            }
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        isRinging = false;
        autoStop.removeCallbacksAndMessages(null);
        releasePlayer();
        if (vibrator != null) {
            vibrator.cancel();
        }
        super.onDestroy();
    }

    /** Convenience for callers that just want the noise to stop. */
    public static void stop(Context context) {
        try {
            Intent intent = new Intent(context, AlarmSoundService.class);
            intent.setAction(ACTION_STOP);
            context.startService(intent);
        } catch (Exception ignored) {
            // Not running.
        }
    }
}

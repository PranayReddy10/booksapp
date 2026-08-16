package com.jntuh.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.jntuh.util.ReminderScheduler;

import java.util.concurrent.TimeUnit;

/**
 * A once-a-day safety net that re-arms every reminder.
 *
 * <p>Not the primary mechanism — WorkManager cannot hit an exact time, so it could never
 * fire the alarms itself. Its job is to repair the schedule after the cases that silently
 * clear alarms without broadcasting anything we can hear: a force-stop from the app
 * switcher, an OEM battery sweep, or a restore onto a new device.
 */
public class ReminderSweepWorker extends Worker {

    private static final String TAG = "ReminderSweep";
    private static final String WORK_NAME = "task_reminder_sweep";

    public ReminderSweepWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            ReminderScheduler.rescheduleAll(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sweep failed", e);
            return Result.retry();
        }
    }

    /** Idempotent: safe to call on every app start. */
    public static void schedule(Context context) {
        try {
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    ReminderSweepWorker.class, 1, TimeUnit.DAYS)
                    .setConstraints(new Constraints.Builder().build())
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
        } catch (Exception e) {
            Log.e(TAG, "Could not enqueue the sweep", e);
        }
    }
}

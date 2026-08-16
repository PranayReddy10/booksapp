package com.jntuh.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jntuh.util.ReminderScheduler;

/**
 * Pending alarms are wiped by a reboot, an app update and a force-stop, and their absolute
 * trigger times become wrong when the clock or timezone moves. Every one of those events
 * lands here and rebuilds the whole schedule from the database.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        Log.d(TAG, "Rebuilding reminders after " + action);
        try {
            ReminderScheduler.rescheduleAll(context.getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Reschedule failed", e);
        }
    }
}

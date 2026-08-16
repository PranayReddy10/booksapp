package com.jntuh.util;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * The three OS gates a reminder has to pass, and the intents that let the user open them.
 *
 * <p>None of these can be granted silently:
 * <ul>
 *   <li><b>Exact alarms</b> — Android 12+ needs SCHEDULE_EXACT_ALARM granted in Settings.
 *       Without it a reminder still fires, just inexactly (minutes late under Doze).</li>
 *   <li><b>Full-screen intent</b> — Android 14+ revokes USE_FULL_SCREEN_INTENT by default
 *       for anything that isn't a clock or calling app, so the lockscreen takeover has to
 *       degrade to a heads-up notification.</li>
 *   <li><b>Battery optimisation</b> — the usual cause of "my alarm never went off" on
 *       Xiaomi / Oppo / Vivo, which kill background alarms aggressively.</li>
 * </ul>
 */
public final class AlarmPermissions {

    private AlarmPermissions() {
    }

    /** True when the OS will honour an exact trigger time. */
    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Sends the user to the per-app "Alarms & reminders" toggle. */
    public static Intent exactAlarmSettingsIntent(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null;
        }
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return intent;
    }

    /**
     * True when a full-screen alarm may cover the lockscreen. On Android 14+ this is
     * off by default for us, so callers must have a heads-up fallback ready.
     */
    public static boolean canUseFullScreen(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.canUseFullScreenIntent();
    }

    public static Intent fullScreenSettingsIntent(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return null;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return intent;
    }

    /**
     * Opens the OEM battery settings so the user can exempt the app. Deliberately the
     * generic screen rather than ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which Play
     * only permits for apps whose core function is alarms.
     */
    public static Intent batterySettingsIntent() {
        return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
    }

    /** True when every gate an alarm-type reminder wants is already open. */
    public static boolean isFullyReady(Context context) {
        return canScheduleExact(context) && canUseFullScreen(context);
    }
}

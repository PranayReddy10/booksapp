package com.jntuh.util;

import android.content.Context;

import com.jntuh.books.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Date formatting and day-boundary maths shared by the task screens. */
public final class TaskDates {

    private TaskDates() {
    }

    public static SimpleDateFormat timeFormat() {
        return new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    public static SimpleDateFormat dateFormat() {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
    }

    public static SimpleDateFormat dayFormat() {
        return new SimpleDateFormat("dd", Locale.getDefault());
    }

    public static SimpleDateFormat weekdayFormat() {
        return new SimpleDateFormat("EEE", Locale.getDefault());
    }

    public static SimpleDateFormat longDateFormat() {
        return new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
    }

    /** Midnight at the start of the day containing {@code millis}. */
    public static long startOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** Midnight at the start of the following day. */
    public static long endOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startOfDay(millis));
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return cal.getTimeInMillis();
    }

    public static long plusDays(long millis, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTimeInMillis();
    }

    public static boolean isSameDay(long a, long b) {
        return startOfDay(a) == startOfDay(b);
    }

    /**
     * "Today · 8:00 PM" / "Tomorrow · 7:30 AM" / "12 Sep · 9:00 AM" — the label under a
     * task title. Relative wording only for today and tomorrow; past days get the date so
     * an overdue task is unambiguous.
     */
    public static String relativeLabel(Context context, long millis) {
        String time = timeFormat().format(new Date(millis));
        long now = System.currentTimeMillis();

        if (isSameDay(millis, now)) {
            return context.getString(R.string.task_today_tag) + " · " + time;
        }
        if (isSameDay(millis, plusDays(now, 1))) {
            return context.getString(R.string.task_tomorrow_tag) + " · " + time;
        }
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(millis)) + " · " + time;
    }
}

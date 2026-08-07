package com.jntuh.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.WorkManager;

import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.Method;

import java.io.File;

/**
 * Fired when the user taps the download progress notification. Cancels the
 * WorkManager job for that book (the worker sees isStopped() and aborts), removes the
 * notification, and cleans up any partial file / db row.
 */
public class DownloadCancelReceiver extends BroadcastReceiver {

    public static final String ACTION_CANCEL = "com.jntuh.download.action.CANCEL";
    private static final int NOTIFICATION_ID = 111;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_CANCEL.equals(intent.getAction())) return;

        String id = intent.getStringExtra(DownloadWorker.KEY_ID);
        String path = intent.getStringExtra(DownloadWorker.KEY_PATH);
        String name = intent.getStringExtra(DownloadWorker.KEY_NAME);

        // Cancel the running work; the worker's isStopped() loop aborts the download.
        if (id != null) {
            WorkManager.getInstance(context.getApplicationContext())
                    .cancelUniqueWork(DownloadWorker.WORK_PREFIX + id);
        }

        // Remove the notification.
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIFICATION_ID);

        // Clean up partial file + db row (best-effort; worker also cleans up).
        try {
            if (path != null && name != null) {
                File f = new File(path, name);
                if (f.exists()) f.delete();
            }
            if (id != null) {
                DatabaseHandler db = new DatabaseHandler(context.getApplicationContext());
                if (!db.checkIdDownloadBook(id)) db.deleteDownloadBook(id);
            }
        } catch (Exception ignored) {
        }

        Method.isDownload = true;
    }
}

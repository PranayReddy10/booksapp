package com.jntuh.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.jntuh.books.R;
import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.Method;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import io.github.lizhangqu.coreprogress.ProgressHelper;
import io.github.lizhangqu.coreprogress.ProgressUIListener;

/**
 * Downloads a book PDF in the background via WorkManager, with a progress notification.
 * Replaces the old foreground DownloadService (FOREGROUND_SERVICE_DATA_SYNC was flagged
 * by Google Play — a book download can be deferred, so a foreground service is not
 * compliant).
 *
 * The notification has a Cancel action: tapping it broadcasts to
 * {@link DownloadCancelReceiver}, which cancels the unique work by id. The worker
 * checks {@link #isStopped()} during the download and aborts + cleans up.
 *
 * Input Data keys: "id", "downloadUrl", "file_path", "file_name".
 * The unique work name is "download_" + id.
 */
public class DownloadWorker extends Worker {

    public static final String KEY_ID = "id";
    public static final String KEY_URL = "downloadUrl";
    public static final String KEY_PATH = "file_path";
    public static final String KEY_NAME = "file_name";

    public static final String WORK_PREFIX = "download_";

    private static final int NOTIFICATION_ID = 111;
    private static final String CHANNEL_ID = "download_book";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private RemoteViews rv;

    private String bookId, filePath, fileName;

    public DownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String downloadUrl = getInputData().getString(KEY_URL);
        filePath = getInputData().getString(KEY_PATH);
        fileName = getInputData().getString(KEY_NAME);
        bookId = getInputData().getString(KEY_ID);

        if (downloadUrl == null || filePath == null || fileName == null) {
            Method.isDownload = true;
            return Result.failure();
        }

        setupNotification();

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(downloadUrl)
                    .addHeader("Accept-Encoding", "identity")
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (response.body() == null) {
                cleanupPartial();
                cancelNotification();
                Method.isDownload = true;
                return Result.failure();
            }

            ResponseBody responseBody = ProgressHelper.withProgress(response.body(),
                    new ProgressUIListener() {
                        @Override
                        public void onUIProgressChanged(long numBytes, long totalBytes, float percent, float speed) {
                            updateProgress((int) (100 * percent));
                        }

                        @Override
                        public void onUIProgressFinish() {
                            super.onUIProgressFinish();
                            Method.isDownload = true;
                        }
                    });

            File outFile = new File(filePath + "/" + fileName);
            BufferedSource source = responseBody.source();
            BufferedSink sink = Okio.buffer(Okio.sink(outFile));

            // Stream in chunks so we can react to a cancel promptly.
            okio.Buffer buffer = new okio.Buffer();
            long read;
            while ((read = source.read(buffer, 8192)) != -1) {
                if (isStopped()) {           // user tapped Cancel
                    sink.flush();
                    sink.close();
                    source.close();
                    cleanupPartial();
                    cancelNotification();
                    Method.isDownload = true;
                    return Result.success();  // cancellation is not a failure
                }
                sink.write(buffer, read);
            }
            sink.flush();
            sink.close();
            source.close();

            cancelNotification();
            Method.isDownload = true;
            return Result.success();

        } catch (IOException e) {
            cleanupPartial();
            cancelNotification();
            Method.isDownload = true;
            return Result.failure();
        }
    }

    private void cleanupPartial() {
        try {
            File f = new File(filePath, fileName);
            if (f.exists()) f.delete();
            DatabaseHandler db = new DatabaseHandler(getApplicationContext());
            if (bookId != null && !db.checkIdDownloadBook(bookId)) {
                db.deleteDownloadBook(bookId);
            }
        } catch (Exception ignored) {
        }
    }

    private void setupNotification() {
        Context ctx = getApplicationContext();
        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Download", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        rv = new RemoteViews(ctx.getPackageName(), R.layout.my_custom_notification);
        rv.setTextViewText(R.id.nf_title, ctx.getString(R.string.app_name));
        rv.setProgressBar(R.id.progress, 100, 0, false);
        rv.setTextViewText(R.id.nf_percentage, ctx.getString(R.string.downloading) + " (0%)");

        // Tapping the notification cancels the download (same as the old behaviour).
        Intent cancelIntent = new Intent(ctx, DownloadCancelReceiver.class);
        cancelIntent.setAction(DownloadCancelReceiver.ACTION_CANCEL);
        cancelIntent.putExtra(KEY_ID, bookId);
        cancelIntent.putExtra(KEY_PATH, filePath);
        cancelIntent.putExtra(KEY_NAME, fileName);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, bookId != null ? bookId.hashCode() : 0, cancelIntent, flags);
        rv.setOnClickPendingIntent(R.id.relativeLayout_nf, pi);

        builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_onesignal_large_icon_default)
                .setContentTitle(ctx.getString(R.string.app_name))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCustomContentView(rv);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateProgress(int progress) {
        if (notificationManager == null || builder == null) return;
        Context ctx = getApplicationContext();
        rv.setProgressBar(R.id.progress, 100, progress, false);
        rv.setTextViewText(R.id.nf_percentage, ctx.getString(R.string.downloading) + " (" + progress + "%)");
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
    }
}

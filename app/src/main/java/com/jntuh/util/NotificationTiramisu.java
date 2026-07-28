package com.jntuh.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.onesignal.OneSignal;



public class NotificationTiramisu {
    public static void takePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dexter.withContext(context)
                    .withPermission(Manifest.permission.POST_NOTIFICATIONS)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {

                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            // check for permanent denial of permission
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();
        }
    }

    public static boolean isNotificationChecked(Context context) {
        boolean status;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                //when permission is granted but want to off notification only
                status = MyApplication.getInstance().getNotification();
            } else {
                status = false;
            }
        } else {
            status = MyApplication.getInstance().getNotification();
        }
        return status;
    }

    public static void takePermissionSettings(Activity activity, SwitchCompat notificationSwitch, ActivityResultLauncher<Intent> activityResultLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationSwitch.setChecked(false);
            Dexter.withContext(activity)
                    .withPermission(Manifest.permission.POST_NOTIFICATIONS)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            notificationSwitch.setChecked(true);
                            saveNotification();
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            // check for permanent denial of permission
                            notificationSwitch.setChecked(false);
                            showSettingsDialog(activity, activityResultLauncher);
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();
        } else {
            saveNotification();
        }
    }

    private static void saveNotification() {
        MyApplication.getInstance().saveIsNotification(true);
        OneSignal.getUser().getPushSubscription().optIn();
    }

    private static void showSettingsDialog(Activity activity, ActivityResultLauncher<Intent> activityResultLauncher) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Get Notified");
        builder.setMessage("Allow this to send you notifications");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activityResultLauncher.launch(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public static void setCheckedFromSetting(Activity activity, SwitchCompat notificationSwitch) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationSwitch.setChecked(ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        }
    }
}

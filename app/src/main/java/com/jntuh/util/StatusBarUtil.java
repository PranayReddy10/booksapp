package com.jntuh.util;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.jntuh.books.R;

public class StatusBarUtil {
    public static void setStatusBar(Activity activity, String activityName) {
        Window window = activity.getWindow();
        Drawable background;
        if(activityName.isEmpty()) {
            background = ContextCompat.getDrawable(activity, R.drawable.status_bar);
        } else if(activityName.equalsIgnoreCase("profile")) {
            background = ContextCompat.getDrawable(activity, R.drawable.status_bar_profile);
        } else if(activityName.equalsIgnoreCase("payment")) {
            background = ContextCompat.getDrawable(activity, R.drawable.status_bar_ppayment);
        } else {
            background = ContextCompat.getDrawable(activity, R.drawable.status_bar_login);
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setBackgroundDrawable(background);
        EdgeUtils.enable(activity);

        if(activityName.equalsIgnoreCase("profile")) {
            window.setNavigationBarColor(ContextCompat.getColor(activity, R.color.bg_status_bar));
        }

        boolean isLightMode = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;

        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = decorView.getWindowInsetsController();

            if (insetsController != null) {
                if (isLightMode) {
                    insetsController.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    );
                } else {
                    insetsController.setSystemBarsAppearance(
                            0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    );
                }
            }
        } else {
            int flags = decorView.getSystemUiVisibility();
            if (isLightMode) {
                // Light mode → dark icons (status + nav bar)
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                // Dark mode → light icons (status + nav bar)
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            decorView.setSystemUiVisibility(flags);
        }
    }
}
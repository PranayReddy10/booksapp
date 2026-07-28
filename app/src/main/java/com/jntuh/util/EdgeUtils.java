package com.jntuh.util;

import android.app.Activity;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import dev.chrisbanes.insetter.Insetter;

public class EdgeUtils {

    public static void enable(Activity activity) {
        try {
            EdgeToEdge.enable((ComponentActivity) activity);
            setSystemBarAppearance(activity, false);
            View view = activity.findViewById(android.R.id.content);
            Insetter.builder().padding(WindowInsetsCompat.Type.systemBars()).applyToView(view);
        } catch (Exception ignored) {

        }
    }

    /*
    Light status have black icon and text in status bar
 */
    public static void setSystemBarAppearance(Activity activity, boolean isLight) {
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(isLight);
        insetsController.setAppearanceLightNavigationBars(isLight);
    }
}

package com.jntuh.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.multidex.MultiDex;

import com.jntuh.books.ForgotPassActivity;
import com.jntuh.books.LoginActivity;
import com.jntuh.books.MainActivity;
import com.jntuh.books.PaymentMethodActivity;
import com.jntuh.books.PlanListActivity;
import com.jntuh.books.RegisterActivity;
import com.jntuh.books.SplashActivity;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.signin.internal.SignInHubActivity;
import com.onesignal.OneSignal;
import com.onesignal.notifications.INotificationClickEvent;
import com.onesignal.notifications.INotificationClickListener;


import org.json.JSONException;
import org.json.JSONObject;


public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private static MyApplication mInstance;
    public SharedPreferences preferences;
    public String prefName = "app";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public MyApplication() {
        mInstance = this;
    }

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.registerActivityLifecycleCallbacks(this);

        AudienceNetworkAds.initialize(this);
        MobileAds.initialize(this, initializationStatus -> {
        });


        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        OneSignal.initWithContext(this, "ddb1b0a7-b5e1-4v82-86c3-5b30f3fb5e50");
        OneSignal.getNotifications().addClickListener(new NotificationExtenderExample());

    }

    public void saveIsNotification(boolean flag) {
        preferences = this.getSharedPreferences(prefName, 0);
        Editor editor = preferences.edit();
        editor.putBoolean("IsNotification", flag);
        editor.apply();
    }

    public boolean getNotification() {
        preferences = this.getSharedPreferences(prefName, 0);
        return preferences.getBoolean("IsNotification", true);
    }

    class NotificationExtenderExample implements INotificationClickListener {

        @Override
        public void onClick(@NonNull INotificationClickEvent result) {
            JSONObject jsonObject = result.getNotification().getAdditionalData();
            if (jsonObject != null) {
                try {

                    String id = jsonObject.getString("post_id");
                    String type = jsonObject.getString("type");
                    String titleName = jsonObject.getString("post_title");
                    String url = jsonObject.getString("external_link");

                    Intent intent;
                    if (id.equals("") && !url.equals("false") && !url.trim().isEmpty()) {
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.parse(url));
                    } else {
                        intent = new Intent(MyApplication.this, SplashActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("id", id);
                        intent.putExtra("type", type);
                        intent.putExtra("title", titleName);
                    }
                    startActivity(intent);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Intent intent = new Intent(MyApplication.this, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            } else {
                Intent intent = new Intent(MyApplication.this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        if(!(activity instanceof MainActivity) && !(activity instanceof LoginActivity) && !(activity instanceof RegisterActivity) && !(activity instanceof ForgotPassActivity) && !(activity instanceof SplashActivity) && !(activity instanceof SignInHubActivity)) {
            if(!(activity instanceof PaymentMethodActivity) && !(activity instanceof PlanListActivity)) {
                StatusBarUtil.setStatusBar(activity, "");
            } else {
                StatusBarUtil.setStatusBar(activity, "payment");
            }
        } else if(!(activity instanceof MainActivity) && !(activity instanceof SplashActivity) && !(activity instanceof SignInHubActivity)){
            StatusBarUtil.setStatusBar(activity, "login");
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}

package com.jntuh.books;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jntuh.adapter.SettingAdapter;
import com.jntuh.books.databinding.ActivitySettingsBinding;
import com.jntuh.fragment.ThemeChangeFragment;
import com.jntuh.response.AppDetailRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Constant;
import com.jntuh.util.Method;
import com.jntuh.util.MyApplication;
import com.jntuh.util.NotificationTiramisu;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.onesignal.OneSignal;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DecimalFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SettingsActivity extends AppCompatActivity {

    ActivitySettingsBinding viewSetting;
    Method method;
    SettingAdapter settingAdapter;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewSetting = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(viewSetting.getRoot());
        method = new Method(SettingsActivity.this);
        method.forceRTLIfSupported();

        viewSetting.toolbarMain.tvToolbarTitle.setText(getString(R.string.lbl_setting));
        viewSetting.toolbarMain.ivSearch.setVisibility(View.GONE);
        viewSetting.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        viewSetting.progressHome.setVisibility(View.GONE);
        viewSetting.llNoData.clNoDataFound.setVisibility(View.GONE);
        viewSetting.nsSettingScroll.setVisibility(View.GONE);

        viewSetting.rvSettingList.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(SettingsActivity.this, 1);
        viewSetting.rvSettingList.setLayoutManager(layoutManager);
        viewSetting.rvSettingList.setFocusable(false);

        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked},
        };

        int[] thumbColors = new int[]{
                ContextCompat.getColor(SettingsActivity.this, R.color.switch_thumb_disable),
                ContextCompat.getColor(SettingsActivity.this, R.color.switch_thumb_enable),
        };

        int[] trackColors = new int[]{
                ContextCompat.getColor(SettingsActivity.this, R.color.switch_track_disable),
                ContextCompat.getColor(SettingsActivity.this, R.color.switch_track),
        };
        DrawableCompat.setTintList(DrawableCompat.wrap(viewSetting.scNotification.getThumbDrawable()), new ColorStateList(states, thumbColors));
        DrawableCompat.setTintList(DrawableCompat.wrap(viewSetting.scNotification.getTrackDrawable()), new ColorStateList(states, trackColors));

        viewSetting.scNotification.setChecked(NotificationTiramisu.isNotificationChecked(SettingsActivity.this));//myApplication.getNotification()
        viewSetting.scNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                NotificationTiramisu.takePermissionSettings(SettingsActivity.this, viewSetting.scNotification, activityResultLauncher);
            } else {
                MyApplication.getInstance().saveIsNotification(false);
                OneSignal.getUser().getPushSubscription().optOut();
            }
        });

        initializeCache();
        viewSetting.rlClearCache.setOnClickListener(v -> {
            FileUtils.deleteQuietly(getCacheDir());
            FileUtils.deleteQuietly(getExternalCacheDir());
            viewSetting.tvSettingClearCacheSize.setText("0 MB");
        });

        viewSetting.rlDarkMode.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            ThemeChangeFragment themeChangeFragment = new ThemeChangeFragment();
            themeChangeFragment.setArguments(bundle);
            themeChangeFragment.show(getSupportFragmentManager(), themeChangeFragment.getTag());

        });

        viewSetting.rlMoreApp.setOnClickListener(v -> {
            if (Constant.appListData.getGooglePlayLink().isEmpty()) {
                Toast.makeText(SettingsActivity.this, getString(R.string.no_link_found), Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constant.appListData.getGooglePlayLink())));
            }
        });

        viewSetting.rlRateApp.setOnClickListener(v -> {
            final String appName = getPackageName();//your application package name i.e play store application url
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id="
                                + appName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id="
                                + appName)));
            }
        });

        viewSetting.rlShareApp.setOnClickListener(v -> {
            Intent intentShare = new Intent(Intent.ACTION_SEND);
            intentShare.setType("text/plain");
            intentShare.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_msg) + "\n" + "http://play.google.com/store/apps/details?id=" + getPackageName());
            startActivity(intentShare);
        });

        if (method.isNetworkAvailable()) {
            settingPageData();
        } else {
            method.alertBox(getResources().getString(R.string.internet_connection));
        }

    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        NotificationTiramisu.setCheckedFromSetting(SettingsActivity.this, viewSetting.scNotification);
    });


    private void settingPageData() {

        viewSetting.progressHome.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(SettingsActivity.this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<AppDetailRP> call = apiService.getSettingPageData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<AppDetailRP>() {
            @Override
            public void onResponse(@NotNull Call<AppDetailRP> call, @NotNull Response<AppDetailRP> response) {

                try {

                    AppDetailRP appDetailRP = response.body();

                    if (appDetailRP != null && appDetailRP.getSuccess().equals("1")) {
                        if (appDetailRP.getAppLists().get(0).getPageList().size() != 0) {
                            settingAdapter = new SettingAdapter(SettingsActivity.this, appDetailRP.getAppLists().get(0).getPageList());
                            viewSetting.rvSettingList.setAdapter(settingAdapter);
                            settingAdapter.setOnItemClickListener(position -> {
                                if (appDetailRP.getAppLists().get(0).getPageList().get(position).getPageId().equals("1")) {
                                    Intent intentAbout = new Intent(SettingsActivity.this, AboutUsActivity.class);
                                    intentAbout.putExtra("ABOUT", appDetailRP.getAppLists().get(0).getPageList().get(position).getPageContent());
                                    startActivity(intentAbout);
                                } else {
                                    Intent intentPage = new Intent(SettingsActivity.this, PagesActivity.class);
                                    intentPage.putExtra("PAGE_TITLE", appDetailRP.getAppLists().get(0).getPageList().get(position).getPageTitle());
                                    intentPage.putExtra("PAGE_DESC", appDetailRP.getAppLists().get(0).getPageList().get(position).getPageContent());
                                    startActivity(intentPage);
                                }

                            });

                        } else {
                            viewSetting.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                            viewSetting.rvSettingList.setVisibility(View.GONE);
                            viewSetting.progressHome.setVisibility(View.GONE);
                        }
                    } else {
                        viewSetting.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                        viewSetting.rvSettingList.setVisibility(View.GONE);
                        viewSetting.progressHome.setVisibility(View.GONE);
                        method.alertBox(getResources().getString(R.string.failed_try_again));
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }

                viewSetting.progressHome.setVisibility(View.GONE);
                viewSetting.llNoData.clNoDataFound.setVisibility(View.GONE);
                viewSetting.nsSettingScroll.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(@NotNull Call<AppDetailRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                viewSetting.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                viewSetting.progressHome.setVisibility(View.GONE);
                viewSetting.nsSettingScroll.setVisibility(View.VISIBLE);
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });

    }

    private void initializeCache() {
        long size = 0;
        size += getDirSize(this.getCacheDir());
        size += getDirSize(this.getExternalCacheDir());
        viewSetting.tvSettingClearCacheSize.setText(readableFileSize(size));
    }

    public long getDirSize(File dir) {
        long size = 0;
        for (File file : dir.listFiles()) {
            if (file != null && file.isDirectory()) {
                size += getDirSize(file);
            } else if (file != null && file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0 Bytes";
        final String[] units = new String[]{"Bytes", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}

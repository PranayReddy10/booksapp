package com.jntuh.books;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jntuh.books.databinding.ActivityMainBinding;
import com.jntuh.fragment.MediaExploreFragment;
import com.jntuh.fragment.CategoryFragment;
import com.jntuh.fragment.HomeFragment;
import com.jntuh.fragment.LatestFragment;
import com.jntuh.fragment.ProfileFragment;
import com.jntuh.util.BannerAds;
import com.jntuh.util.Constant;
import com.jntuh.util.GDPRChecker;
import com.jntuh.util.Method;
import com.jntuh.util.NotificationTiramisu;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    ImageView[] imageViews;
    TextView[] textViews;
    LinearLayout[] linearLayouts;
    ActivityMainBinding viewMain;
    FragmentManager fragmentManager;
    boolean doubleBackToExitPressedOnce = false;
    Method method;
    int versionCode;
    FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewMain = ActivityMainBinding.inflate(getLayoutInflater());
        View view = viewMain.getRoot();
        setContentView(view);

        method = new Method(MainActivity.this);
        method.forceRTLIfSupported();
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        versionCode = BuildConfig.VERSION_CODE;

        fragmentManager = getSupportFragmentManager();
        linearLayouts = new LinearLayout[]{viewMain.bottomNav.frameHome, viewMain.bottomNav.frameLatest, viewMain.bottomNav.frameCat, viewMain.bottomNav.frameAuthor, viewMain.bottomNav.frameProfile};
        imageViews = new ImageView[]{viewMain.bottomNav.imageHome, viewMain.bottomNav.imageLatest, viewMain.bottomNav.imageCat, viewMain.bottomNav.imageAuthor, viewMain.bottomNav.imageProfile};
        textViews = new TextView[]{viewMain.bottomNav.tvHome, viewMain.bottomNav.tvLatest, viewMain.bottomNav.tvCat, viewMain.bottomNav.tvAuthor, viewMain.bottomNav.tvProfile};

        NotificationTiramisu.takePermission(this);

        if (Constant.isBanner) {
            if (Constant.adNetworkType.equals("Admob")) {
                checkForConsent();
            } else {
                BannerAds.showBannerAds(MainActivity.this, viewMain.layoutAds);
            }
        }

        if (Constant.appUpdateVersion > versionCode && Constant.isAppUpdate) {
            newUpdateDialog();
        }

        selectBottomNav(0);
        HomeFragment homeFragment = new HomeFragment();
        homeFragment.setOnItemClickListener(position -> profileFragment(true, 0));

        loadFrag(homeFragment, "", fragmentManager);
        viewMain.toolbarMain.toolbarToolbar.setVisibility(View.GONE);

        // Deep-link from the feed menu: open the Profile tab directly.
        if (getIntent() != null && getIntent().getBooleanExtra("openProfile", false)) {
            profileFragment(false, 0);
        }

        // Deep-link from the home "Latest Books" arrow: open the Latest tab.
        if (getIntent() != null && getIntent().getBooleanExtra("openLatest", false)) {
            selectBottomNav(1);
            viewMain.toolbarMain.toolbarToolbar.setVisibility(View.GONE);
            loadFrag(new LatestFragment(), "", fragmentManager);
        }
        viewMain.bottomNav.frameHome.setOnClickListener(v -> {
            selectBottomNav(0);
            viewMain.toolbarMain.toolbarToolbar.setVisibility(View.GONE);
            HomeFragment homeFragment1 = new HomeFragment();
            homeFragment1.setOnItemClickListener(position -> profileFragment(true, 0));
            loadFrag(homeFragment1, "", fragmentManager);

        });

        viewMain.bottomNav.frameLatest.setOnClickListener(v -> {
            selectBottomNav(1);
            viewMain.toolbarMain.toolbarToolbar.setVisibility(View.GONE);
            LatestFragment latestFragment = new LatestFragment();
            loadFrag(latestFragment, "", fragmentManager);
        });

        viewMain.bottomNav.frameCat.setOnClickListener(v -> {
            selectBottomNav(2);
            viewMain.toolbarMain.toolbarToolbar.setVisibility(View.GONE);
            CategoryFragment categoryFragment = new CategoryFragment();
            loadFrag(categoryFragment, "", fragmentManager);

        });

        viewMain.bottomNav.frameAuthor.setOnClickListener(v -> {
            selectBottomNav(3);
            viewMain.toolbarMain.toolbarToolbar.setVisibility(View.GONE);
            MediaExploreFragment mediaExploreFragment = new MediaExploreFragment();
            loadFrag(mediaExploreFragment, "", fragmentManager);
        });

        viewMain.bottomNav.frameProfile.setOnClickListener(v -> profileFragment(false, 0));

        viewMain.fabUpload.extend();
        viewMain.fabUpload.setOnClickListener(v -> {
            if (method.getIsLogin()) {
                startActivity(new Intent(MainActivity.this, UploadBookActivity.class));
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

        // Feed is public to browse; uploading from inside it requires login.
        viewMain.fabFeed.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MediaFeedActivity.class)));

    }

    private void profileFragment(boolean isContinue, int pos) {
        selectBottomNav(4);
        Bundle bundle = new Bundle();
        bundle.putBoolean("isContinue", isContinue);
        bundle.putInt("movePos", pos);
        ProfileFragment profileFragment = new ProfileFragment();
        profileFragment.setArguments(bundle);
        loadFrag(profileFragment, "", fragmentManager);
    }

    private void selectBottomNav(int pos) {
        for (int i = 0; i < linearLayouts.length; i++) {
            if (i == pos) {
                textViews[i].setTextColor(ContextCompat.getColor(MainActivity.this, R.color.bottom_menu_bg_select));
                imageViews[i].setColorFilter(getResources().getColor(R.color.bottom_menu_bg_select), PorterDuff.Mode.SRC_IN);
            } else {
                textViews[i].setTextColor(ContextCompat.getColor(MainActivity.this, R.color.bottom_menu_bg_normal));
                imageViews[i].setColorFilter(getResources().getColor(R.color.bottom_menu_bg_normal), PorterDuff.Mode.SRC_IN);
            }
        }
    }

    public void highLightNavigation(int position) {
        selectBottomNav(position);
        viewMain.toolbarMain.toolbarToolbar.setVisibility(View.GONE);
        LatestFragment latestFragment = new LatestFragment();
        loadFrag(latestFragment, "", fragmentManager);
    }

    public void loadFrag(Fragment f1, String name, FragmentManager fm) {
        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.frameMain, f1, name);
        ft.commitAllowingStateLoss();
    }

    public void checkForConsent() {
        new GDPRChecker()
                .withContext(MainActivity.this)
                .check();
        BannerAds.showBannerAds(MainActivity.this, viewMain.layoutAds);
    }

    private void newUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.app_update_title));
        builder.setCancelable(false);
        builder.setMessage(Constant.appUpdateDesc);
        builder.setPositiveButton(getString(R.string.app_update_btn), (dialog, which) -> startActivity(new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Constant.appUpdateUrl))));
        if (Constant.isAppUpdateCancel) {
            builder.setNegativeButton(getString(R.string.app_cancel_btn), (dialog, which) -> {

            });
        }
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.show();
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() != 0) {
            super.onBackPressed();
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, getString(R.string.back_key), Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        }
    }
}
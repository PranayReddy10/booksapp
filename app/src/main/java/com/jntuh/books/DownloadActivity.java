package com.jntuh.books;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.jntuh.books.databinding.ActivityDownloadBinding;
import com.jntuh.fragment.DownloadFragment;
import com.jntuh.fragment.FavoriteFragment;
import com.jntuh.util.BannerAds;
import com.jntuh.util.Method;


public class DownloadActivity extends AppCompatActivity {

    ActivityDownloadBinding viewDownloadBinding;
    Method method;
    String isDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewDownloadBinding = ActivityDownloadBinding.inflate(getLayoutInflater());
        setContentView(viewDownloadBinding.getRoot());

        method = new Method(DownloadActivity.this);
        method.forceRTLIfSupported();
        Intent intent = getIntent();
        isDown = intent.getStringExtra("isDown");
        switch (isDown) {
            case "isDown":
                viewDownloadBinding.toolbarMain.tvToolbarTitle.setText(getString(R.string.tab_download));
                viewDownloadBinding.toolbarMain.imageArrowBack.setVisibility(View.VISIBLE);
                break;
            case "isOff":
                viewDownloadBinding.toolbarMain.tvToolbarTitle.setText(getString(R.string.tab_download));
                viewDownloadBinding.toolbarMain.imageArrowBack.setVisibility(View.GONE);
                break;
            case "isFav":
                viewDownloadBinding.toolbarMain.tvToolbarTitle.setText(getString(R.string.tab_favorite));
                viewDownloadBinding.toolbarMain.imageArrowBack.setVisibility(View.VISIBLE);
                break;
        }
        viewDownloadBinding.toolbarMain.imageArrowBack.setOnClickListener(v -> onBackPressed());

        if (isDown.equals("isDown")) {
            DownloadFragment downloadFragment = new DownloadFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.frameMain, downloadFragment, "")
                    .commitAllowingStateLoss();
        } else if (isDown.equals("isFav")) {
            FavoriteFragment favoriteFragment = new FavoriteFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.frameMain, favoriteFragment, "")
                    .commitAllowingStateLoss();
        } else {
            DownloadFragment downloadFragment = new DownloadFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.frameMain, downloadFragment, "")
                    .commitAllowingStateLoss();
        }

        BannerAds.showBannerAds(DownloadActivity.this, viewDownloadBinding.layoutAds);
    }

    @Override
    public void onBackPressed() {
        if (isDown.equals("isDown")) {
            super.onBackPressed();
        } else if (isDown.equals("isFav")) {
            super.onBackPressed();
        } else {
            if (method.isNetworkAvailable()) {
                Intent intentMain = new Intent(getApplicationContext(), SplashActivity.class);
                intentMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentMain);
                finishAffinity();
            } else {
                super.onBackPressed();
            }
        }
    }
}

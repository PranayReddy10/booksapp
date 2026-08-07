package com.jntuh.books;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Plays a YouTube video IN-APP by loading the SAME embed the website uses
 * (youtube-nocookie.com/embed/{id}) inside a WebView. This matches the website's
 * working playback exactly and avoids the IFrame-API library's 150/152 errors.
 *
 * Extras: "video_id" (required), "title" (optional).
 */
public class VideoPlayerActivity extends AppCompatActivity {

    private WebView webView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private View customView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String videoId = getIntent().getStringExtra("video_id");
        String title = getIntent().getStringExtra("title");

        ((TextView) findViewById(R.id.tvTitle)).setText(title != null ? title : "");
        ((ImageView) findViewById(R.id.ivBack)).setOnClickListener(v -> onBackPressed());

        webView = findViewById(R.id.webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        // Handle the player's fullscreen button (custom view).
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                customView = view;
                fullscreenCallback = callback;
                ((android.view.ViewGroup) getWindow().getDecorView())
                        .addView(view, new android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                findViewById(R.id.topBar).setVisibility(View.GONE);
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView != null) {
                    ((android.view.ViewGroup) getWindow().getDecorView()).removeView(customView);
                    customView = null;
                }
                if (fullscreenCallback != null) {
                    fullscreenCallback.onCustomViewHidden();
                    fullscreenCallback = null;
                }
                findViewById(R.id.topBar).setVisibility(View.VISIBLE);
                webView.setVisibility(View.VISIBLE);
            }
        });

        String id = videoId != null ? videoId : "";
        // Same embed origin/host as the website (youtube-nocookie), autoplay + rel=0.
        String embedUrl = "https://www.youtube-nocookie.com/embed/" + id + "?autoplay=1&rel=0&playsinline=1";
        String html = "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<style>*{margin:0;padding:0}html,body{background:#000;height:100%}"
                + ".wrap{position:fixed;top:0;left:0;right:0;bottom:0}"
                + "iframe{width:100%;height:100%;border:0}</style></head>"
                + "<body><div class='wrap'>"
                + "<iframe src='" + embedUrl + "' "
                + "allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture' "
                + "allowfullscreen></iframe></div></body></html>";

        // Load with the app's site as the base URL so the embed's referrer/origin is valid.
        webView.loadDataWithBaseURL("https://read.jntubooks.in", html, "text/html", "utf-8", null);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {           // exit fullscreen first
            webView.getWebChromeClient().onHideCustomView();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}

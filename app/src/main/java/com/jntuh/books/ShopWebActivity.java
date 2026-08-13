package com.jntuh.books;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.jntuh.books.databinding.ActivityShopWebBinding;
import com.jntuh.util.Method;

/**
 * In-app browser for the MadeForU website: product personalisation, cart,
 * checkout, payment, and order tracking all run here. Keeps the user inside the
 * app for the whole purchase, with back-navigation through web history.
 *
 * Non-http(s) links (upi:, tel:, mailto:, whatsapp:, intent:) are handed off to
 * the system so payment apps / dialer / WhatsApp open correctly.
 */
public class ShopWebActivity extends AppCompatActivity {

    private ActivityShopWebBinding binding;

    // For <input type="file"> in the WebView (photo personalisation on the site).
    private ValueCallback<Uri[]> filePathCallback;
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (filePathCallback == null) return;
                        Uri[] results = null;
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            if (data.getClipData() != null) {
                                int count = data.getClipData().getItemCount();
                                results = new Uri[count];
                                for (int i = 0; i < count; i++) {
                                    results[i] = data.getClipData().getItemAt(i).getUri();
                                }
                            } else if (data.getData() != null) {
                                results = new Uri[]{data.getData()};
                            }
                        }
                        filePathCallback.onReceiveValue(results);
                        filePathCallback = null;
                    });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopWebBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Method(this);

        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        if (url == null || url.isEmpty()) url = "https://madeforu.co.in/shop/";

        binding.toolbarMain.tvToolbarTitle.setText(title == null ? getString(R.string.shop_title) : title);
        binding.toolbarMain.ivSearch.setVisibility(View.GONE);
        binding.toolbarMain.imageFilter.setVisibility(View.GONE);
        binding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        WebSettings s = binding.webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (binding == null) return;
                if (newProgress < 100) {
                    binding.webProgress.setVisibility(View.VISIBLE);
                    binding.webProgress.setProgress(newProgress);
                } else {
                    binding.webProgress.setVisibility(View.GONE);
                }
            }

            // Enables the site's "upload your photo" fields to open the picker.
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    // Allow multiple + any image; the site decides what it accepts.
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    fileChooserLauncher.launch(Intent.createChooser(intent, "Select"));
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (binding != null) binding.webProgress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (binding != null) binding.webProgress.setVisibility(View.GONE);
            }
        });

        // Back navigates web history first, then leaves the screen.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        binding.webView.loadUrl(url);
    }

    /** Return true if we handled the URL (i.e. WebView should NOT load it). */
    private boolean handleUrl(String url) {
        if (url == null) return false;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false; // let the WebView load site pages normally
        }
        // upi:, tel:, mailto:, whatsapp:, intent: -> hand to the system.
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        if (binding != null && binding.webView != null) {
            binding.webView.destroy();
        }
        super.onDestroy();
        binding = null;
    }
}

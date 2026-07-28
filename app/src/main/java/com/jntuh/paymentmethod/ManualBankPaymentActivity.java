package com.jntuh.paymentmethod;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebSettings;

import androidx.appcompat.app.AppCompatActivity;

import com.jntuh.books.databinding.ActivityManualPaymentBinding;
import com.jntuh.util.BannerAds;
import com.jntuh.util.Method;


public class ManualBankPaymentActivity extends AppCompatActivity {

    Method method;
    ActivityManualPaymentBinding viewManualPaymentBinding;
    String planName, bankInfo;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewManualPaymentBinding = ActivityManualPaymentBinding.inflate(getLayoutInflater());
        setContentView(viewManualPaymentBinding.getRoot());

        method = new Method(ManualBankPaymentActivity.this);
        method.forceRTLIfSupported();

        Intent intent = getIntent();
        planName = intent.getStringExtra("planGateway");
        bankInfo = intent.getStringExtra("bankInfo");

        WebSettings webSettings = viewManualPaymentBinding.wvBankInfo.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setPluginState(WebSettings.PluginState.ON);
        viewManualPaymentBinding.wvBankInfo.setBackgroundColor(Color.TRANSPARENT);
        viewManualPaymentBinding.wvBankInfo.setFocusableInTouchMode(false);
        viewManualPaymentBinding.wvBankInfo.setFocusable(false);

        viewManualPaymentBinding.wvBankInfo.getSettings().setDefaultTextEncodingName("UTF-8");
        String mimeType = "text/html";
        String encoding = "utf-8";

        String text = "<html dir=" + method.isWebViewTextRtl() + "><head>"
                + "<style type=\"text/css\">@font-face {font-family: MyFont;src: url(\"file:///android_asset/fonts/opensansromanregular.ttf\")}body{font-family: MyFont;color: " + method.webViewText() + "font-size: 14px;line-height:1.7;margin-left: 0px;margin-right: 0px;margin-top: 0px;margin-bottom: 0px;padding: 0px;}"
                + "a {color:" + method.webViewLink() + "text-decoration:none}"
                + "</style></head>"
                + "<body>"
                + bankInfo
                + "</body></html>";

        viewManualPaymentBinding.wvBankInfo.loadDataWithBaseURL(null, text, mimeType, encoding, null);


        viewManualPaymentBinding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        viewManualPaymentBinding.toolbarMain.tvToolbarTitle.setText(planName);

        BannerAds.showBannerAds(ManualBankPaymentActivity.this, viewManualPaymentBinding.layoutAds);
    }

}

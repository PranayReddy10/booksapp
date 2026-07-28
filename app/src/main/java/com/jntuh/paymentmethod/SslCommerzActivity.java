package com.jntuh.paymentmethod;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jntuh.books.R;
import com.jntuh.util.Method;
import com.sslwireless.sslcommerzlibrary.model.initializer.SSLCAdditionalInitializer;
import com.sslwireless.sslcommerzlibrary.model.initializer.SSLCommerzInitialization;
import com.sslwireless.sslcommerzlibrary.model.response.SSLCTransactionInfoModel;
import com.sslwireless.sslcommerzlibrary.model.util.SSLCSdkType;
import com.sslwireless.sslcommerzlibrary.view.singleton.IntegrateSSLCommerz;
import com.sslwireless.sslcommerzlibrary.viewmodel.listener.SSLCTransactionResponseListener;

import java.util.Locale;
import java.util.Random;


public class SslCommerzActivity extends AppCompatActivity implements SSLCTransactionResponseListener {

    String planId, planPrice, planCurrency, planGateway, sslStoreId, sslStorePassword;
    Button btnPay;
    Method myApplication;
    ProgressDialog pDialog;
    boolean isSandbox = false;
    SSLCommerzInitialization sslCommerzInitialization;
    SSLCAdditionalInitializer additionalInitialization;
    boolean isRent;
    TextView textView;
    ImageView imageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        pDialog = new ProgressDialog(this);
        myApplication = new Method(SslCommerzActivity.this);

        Intent intent = getIntent();
        planId = intent.getStringExtra("planId");
        planPrice = intent.getStringExtra("planPrice");
        planCurrency = intent.getStringExtra("planCurrency");
        planGateway = intent.getStringExtra("planGateway");
        sslStoreId = intent.getStringExtra("sslStoreId");
        sslStorePassword = intent.getStringExtra("sslStorePass");
        isSandbox = intent.getBooleanExtra("isSandbox", false);
        if (intent.hasExtra("isRent")){
            isRent=intent.getBooleanExtra("isRent",false);
        }

        textView=findViewById(R.id.tvToolbarTitle);
        textView.setText(planGateway);
        imageView=findViewById(R.id.imageArrowBack);
        imageView.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        btnPay = findViewById(R.id.btn_pay);

        initPaymentGateway();

        btnPay.setOnClickListener(view -> initPaymentGateway());
    }

    private void initPaymentGateway() {
        double amount = Double.parseDouble(planPrice);
        sslCommerzInitialization = new SSLCommerzInitialization(sslStoreId, sslStorePassword, amount, planCurrency,
                getTransactionId(), getString(R.string.payment_sslcommerz), isSandbox ? SSLCSdkType.TESTBOX : SSLCSdkType.LIVE);
        additionalInitialization = new SSLCAdditionalInitializer();
        additionalInitialization.setValueA("");

        IntegrateSSLCommerz.getInstance(this).addSSLCommerzInitialization(sslCommerzInitialization).buildApiCall(this);
    }

    public String getTransactionId() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String orderId = String.format(Locale.getDefault(), "%06d", number);
        return "Ssl" + myApplication.getUserId() + orderId;
    }

    @Override
    public void transactionSuccess(SSLCTransactionInfoModel sslcTransactionInfoModel) {
        if (myApplication.isNetworkAvailable()) {
            new Transaction(SslCommerzActivity.this)
                    .purchasedItem(planId,myApplication.getUserId(), sslcTransactionInfoModel.getTranId(), planGateway,isRent);
        } else {
            showError(getString(R.string.internet_connection));
        }
    }

    @Override
    public void transactionFail(String s) {
        showError(s);
    }

    @Override
    public void closed(String s) {
        showError(s);
    }

    private void showError(String Title) {
        new AlertDialog.Builder(SslCommerzActivity.this)
                .setTitle(getString(R.string.payment_sslcommerz))
                .setMessage(Title)
                .setIcon(R.mipmap.app_icon)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // do nothing
                })
                .show();
    }

}

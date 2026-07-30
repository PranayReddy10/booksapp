package com.jntuh.paymentmethod;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jntuh.books.R;
import com.jntuh.books.databinding.ActivityPaymentBinding;
import com.jntuh.util.Method;
import com.flutterwave.raveandroid.RavePayActivity;
import com.flutterwave.raveandroid.RaveUiManager;
import com.flutterwave.raveandroid.rave_java_commons.RaveConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Random;


public class FlutterWaveActivity extends AppCompatActivity{

    String planId, planName, planPrice, planCurrency, planGateway, planfwPublicKey,planfwEncryptKey;
    Button btnPay;
    Method method;
    ProgressDialog pDialog;
    ActivityPaymentBinding paymentBinding;
    boolean isRent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        paymentBinding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(paymentBinding.getRoot());
        method = new Method(FlutterWaveActivity.this);

        paymentBinding.toolbarMain.tvToolbarTitle.setText(getString(R.string.payment_flutterwave));
        paymentBinding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        pDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);
        Intent intent = getIntent();
        planId = intent.getStringExtra("planId");
        planPrice = intent.getStringExtra("planPrice");
        planName = intent.getStringExtra("planName");
        planCurrency = intent.getStringExtra("planCurrency");
        planGateway = intent.getStringExtra("planGateway");
        planfwPublicKey = intent.getStringExtra("planFlutterPublicKey");
        planfwEncryptKey = intent.getStringExtra("planFlutterEnrcyptKey");
        if (intent.hasExtra("isRent")){
            isRent=intent.getBooleanExtra("isRent",false);
        }

        btnPay = findViewById(R.id.btn_pay);

        btnPay.setOnClickListener(view -> startPayment());
        startPayment();
    }
    public void startPayment() {
        RaveUiManager raveUiManager = new RaveUiManager(FlutterWaveActivity.this);
        raveUiManager.setAmount(Double.parseDouble(planPrice))
                .setCurrency(planCurrency) //NGN
                .setEmail(method.getUserEmail())
                .setfName(method.getUserName())
                .setPublicKey(planfwPublicKey)
                .setEncryptionKey(planfwEncryptKey)
                .setTxRef(getTransactionId())
                .acceptAccountPayments(true)
                .shouldDisplayFee(true)
                .acceptCardPayments(true)
                .acceptMpesaPayments(true)
                .acceptAchPayments(true)
                .acceptGHMobileMoneyPayments(true)
                .acceptUgMobileMoneyPayments(true)
                .acceptZmMobileMoneyPayments(true)
                .acceptRwfMobileMoneyPayments(true)
                .acceptSaBankPayments(true)
                .acceptUkPayments(true)
                .acceptBankTransferPayments(true)
                .acceptUssdPayments(true)
                .acceptBarterPayments(true)
                .acceptFrancMobileMoneyPayments(true, "NG")
                .allowSaveCardFeature(true)
                .onStagingEnv(false)
                .isPreAuth(true)
                .showStagingLabel(true);
        raveUiManager.initialize();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RaveConstants.RAVE_REQUEST_CODE && data != null) {
            String message = data.getStringExtra("response");
            if (resultCode == RavePayActivity.RESULT_SUCCESS) {
                try {
                    assert message != null;
                    JSONObject jsonObject = new JSONObject(message);
                    JSONObject jsonData = jsonObject.getJSONObject("data");
                    String status = jsonData.getString("status");
                    if (status.equals("successful")) {
                        String paymentID = jsonData.getString("flwRef");
                        new Transaction(FlutterWaveActivity.this)
                                .purchasedItem(planId, method.getUserId(), paymentID, planGateway,isRent);
                    } else {
                        showError( "Failed " + jsonData.getString("vbvrespmessage"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == RavePayActivity.RESULT_ERROR) {
                showError(getString(R.string.flutterwave_payment_failed));
            } else if (resultCode == RavePayActivity.RESULT_CANCELLED) {
                showError(getString(R.string.flutterwave_payment_cancel));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public String getTransactionId() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String orderId = String.format(Locale.getDefault(), "%06d", number);
        return "fW" + method.getUserId() + orderId;
    }

    private void showError(String Title) {
        new AlertDialog.Builder(FlutterWaveActivity.this)
                .setTitle(getString(R.string.flutterwave_payment_error))
                .setMessage(Title)
                .setIcon(R.mipmap.ic_launcher_round)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .show();
    }

}

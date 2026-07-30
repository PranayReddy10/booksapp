package com.jntuh.paymentmethod;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.ClientTokenCallback;
import com.braintreepayments.api.ClientTokenProvider;
import com.braintreepayments.api.DropInClient;
import com.braintreepayments.api.DropInListener;
import com.braintreepayments.api.DropInRequest;
import com.braintreepayments.api.DropInResult;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.jntuh.books.R;
import com.jntuh.books.databinding.ActivityPaymentBinding;
import com.jntuh.response.BraintreeCheckOutRP;
import com.jntuh.response.PaypalTokenRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PayPalActivity extends AppCompatActivity {

    String planId,planPrice,planCurrency,planGateway,isSandbox;
    String authToken = "";
    ProgressDialog pDialog;
    Method method;
    ActivityPaymentBinding paypalBinding;
    boolean isRent;
    DropInClient dropInClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        paypalBinding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(paypalBinding.getRoot());

        method = new Method(PayPalActivity.this);
        paypalBinding.toolbarMain.tvToolbarTitle.setText(getString(R.string.payment_paypal));
        paypalBinding.toolbarMain.imageFilter.setVisibility(View.GONE);
        paypalBinding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        Intent intent = getIntent();
        planId = intent.getStringExtra("planId");
        planPrice = intent.getStringExtra("planPrice");
        planCurrency = intent.getStringExtra("planCurrency");
        planGateway = intent.getStringExtra("planGateway");
        isSandbox = intent.getStringExtra("isSandbox");
        if (intent.hasExtra("isRent")){
            isRent=intent.getBooleanExtra("isRent",false);
        }

        pDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);

        paypalBinding.btnPay.setOnClickListener(view -> {
            if (!authToken.isEmpty()) {
                launchDropIn();
            } else {
                showError(getString(R.string.paypal_payment_error_3));
            }
        });

        configureDropInClient();
        launchDropIn();
    }
    private void configureDropInClient() {
        dropInClient = new DropInClient(PayPalActivity.this, new TokenProvider());
        dropInClient.setListener(new DropInListener() {
            @Override
            public void onDropInSuccess(@NonNull DropInResult dropInResult) {
                String nNonce = Objects.requireNonNull(dropInResult.getPaymentMethodNonce()).getString();
                checkoutNonce(nNonce);
            }

            @Override
            public void onDropInFailure(@NonNull Exception error) {
                showError(error.getMessage());
            }
        });
    }

    private class TokenProvider implements ClientTokenProvider {
        @Override
        public void getClientToken(@NonNull ClientTokenCallback clientTokenCallback) {
            generateToken(clientTokenCallback);
        }
    }
    private void generateToken(ClientTokenCallback clientTokenCallback) {
        pDialog.show();
        pDialog.setMessage(getResources().getString(R.string.loading));
        pDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(PayPalActivity.this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<PaypalTokenRP> call = apiService.getPaypalTokenData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<PaypalTokenRP>() {
            @Override
            public void onResponse(@NotNull Call<PaypalTokenRP> call, @NotNull Response<PaypalTokenRP> response) {
                try {
                    PaypalTokenRP paypalTokenRP = response.body();

                    if (paypalTokenRP !=null && paypalTokenRP.getItemPaypalTokens().get(0).getSuccess().equals("1")) {
                        authToken = paypalTokenRP.getItemPaypalTokens().get(0).getAuthToken();
                        clientTokenCallback.onSuccess(authToken);
                    } else {
                        method.alertBox(paypalTokenRP.getItemPaypalTokens().get(0).getMsg());
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    //method.alertBox(getResources().getString(R.string.failed_try_again));
                    clientTokenCallback.onFailure(new Exception(getString(R.string.paypal_payment_error_1)));
                }

                pDialog.dismiss();

            }

            @Override
            public void onFailure(@NotNull Call<PaypalTokenRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                pDialog.dismiss();
                clientTokenCallback.onFailure(new Exception(getString(R.string.paypal_payment_error_1)));
            }
        });
    }

    private void launchDropIn() {
        DropInRequest dropInRequest = new DropInRequest();
        dropInRequest.setPayPalRequest(getPaypalRequest());
        dropInClient.launchDropIn(dropInRequest);
    }

    private PayPalCheckoutRequest getPaypalRequest() {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest(planPrice);
        request.setCurrencyCode(planCurrency);
        request.setIntent(PayPalPaymentIntent.SALE);
        return request;
    }

    private void checkoutNonce(String paymentNonce) {
        pDialog.show();
        pDialog.setMessage(getResources().getString(R.string.loading));
        pDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(PayPalActivity.this));
        jsObj.addProperty("payment_nonce", paymentNonce);
        jsObj.addProperty("payment_amount", planPrice);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<BraintreeCheckOutRP> call = apiService.getPaypalCheckOutData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<BraintreeCheckOutRP>() {
            @Override
            public void onResponse(@NotNull Call<BraintreeCheckOutRP> call, @NotNull Response<BraintreeCheckOutRP> response) {
                try {
                    BraintreeCheckOutRP paypalCheckOutRP = response.body();

                     if (paypalCheckOutRP !=null && paypalCheckOutRP.getItemBrainTreeCheckOuts().get(0).getSuccess().equals("1")) {
                        String paymentId = paypalCheckOutRP.getItemBrainTreeCheckOuts().get(0).getPaypal_payment_id(); //objJson.getString("transaction_id")
                        if (method.isNetworkAvailable()) {
                            new Transaction(PayPalActivity.this)
                                    .purchasedItem(planId, method.getUserId(), paymentId, planGateway,isRent);
                        } else {
                            showError(getString(R.string.internet_connection));
                        }
                    } else {
                         assert paypalCheckOutRP != null;
                         showError(paypalCheckOutRP.getItemBrainTreeCheckOuts().get(0).getMsg());
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }

                pDialog.dismiss();

            }

            @Override
            public void onFailure(@NotNull Call<BraintreeCheckOutRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                pDialog.dismiss();
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }

    private void showError(String Title) {
        new AlertDialog.Builder(PayPalActivity.this)
                .setTitle(getString(R.string.paypal_payment_error_4))
                .setMessage(Title)
                .setIcon(R.mipmap.ic_launcher_round)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // do nothing
                })
                .show();
    }

}

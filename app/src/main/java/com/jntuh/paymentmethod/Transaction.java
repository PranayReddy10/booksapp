package com.jntuh.paymentmethod;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jntuh.books.MainActivity;
import com.jntuh.books.R;
import com.jntuh.response.PaymentSuccessRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class Transaction {
    ProgressDialog pDialog;
    Activity mContext;
    Method method;

    public Transaction(Activity context) {
        this.mContext = context;
        pDialog = new ProgressDialog(mContext,R.style.MyAlertDialogStyle);
        method = new Method(mContext);
    }

    public void purchasedItem(String planId, String userId, String paymentId, String paymentGateway, boolean isRent) {
        pDialog.show();
        pDialog.setMessage(mContext.getResources().getString(R.string.loading));
        pDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(mContext));
        jsObj.addProperty(isRent ? "rent_id" : "plan_id", planId);
        jsObj.addProperty("user_id", userId);
        jsObj.addProperty("payment_id", paymentId);
        jsObj.addProperty("payment_gateway", paymentGateway);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<PaymentSuccessRP> call = apiService.getPaymentSuccessData(isRent ? "transaction_rent_add" : "transaction_add", API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<PaymentSuccessRP>() {
            @Override
            public void onResponse(@NotNull Call<PaymentSuccessRP> call, @NotNull Response<PaymentSuccessRP> response) {
                try {

                    PaymentSuccessRP paymentSuccessRP = response.body();

                    if (paymentSuccessRP !=null && paymentSuccessRP.getSuccess().equals("1")) {
                        showSuccessDialog(paymentSuccessRP.getItemSuccesses().get(0).getMsg());
                    } else {
                        method.alertBox(paymentSuccessRP.getItemSuccesses().get(0).getMsg());
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(mContext.getResources().getString(R.string.failed_try_again));
                }

                pDialog.dismiss();

            }

            @Override
            public void onFailure(@NotNull Call<PaymentSuccessRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                pDialog.dismiss();
                method.alertBox(mContext.getResources().getString(R.string.failed_try_again));
            }
        });
    }

    public void showSuccessDialog(String msg) {
        Dialog dialog = new Dialog(mContext,R.style.RoundedCornersDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_payment_success);
        dialog.setCancelable(false);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        dialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        TextView tvSuccessMsg=dialog.findViewById(R.id.tvSuccessMsg);
        MaterialButton btnHome=dialog.findViewById(R.id.btnHome);
        tvSuccessMsg.setText(msg);

        btnHome.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intentMain=new Intent(mContext, MainActivity.class);
            mContext.startActivity(intentMain);
            mContext.finishAffinity();
        });

        dialog.show();
    }
}

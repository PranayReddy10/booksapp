package com.jntuh.paymentmethod;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.cinetpay.androidsdk.CinetPayWebAppInterface;
import com.jntuh.books.R;
import com.jntuh.util.Method;


import org.json.JSONException;
import org.json.JSONObject;

public class MyCinetPayWebAppInterface extends CinetPayWebAppInterface {
    String planId;
    String gatewayName;
    boolean isRent;
    Context context;
    Method method;

    public MyCinetPayWebAppInterface(Context c,
                                     String apikey,
                                     String site_id,
                                     String transaction_id,
                                     int amount,
                                     String currency,
                                     String description,
                                     String gatewayName,
                                     String planId,
                                     boolean isRent) {
        super(c, apikey, site_id, transaction_id, amount, currency, description);
        this.planId = planId;
        this.gatewayName = gatewayName;
        this.isRent = isRent;
        this.context = c;
        method = new Method((Activity) context);
    }

    @Override
    @JavascriptInterface
    public void onResponse(String response) {
        Log.d("MyCinetPayWebApp", response);
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.has("status")) {
                String statusSuccess = jsonObject.getString("status");
                String transId = jsonObject.getString("operator_id");
                if (statusSuccess.equals("ACCEPTED")) {
                    new Transaction((Activity) context)
                            .purchasedItem(planId, method.getUserId(), transId, gatewayName, isRent);
                    Toast.makeText(context, context.getString(R.string.cinetpay_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.cinetpay_failed), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    @JavascriptInterface
    public void onError(String response) {
        Log.d("onerror", response);
    }
}

package com.jntuh.paymentmethod;

import android.content.Intent;
import android.os.Bundle;

import com.cinetpay.androidsdk.CinetPayActivity;

public class MyCinetPayActivity extends CinetPayActivity {

    boolean isRent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        String api_key = intent.getStringExtra(KEY_API_KEY);
        String site_id = intent.getStringExtra(KEY_SITE_ID);
        String transaction_id = intent.getStringExtra(KEY_TRANSACTION_ID);
        int amount = intent.getIntExtra(KEY_AMOUNT, 0);
        String currency = intent.getStringExtra(KEY_CURRENCY);
        String description = intent.getStringExtra(KEY_DESCRIPTION);
        String channels = intent.getStringExtra(KEY_CHANNELS);
        String customer_name = intent.getStringExtra(KEY_CUSTOMER_NAME);
        String customer_surname = intent.getStringExtra(KEY_CUSTOMER_SURNAME);
        String customer_email = intent.getStringExtra(KEY_CUSTOMER_EMAIL);
        String customer_address = intent.getStringExtra(KEY_CUSTOMER_ADDRESS);
        String customer_phone_number = intent.getStringExtra(KEY_CUSTOMER_PHONE_NUMBER);
        String customer_city = intent.getStringExtra(KEY_CUSTOMER_CITY);
        String customer_country = intent.getStringExtra(KEY_CUSTOMER_COUNTRY);
        String customer_zip_code = intent.getStringExtra(KEY_CUSTOMER_ZIP_CODE);
        String customer_gateway_name = intent.getStringExtra("planGateway");
        String customer_plan_id = intent.getStringExtra("PlanId");
        if (intent.hasExtra("isRent")){
            isRent=intent.getBooleanExtra("isRent",false);
        }

        mWebView
                .addJavascriptInterface(
                        new MyCinetPayWebAppInterface(
                                this,
                                api_key,        // obligatoire
                                site_id,        // obligatoire
                                transaction_id, // obligatoire
                                amount,         // obligatoire
                                currency,       // obligatoire
                                description ,    // obligatoire
                                customer_gateway_name ,    // obligatoire
                                customer_plan_id ,    // obligatoire
                                isRent     // obligatoire
                        )
                                .setChannels(channels)
                                // obligatoire pour les paiements par carte
                                .setCustomerName(customer_name)
                                // obligatoire pour les paiements par carte
                                .setCustomerSurname(customer_surname)
                                // obligatoire pour les paiements par carte
                                .setCustomerEmail(customer_email)
                                // obligatoire pour les paiements par carte
                                .setCustomerAddress(customer_address)
                                // obligatoire pour les paiements par carte
                                .setCustomerPhoneNumber(customer_phone_number)
                                // obligatoire pour les paiements par carte
                                .setCustomerCity(customer_city)
                                // obligatoire pour les paiements par carte (code ISO 2 chiffres: CI, BF, US...)
                                .setCustomerCountry(customer_country)
                                // obligatoire pour les paiements par carte
                                .setCustomerZipCode(customer_zip_code)
                                // obligqtoire si le pays est US (Etats-Unis) ou CA (Canada)
                                /*.setCustomerState("A_STATE")*/,
                        "Android");
    }

}

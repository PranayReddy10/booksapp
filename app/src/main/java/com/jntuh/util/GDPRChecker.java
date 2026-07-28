package com.jntuh.util;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;


public class GDPRChecker {
    private static final String TAG = "GDPRChecker";
    private Activity activity;
    private ConsentInformation consentInformation;


    protected GDPRChecker(Activity activity) {
        this.activity = activity;
        this.consentInformation = UserMessagingPlatform.getConsentInformation(activity);
    }

    public GDPRChecker() {
    }

    public GDPRChecker withContext(Activity activity) {
        return new GDPRChecker(activity);
    }

    public void check() {
        initGDPR();
    }

    private void initGDPR() {
        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .setForceTesting(true)
                .build();

        ConsentRequestParameters parameters = new ConsentRequestParameters.Builder()
                //.setConsentDebugSettings(debugSettings) // comment this line for production it is just used for testing outside eea
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation.requestConsentInfoUpdate(activity, parameters,
                new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    }
                }, new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                    @Override
                    public void onConsentInfoUpdateFailure(@NonNull FormError formError) {
                        Log.e(TAG, "onFailedToUpdateConsentInfo: " + formError.getMessage());
                    }
                });
    }

    public void loadForm() {
        UserMessagingPlatform.loadConsentForm(activity, new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
            @Override
            public void onConsentFormLoadSuccess(@NonNull ConsentForm consentForm) {
                if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED || consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.UNKNOWN) {
                    consentForm.show(activity, new ConsentForm.OnConsentFormDismissedListener() {
                        @Override
                        public void onConsentFormDismissed(@Nullable FormError formError) {
                            if (consentInformation.getConsentStatus() != ConsentInformation.ConsentStatus.OBTAINED) {
                                // App can start requesting ads.
                                loadForm();
                            }
                        }
                    });
                }
            }
        }, new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
            @Override
            public void onConsentFormLoadFailure(@NonNull FormError formError) {
                loadForm();
            }
        });
    }


}

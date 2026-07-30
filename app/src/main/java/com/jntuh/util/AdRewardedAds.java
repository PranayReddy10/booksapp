package com.jntuh.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.jntuh.books.R;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;

/**
 * Rewarded ad manager for the "Read Book" gate.
 *
 * Contract:
 *  - onReward()  -> the user earned the reward (video watched) -> open the book.
 *  - onFailed()  -> ad failed to load / show, OR user dismissed without earning.
 *
 * FAIL-TO-LOAD POLICY: LENIENT. If the network can't load a rewarded ad we call
 * onReward() so the user is never blocked from reading by a missing ad. If you want
 * STRICT behaviour (block + retry) instead, change the onFailed() calls inside the
 * load-failure branches to onFailed() handling on the caller side. The caller decides
 * what onFailed means; see BookDetailsActivity.
 */
public class AdRewardedAds {

    public static ProgressDialog pDialog;

    public interface OnRewardListener {
        void onReward();

        void onFailed();
    }

    // Tracks whether the reward was actually earned during this show, so dismissal
    // callbacks know whether to open the book or not.
    private static boolean earned = false;

    public static void showRewardedAd(Context context, OnRewardListener listener) {
        // If rewarded is globally off or no id, just proceed (lenient).
        if (!Constant.isRewarded || Constant.rewardedId == null || Constant.rewardedId.isEmpty()) {
            listener.onReward();
            return;
        }

        earned = false;
        String network = Constant.adNetworkType == null ? "" : Constant.adNetworkType;

        switch (network) {
            case "Admob":
                showAdmob(context, listener);
                break;
            case "Facebook":
                showFacebook(context, listener);
                break;
            case "Unity Ads":
                showUnity(context, listener);
                break;
            case "StartApp":
                showStartApp(context, listener);
                break;
            case "AppLovins MAX":
                showAppLovin(context, listener);
                break;
            case "Wortise":
                showWortise(context, listener);
                break;
            default:
                // Unknown network -> don't block the user.
                listener.onReward();
                break;
        }
    }

    // ---------------- AdMob ----------------
    private static void showAdmob(Context context, OnRewardListener listener) {
        Loading(context);
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, Constant.rewardedId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                dismissLoading();
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        if (earned) listener.onReward();
                        else listener.onFailed();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                        // Couldn't show -> lenient, let them read.
                        listener.onReward();
                    }
                });
                rewardedAd.show((Activity) context, rewardItem -> earned = true);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                dismissLoading();
                // Lenient: failed to load -> open anyway.
                listener.onReward();
            }
        });
    }

    // ---------------- Facebook ----------------
    private static void showFacebook(Context context, OnRewardListener listener) {
        Loading(context);
        final RewardedVideoAd rewardedVideoAd = new RewardedVideoAd(context, Constant.rewardedId);
        RewardedVideoAdListener adListener = new RewardedVideoAdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                dismissLoading();
                listener.onReward(); // lenient
            }

            @Override
            public void onAdLoaded(Ad ad) {
                dismissLoading();
                if (rewardedVideoAd.isAdLoaded() && !rewardedVideoAd.isAdInvalidated()) {
                    rewardedVideoAd.show();
                } else {
                    listener.onReward();
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
            }

            @Override
            public void onLoggingImpression(Ad ad) {
            }

            @Override
            public void onRewardedVideoCompleted() {
                earned = true;
            }

            @Override
            public void onRewardedVideoClosed() {
                if (earned) listener.onReward();
                else listener.onFailed();
            }
        };
        rewardedVideoAd.loadAd(rewardedVideoAd.buildLoadAdConfig().withAdListener(adListener).build());
    }

    // ---------------- Unity Ads ----------------
    private static void showUnity(Context context, OnRewardListener listener) {
        UnityAds.show((Activity) context, Constant.rewardedId, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                listener.onReward(); // lenient
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {
            }

            @Override
            public void onUnityAdsShowClick(String placementId) {
            }

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    listener.onReward();
                } else {
                    listener.onFailed();
                }
            }
        });
    }

    // ---------------- StartApp ----------------
    private static void showStartApp(Context context, OnRewardListener listener) {
        Loading(context);
        final boolean[] rewarded = {false};
        final StartAppAd startAppAd = new StartAppAd(context);
        startAppAd.setVideoListener(() -> rewarded[0] = true);
        startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override
            public void onReceiveAd(@NonNull com.startapp.sdk.adsbase.Ad ad) {
                dismissLoading();
                startAppAd.showAd(new AdDisplayListener() {
                    @Override
                    public void adHidden(com.startapp.sdk.adsbase.Ad ad) {
                        if (rewarded[0]) listener.onReward();
                        else listener.onFailed();
                    }

                    @Override
                    public void adDisplayed(com.startapp.sdk.adsbase.Ad ad) {
                    }

                    @Override
                    public void adClicked(com.startapp.sdk.adsbase.Ad ad) {
                    }

                    @Override
                    public void adNotDisplayed(com.startapp.sdk.adsbase.Ad ad) {
                        listener.onReward(); // lenient
                    }
                });
            }

            @Override
            public void onFailedToReceiveAd(@Nullable com.startapp.sdk.adsbase.Ad ad) {
                dismissLoading();
                listener.onReward(); // lenient
            }
        });
    }

    // ---------------- AppLovin MAX ----------------
    private static void showAppLovin(Context context, OnRewardListener listener) {
        Loading(context);
        final MaxRewardedAd rewardedAd = MaxRewardedAd.getInstance(Constant.rewardedId, (Activity) context);
        rewardedAd.setListener(new MaxRewardedAdListener() {
            @Override
            public void onAdLoaded(MaxAd ad) {
                dismissLoading();
                if (rewardedAd.isReady()) rewardedAd.showAd();
                else listener.onReward();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {
            }

            @Override
            public void onAdHidden(MaxAd ad) {
                if (earned) listener.onReward();
                else listener.onFailed();
            }

            @Override
            public void onAdClicked(MaxAd ad) {
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                dismissLoading();
                listener.onReward(); // lenient
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                dismissLoading();
                listener.onReward(); // lenient
            }

            @Override
            public void onUserRewarded(MaxAd ad, MaxReward reward) {
                earned = true;
            }
        });
        rewardedAd.loadAd();
    }

    // ---------------- Wortise ----------------
    // Wortise rewarded ads are intentionally NOT implemented (SDK listener API varies by
    // version and caused build issues). Falls through to lenient: open the book directly.
    private static void showWortise(Context context, OnRewardListener listener) {
        listener.onReward();
    }

    // ---------------- helpers ----------------
    public static void Loading(Context context) {
        try {
            pDialog = new ProgressDialog(context, R.style.MyAlertDialogStyle);
            pDialog.setMessage(context.getResources().getString(R.string.loading));
            pDialog.setCancelable(false);
            pDialog.show();
        } catch (Exception ignored) {
        }
    }

    private static void dismissLoading() {
        try {
            if (pDialog != null && pDialog.isShowing()) pDialog.dismiss();
        } catch (Exception ignored) {
        }
    }
}

package com.jntuh.util;


import com.jntuh.item.AdsInfo;
import com.jntuh.item.AppList;

public class Constant {

    public static String constantCurrency;

    // Public website host used for shareable links and App Links (see assetlinks.json).
    public static final String MEDIA_WEB_HOST = "read.jntubooks.in";
    public static final String MEDIA_WEB_BASE = "https://read.jntubooks.in";

    /**
     * Slugify a title the same way the public website does (Laravel Str::slug):
     * lowercase, non-alphanumeric runs collapse to a single hyphen, trimmed.
     * Used to build shareable URLs like read.jntubooks.in/book/{id}-{slug}.
     */
    public static String webSlug(String title) {
        if (title == null) return "";
        String s = title.toLowerCase().trim();
        s = s.replaceAll("[^a-z0-9]+", "-");   // non-alphanumerics -> hyphen
        s = s.replaceAll("^-+|-+$", "");        // trim leading/trailing hyphens
        return s;
    }
    public static String webViewText = "#41414199;";
    public static String webViewTextDark = "#FFFFFF;";

    public static String webViewTextAuthor = "#4d506ccc;";
    public static String webViewTextDarkAuthor = "#FFFFFF;";

    public static String webViewLink = "#99414141;";
    public static String webViewLinkDark = "#FFFFFF;";

    public static String webViewTextAbout = "#65637BE5;";
    public static String webViewTextAboutDark = "#FFFFFF;";

    public static int AD_COUNT = 0;

    public static AppList appListData;

    public static boolean isNative= false;
    public static boolean isBanner= false;
    public static boolean isInterstitial= false;
    public static boolean isRewarded= false;

    public static AdsInfo adsInfo;

    public static int interstitialClick,nativePosition;
    public static String bannerId,interstitialId,nativeId,publisherId,
            adNetworkType,rewardedId;

    public static boolean isAppUpdate = false, isAppUpdateCancel = false;
    public static int appUpdateVersion;
    public static String  appUpdateUrl, appUpdateDesc;
}

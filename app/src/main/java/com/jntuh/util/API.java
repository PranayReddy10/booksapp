package com.jntuh.util;

import android.app.Activity;
import android.util.Base64;

import com.google.gson.annotations.SerializedName;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class API {

    @SerializedName("sign")
    private String sign;
    @SerializedName("salt")
    private String salt;
    @SerializedName("package_name")
    private String packageName;
    /**
     * The signed-in student, on every call. The server uses it to show only
     * books for that student's university. Left out when nobody is signed in.
     */
    @SerializedName("viewer_id")
    private String viewerId;

    public API(Activity activity) {
        String apiKey = "viaviweb";
        salt = "" + getRandomSalt();
        sign = md5(apiKey + salt);
        packageName = activity.getApplication().getPackageName();
        Method method = new Method(activity);
        if (method.getIsLogin() && !method.getUserId().isEmpty()) {
            viewerId = method.getUserId();
        }
    }

    private int getRandomSalt() {
        Random random = new Random();
        return random.nextInt(900);
    }

    private String md5(String input) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(String.format("%02x", messageDigest[i]));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String toBase64(String input) {
        byte[] encodeValue = Base64.encode(input.getBytes(), Base64.DEFAULT);
        return new String(encodeValue);
    }

}

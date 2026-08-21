package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/** A gift card the student has redeemed coins for. */
public class CoinCardItem implements Serializable {

    @SerializedName("redemption_id")
    private String redemption_id;

    /** Empty until the shop has actually minted the coupon. */
    @SerializedName("code")
    private String code;

    @SerializedName("coins")
    private int coins;

    @SerializedName("amount")
    private String amount;

    @SerializedName("status")
    private String status;

    @SerializedName("date")
    private String date;

    @SerializedName("msg")
    private String msg;

    @SerializedName("success")
    private String success;

    public String getRedemption_id() { return redemption_id; }

    public String getCode() { return code; }

    public int getCoins() { return coins; }

    public String getAmount() { return amount; }

    public String getStatus() { return status; }

    public String getDate() { return date; }

    public String getMsg() { return msg; }

    public String getSuccess() { return success; }
}

package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/** EBOOK_APP[0] of coins_summary: the whole My Coins screen in one object. */
public class CoinSummaryItem implements Serializable {

    @SerializedName("enabled")
    private int enabled;

    @SerializedName("balance")
    private int balance;

    @SerializedName("balance_value")
    private String balance_value;

    @SerializedName("total_earned")
    private int total_earned;

    @SerializedName("total_redeemed")
    private int total_redeemed;

    @SerializedName("coins_per_read")
    private int coins_per_read;

    @SerializedName("coins_per_upload")
    private int coins_per_upload;

    @SerializedName("min_redeem")
    private int min_redeem;

    @SerializedName("can_redeem")
    private int can_redeem;

    @SerializedName("currency")
    private String currency;

    @SerializedName("books")
    private List<CoinBookItem> books;

    /** "setup" (migration not run) or "disabled" when enabled is 0. */
    @SerializedName("reason")
    private String reason;

    @SerializedName("msg")
    private String msg;

    @SerializedName("success")
    private String success;

    public int getEnabled() { return enabled; }

    public int getBalance() { return balance; }

    public String getBalance_value() { return balance_value; }

    public int getTotal_earned() { return total_earned; }

    public int getTotal_redeemed() { return total_redeemed; }

    public int getCoins_per_read() { return coins_per_read; }

    public int getCoins_per_upload() { return coins_per_upload; }

    public int getMin_redeem() { return min_redeem; }

    public int getCan_redeem() { return can_redeem; }

    public String getCurrency() { return currency; }

    public List<CoinBookItem> getBooks() { return books; }

    public String getReason() { return reason; }

    public String getMsg() { return msg; }

    public String getSuccess() { return success; }
}

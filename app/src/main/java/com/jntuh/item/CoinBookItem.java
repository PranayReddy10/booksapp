package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/** One uploaded book on the My Coins screen: how it did and what it paid. */
public class CoinBookItem implements Serializable {

    @SerializedName("book_id")
    private String book_id;

    @SerializedName("title")
    private String title;

    @SerializedName("image")
    private String image;

    @SerializedName("status")
    private String status;

    @SerializedName("views")
    private int views;

    /** Distinct readers who earned the uploader coins. */
    @SerializedName("reads")
    private int reads;

    @SerializedName("coins")
    private int coins;

    public String getBook_id() { return book_id; }

    public String getTitle() { return title; }

    public String getImage() { return image; }

    public String getStatus() { return status; }

    public int getViews() { return views; }

    public int getReads() { return reads; }

    public int getCoins() { return coins; }
}

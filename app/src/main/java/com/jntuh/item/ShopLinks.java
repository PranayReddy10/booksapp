package com.jntuh.item;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ShopLinks implements Serializable {

    @SerializedName("shop_url")
    private String shop_url;

    @SerializedName("track_url")
    private String track_url;

    public String getShop_url() { return shop_url; }
    public String getTrack_url() { return track_url; }
}

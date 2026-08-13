package com.jntuh.response;

import com.google.gson.annotations.SerializedName;
import com.jntuh.item.ShopLinks;
import java.io.Serializable;
import java.util.List;

public class ShopLinksRP implements Serializable {

    @SerializedName("EBOOK_APP")
    private List<ShopLinks> links;

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    public List<ShopLinks> getLinks() { return links; }
    public String getSuccess() { return success; }
}

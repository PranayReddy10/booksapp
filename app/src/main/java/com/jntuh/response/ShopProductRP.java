package com.jntuh.response;

import com.google.gson.annotations.SerializedName;
import com.jntuh.item.ShopProduct;
import java.io.Serializable;
import java.util.List;

public class ShopProductRP implements Serializable {

    @SerializedName("EBOOK_APP")
    private List<ShopProduct> products;

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    public List<ShopProduct> getProducts() { return products; }
    public String getStatus_code() { return status_code; }
    public String getSuccess() { return success; }
}

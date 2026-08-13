package com.jntuh.response;

import com.google.gson.annotations.SerializedName;
import com.jntuh.item.ShopCategory;
import java.io.Serializable;
import java.util.List;

public class ShopCategoryRP implements Serializable {

    @SerializedName("EBOOK_APP")
    private List<ShopCategory> categories;

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    public List<ShopCategory> getCategories() { return categories; }
    public String getStatus_code() { return status_code; }
    public String getSuccess() { return success; }
}

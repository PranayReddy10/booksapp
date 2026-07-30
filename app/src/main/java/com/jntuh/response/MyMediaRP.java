package com.jntuh.response;

import com.jntuh.item.MyMediaItem;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MyMediaRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<MyMediaItem> myMediaItems;

    public String getStatus_code() {
        return status_code;
    }

    public void setStatus_code(String status_code) {
        this.status_code = status_code;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public List<MyMediaItem> getMyMediaItems() {
        return myMediaItems;
    }

    public void setMyMediaItems(List<MyMediaItem> myMediaItems) {
        this.myMediaItems = myMediaItems;
    }
}

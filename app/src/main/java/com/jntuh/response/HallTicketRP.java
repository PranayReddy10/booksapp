package com.jntuh.response;

import com.google.gson.annotations.SerializedName;
import com.jntuh.item.HallTicketItem;

import java.io.Serializable;
import java.util.List;

public class HallTicketRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<HallTicketItem> ebookApp;

    public String getStatus_code() {
        return status_code;
    }

    public String getSuccess() {
        return success;
    }

    public List<HallTicketItem> getEbookApp() {
        return ebookApp;
    }
}

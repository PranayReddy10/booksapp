package com.jntuh.response;

import com.google.gson.annotations.SerializedName;
import com.jntuh.item.CoinSummaryItem;

import java.io.Serializable;
import java.util.List;

public class CoinSummaryRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<CoinSummaryItem> ebookApp;

    public String getStatus_code() { return status_code; }

    public String getSuccess() { return success; }

    public List<CoinSummaryItem> getEbookApp() { return ebookApp; }
}

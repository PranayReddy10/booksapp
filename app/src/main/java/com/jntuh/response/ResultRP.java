package com.jntuh.response;

import com.jntuh.item.ResultItem;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ResultRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<ResultItem> resultItems;

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

    public List<ResultItem> getResultItems() {
        return resultItems;
    }

    public void setResultItems(List<ResultItem> resultItems) {
        this.resultItems = resultItems;
    }
}

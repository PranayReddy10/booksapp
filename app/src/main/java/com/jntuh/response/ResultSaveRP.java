package com.jntuh.response;

import com.jntuh.item.SimpleMsg;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ResultSaveRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<SimpleMsg> ebookApp;

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

    public List<SimpleMsg> getEbookApp() {
        return ebookApp;
    }

    public void setEbookApp(List<SimpleMsg> ebookApp) {
        this.ebookApp = ebookApp;
    }
}

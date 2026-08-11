package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * EBOOK_APP[0] payload of report_generate: the generated PNG URL + share link.
 */
public class CardItem implements Serializable {

    @SerializedName("report_image_url")
    private String report_image_url;

    @SerializedName("share_url")
    private String share_url;

    @SerializedName("verified")
    private int verified;

    @SerializedName("msg")
    private String msg;

    @SerializedName("success")
    private String success;

    public String getReport_image_url() {
        return report_image_url;
    }

    public void setReport_image_url(String report_image_url) {
        this.report_image_url = report_image_url;
    }

    public String getShare_url() {
        return share_url;
    }

    public void setShare_url(String share_url) {
        this.share_url = share_url;
    }

    public int getVerified() {
        return verified;
    }

    public void setVerified(int verified) {
        this.verified = verified;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }
}

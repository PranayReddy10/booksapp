package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MyUploadList implements Serializable {

    @SerializedName("post_id")
    private String post_id;

    @SerializedName("post_title")
    private String post_title;

    @SerializedName("post_image")
    private String post_image;

    @SerializedName("cover_color")
    private String cover_color;

    @SerializedName("upload_status")
    private String upload_status;

    @SerializedName("reject_reason")
    private String reject_reason;

    /** 0 when the coins feature is off, so the row can hide the earnings line. */
    @SerializedName("coins_enabled")
    private int coins_enabled;

    @SerializedName("total_views")
    private int total_views;

    /** Distinct readers who earned coins, which is not the same as views. */
    @SerializedName("reader_count")
    private int reader_count;

    @SerializedName("coins_earned")
    private int coins_earned;

    public int getCoins_enabled() {
        return coins_enabled;
    }

    public int getTotal_views() {
        return total_views;
    }

    public int getReader_count() {
        return reader_count;
    }

    public int getCoins_earned() {
        return coins_earned;
    }

    public String getPost_id() {
        return post_id;
    }

    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public String getPost_title() {
        return post_title;
    }

    public void setPost_title(String post_title) {
        this.post_title = post_title;
    }

    public String getPost_image() {
        return post_image;
    }

    public void setPost_image(String post_image) {
        this.post_image = post_image;
    }

    public String getCover_color() {
        return cover_color;
    }

    public String getUpload_status() {
        return upload_status;
    }

    public void setUpload_status(String upload_status) {
        this.upload_status = upload_status;
    }

    public String getReject_reason() {
        return reject_reason;
    }

    public void setReject_reason(String reject_reason) {
        this.reject_reason = reject_reason;
    }
}

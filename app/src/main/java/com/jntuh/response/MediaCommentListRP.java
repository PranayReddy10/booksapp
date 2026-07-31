package com.jntuh.response;

import com.jntuh.item.MediaCommentItem;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MediaCommentListRP implements Serializable {

    @SerializedName("EBOOK_APP")
    private List<MediaCommentItem> commentItems;

    @SerializedName("total_pages")
    private int total_pages;

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    public List<MediaCommentItem> getCommentItems() {
        return commentItems;
    }

    public void setCommentItems(List<MediaCommentItem> commentItems) {
        this.commentItems = commentItems;
    }

    public int getTotal_pages() {
        return total_pages;
    }

    public void setTotal_pages(int total_pages) {
        this.total_pages = total_pages;
    }

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
}

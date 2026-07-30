package com.jntuh.response;

import com.jntuh.item.MediaItem;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MediaFeedRP implements Serializable {

    @SerializedName("EBOOK_APP")
    private List<MediaItem> mediaItems;

    @SerializedName("total_pages")
    private int total_pages;

    @SerializedName("current_page")
    private int current_page;

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }

    public void setMediaItems(List<MediaItem> mediaItems) {
        this.mediaItems = mediaItems;
    }

    public int getTotal_pages() {
        return total_pages;
    }

    public void setTotal_pages(int total_pages) {
        this.total_pages = total_pages;
    }

    public int getCurrent_page() {
        return current_page;
    }

    public void setCurrent_page(int current_page) {
        this.current_page = current_page;
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

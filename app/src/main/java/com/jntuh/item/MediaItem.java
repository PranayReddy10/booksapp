package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

// One post in the media feed (photo or video). Keys mirror the media_feed API.
public class MediaItem implements Serializable {

    @SerializedName("post_id")
    private String post_id;

    @SerializedName("media_type")
    private String media_type;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("file_url")
    private String file_url;

    @SerializedName("thumb_url")
    private String thumb_url;

    @SerializedName("uploaded_by")
    private String uploaded_by;

    @SerializedName("is_admin")
    private String is_admin;

    @SerializedName("view_count")
    private String view_count;

    @SerializedName("created_at")
    private String created_at;

    public String getPost_id() {
        return post_id;
    }

    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public String getMedia_type() {
        return media_type;
    }

    public void setMedia_type(String media_type) {
        this.media_type = media_type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFile_url() {
        return file_url;
    }

    public void setFile_url(String file_url) {
        this.file_url = file_url;
    }

    public String getThumb_url() {
        return thumb_url;
    }

    public void setThumb_url(String thumb_url) {
        this.thumb_url = thumb_url;
    }

    public String getUploaded_by() {
        return uploaded_by;
    }

    public void setUploaded_by(String uploaded_by) {
        this.uploaded_by = uploaded_by;
    }

    public String getIs_admin() {
        return is_admin;
    }

    public void setIs_admin(String is_admin) {
        this.is_admin = is_admin;
    }

    public String getView_count() {
        return view_count;
    }

    public void setView_count(String view_count) {
        this.view_count = view_count;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}

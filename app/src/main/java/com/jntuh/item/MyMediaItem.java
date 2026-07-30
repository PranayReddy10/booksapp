package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

// A user's own media post with its moderation status. Mirrors my_uploaded_media.
public class MyMediaItem implements Serializable {

    @SerializedName("post_id")
    private String post_id;

    @SerializedName("media_type")
    private String media_type;

    @SerializedName("title")
    private String title;

    @SerializedName("file_url")
    private String file_url;

    @SerializedName("thumb_url")
    private String thumb_url;

    @SerializedName("upload_status")
    private String upload_status;

    @SerializedName("reject_reason")
    private String reject_reason;

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

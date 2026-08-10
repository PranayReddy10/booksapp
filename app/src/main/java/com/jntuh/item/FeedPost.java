package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

public class FeedPost {

    @SerializedName("post_id")
    private String post_id;

    @SerializedName("media_type")
    private String media_type;

    @SerializedName("title")
    private String title;

    @SerializedName("thumb_url")
    private String thumb_url;

    @SerializedName("file_url")
    private String file_url;

    public String getPost_id() {
        return post_id;
    }

    public String getMedia_type() {
        return media_type;
    }

    public String getTitle() {
        return title;
    }

    public String getThumb_url() {
        return thumb_url;
    }

    public String getFile_url() {
        return file_url;
    }
}

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

    @SerializedName("link_url")
    private String link_url;

    @SerializedName("file_url")
    private String file_url;

    @SerializedName("thumb_url")
    private String thumb_url;

    @SerializedName("uploaded_by")
    private String uploaded_by;

    @SerializedName("is_admin")
    private String is_admin;

    @SerializedName("show_views")
    private String show_views;

    @SerializedName("allow_likes")
    private String allow_likes;

    @SerializedName("allow_comments")
    private String allow_comments;

    @SerializedName("view_count")
    private String view_count;

    @SerializedName("like_count")
    private String like_count;

    @SerializedName("comment_count")
    private String comment_count;

    @SerializedName("is_liked")
    private String is_liked;

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

    public String getLink_url() {
        return link_url;
    }

    public void setLink_url(String link_url) {
        this.link_url = link_url;
    }

    public String getShow_views() {
        return show_views;
    }

    public void setShow_views(String show_views) {
        this.show_views = show_views;
    }

    public String getAllow_likes() {
        return allow_likes;
    }

    public void setAllow_likes(String allow_likes) {
        this.allow_likes = allow_likes;
    }

    public String getAllow_comments() {
        return allow_comments;
    }

    public void setAllow_comments(String allow_comments) {
        this.allow_comments = allow_comments;
    }

    public String getLike_count() {
        return like_count;
    }

    public void setLike_count(String like_count) {
        this.like_count = like_count;
    }

    public String getComment_count() {
        return comment_count;
    }

    public void setComment_count(String comment_count) {
        this.comment_count = comment_count;
    }

    public String getIs_liked() {
        return is_liked;
    }

    public void setIs_liked(String is_liked) {
        this.is_liked = is_liked;
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

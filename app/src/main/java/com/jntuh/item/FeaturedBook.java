package com.jntuh.item;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class FeaturedBook implements Serializable {

    @SerializedName("post_id")
    @Expose
    private String postId;
    @SerializedName("post_title")
    @Expose
    private String postTitle;
    @SerializedName("post_image")
    @Expose
    private String postImage;
    @SerializedName("author_name")
    @Expose
    private String authorName;


    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostImage() {
        return postImage;
    }

    public void setPostImage(String postImage) {
        this.postImage = postImage;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
}

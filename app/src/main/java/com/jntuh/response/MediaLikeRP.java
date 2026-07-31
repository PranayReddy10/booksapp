package com.jntuh.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MediaLikeRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<ItemLike> itemLikeList;

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

    public List<ItemLike> getItemLikeList() {
        return itemLikeList;
    }

    public void setItemLikeList(List<ItemLike> itemLikeList) {
        this.itemLikeList = itemLikeList;
    }

    public static class ItemLike implements Serializable {

        @SerializedName("success")
        private String success;

        @SerializedName("msg")
        private String msg;

        @SerializedName("is_liked")
        private String is_liked;

        @SerializedName("like_count")
        private String like_count;

        public String getSuccess() {
            return success;
        }

        public void setSuccess(String success) {
            this.success = success;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getIs_liked() {
            return is_liked;
        }

        public void setIs_liked(String is_liked) {
            this.is_liked = is_liked;
        }

        public String getLike_count() {
            return like_count;
        }

        public void setLike_count(String like_count) {
            this.like_count = like_count;
        }
    }
}

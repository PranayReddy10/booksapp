package com.jntuh.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MediaCommentRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<ItemComment> itemCommentList;

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

    public List<ItemComment> getItemCommentList() {
        return itemCommentList;
    }

    public void setItemCommentList(List<ItemComment> itemCommentList) {
        this.itemCommentList = itemCommentList;
    }

    public static class ItemComment implements Serializable {

        @SerializedName("success")
        private String success;

        @SerializedName("msg")
        private String msg;

        @SerializedName("comment_id")
        private String comment_id;

        @SerializedName("comment_count")
        private String comment_count;

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

        public String getComment_id() {
            return comment_id;
        }

        public void setComment_id(String comment_id) {
            this.comment_id = comment_id;
        }

        public String getComment_count() {
            return comment_count;
        }

        public void setComment_count(String comment_count) {
            this.comment_count = comment_count;
        }
    }
}

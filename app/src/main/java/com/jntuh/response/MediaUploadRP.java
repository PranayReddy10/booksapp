package com.jntuh.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MediaUploadRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<ItemMediaUpload> itemMediaUploadList;

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

    public List<ItemMediaUpload> getItemMediaUploadList() {
        return itemMediaUploadList;
    }

    public void setItemMediaUploadList(List<ItemMediaUpload> itemMediaUploadList) {
        this.itemMediaUploadList = itemMediaUploadList;
    }

    public static class ItemMediaUpload implements Serializable {

        @SerializedName("success")
        private String success;

        @SerializedName("msg")
        private String msg;

        @SerializedName("post_id")
        private String post_id;

        @SerializedName("auto_approved")
        private String auto_approved;

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

        public String getPost_id() {
            return post_id;
        }

        public void setPost_id(String post_id) {
            this.post_id = post_id;
        }

        public String getAuto_approved() {
            return auto_approved;
        }

        public void setAuto_approved(String auto_approved) {
            this.auto_approved = auto_approved;
        }
    }
}

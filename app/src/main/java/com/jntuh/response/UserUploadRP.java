package com.jntuh.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class UserUploadRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<ItemUserUpload> itemUserUploadList;

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

    public List<ItemUserUpload> getItemUserUploadList() {
        return itemUserUploadList;
    }

    public void setItemUserUploadList(List<ItemUserUpload> itemUserUploadList) {
        this.itemUserUploadList = itemUserUploadList;
    }

    public static class ItemUserUpload implements Serializable {

        @SerializedName("success")
        private String success;

        @SerializedName("msg")
        private String msg;

        @SerializedName("book_id")
        private String book_id;

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

        public String getBook_id() {
            return book_id;
        }

        public void setBook_id(String book_id) {
            this.book_id = book_id;
        }
    }
}

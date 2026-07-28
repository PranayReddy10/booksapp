package com.jntuh.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class DeleteAccRP implements Serializable {


    @SerializedName("status_code")
    private String status_code;

    @SerializedName("EBOOK_APP")
    private List<ItemDeleteAcc> itemDeleteAcc;

    public String getStatus_code() {
        return status_code;
    }

    public void setStatus_code(String status_code) {
        this.status_code = status_code;
    }

    public List<ItemDeleteAcc> getItemDeleteAcc() {
        return itemDeleteAcc;
    }

    public void setItemDeleteAcc(List<ItemDeleteAcc> itemDeleteAcc) {
        this.itemDeleteAcc = itemDeleteAcc;
    }


    public static class ItemDeleteAcc implements Serializable {

        @SerializedName("msg")
        String deleteMsg;

        @SerializedName("success")
        String deleteSuccess;


        public String getDeleteMsg() {
            return deleteMsg;
        }

        public void setDeleteMsg(String deleteMsg) {
            this.deleteMsg = deleteMsg;
        }

        public String getDeleteSuccess() {
            return deleteSuccess;
        }

        public void setDeleteSuccess(String deleteSuccess) {
            this.deleteSuccess = deleteSuccess;
        }
    }
}

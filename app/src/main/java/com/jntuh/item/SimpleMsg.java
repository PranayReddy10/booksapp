package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * EBOOK_APP[0] payload of result_save: a lightweight status object.
 * `locked` is present only in the "verified, cannot edit" failure case.
 */
public class SimpleMsg implements Serializable {

    @SerializedName("result_id")
    private String result_id;

    @SerializedName("msg")
    private String msg;

    @SerializedName("success")
    private String success;

    @SerializedName("locked")
    private int locked;

    public String getResult_id() {
        return result_id;
    }

    public void setResult_id(String result_id) {
        this.result_id = result_id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public int getLocked() {
        return locked;
    }

    public void setLocked(int locked) {
        this.locked = locked;
    }
}

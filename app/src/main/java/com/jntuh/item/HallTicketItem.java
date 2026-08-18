package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * EBOOK_APP[0] of hall_ticket_lookup: who a hall ticket belongs to, used to
 * fill in the sign-up form. `state` is "ready", "queued" or "error" — queued
 * means the university feed is still fetching, so ask again shortly.
 */
public class HallTicketItem implements Serializable {

    @SerializedName("state")
    private String state;

    @SerializedName("hall_ticket_no")
    private String hall_ticket_no;

    @SerializedName("student_name")
    private String student_name;

    @SerializedName("father_name")
    private String father_name;

    @SerializedName("college_code")
    private String college_code;

    @SerializedName("branch")
    private String branch;

    @SerializedName("regulation")
    private String regulation;

    @SerializedName("semester_count")
    private int semester_count;

    @SerializedName("already_registered")
    private int already_registered;

    @SerializedName("msg")
    private String msg;

    @SerializedName("success")
    private String success;

    public String getState() {
        return state;
    }

    public String getHall_ticket_no() {
        return hall_ticket_no;
    }

    public String getStudent_name() {
        return student_name;
    }

    public String getFather_name() {
        return father_name;
    }

    public String getCollege_code() {
        return college_code;
    }

    public String getBranch() {
        return branch;
    }

    public String getRegulation() {
        return regulation;
    }

    public int getSemester_count() {
        return semester_count;
    }

    public int getAlready_registered() {
        return already_registered;
    }

    public String getMsg() {
        return msg;
    }

    public String getSuccess() {
        return success;
    }
}

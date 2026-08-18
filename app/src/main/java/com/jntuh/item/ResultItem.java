package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The EBOOK_APP[0] payload of result_get: the student's own result header
 * plus its list of semesters. `success`, `has_result`, `verified` and
 * `locked` gate the UI.
 */
public class ResultItem implements Serializable {

    @SerializedName("success")
    private String success;

    @SerializedName("has_result")
    private int has_result;

    @SerializedName("result_id")
    private String result_id;

    @SerializedName("hall_ticket_no")
    private String hall_ticket_no;

    /** "jntuh" when fetched from the university feed, "manual" when typed. */
    @SerializedName("source")
    private String source;

    @SerializedName("student_name")
    private String student_name;

    @SerializedName("regulation")
    private String regulation;

    @SerializedName("degree")
    private String degree;

    @SerializedName("branch")
    private String branch;

    @SerializedName("current_cgpa")
    private String current_cgpa;

    @SerializedName("total_credits")
    private String total_credits;

    @SerializedName("backlogs_count")
    private String backlogs_count;

    @SerializedName("verified")
    private int verified;

    @SerializedName("locked")
    private int locked;

    @SerializedName("share_url")
    private String share_url;

    @SerializedName("report_image_url")
    private String report_image_url;

    @SerializedName("semesters")
    private List<SemesterItem> semesters = new ArrayList<>();

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public int getHas_result() {
        return has_result;
    }

    public void setHas_result(int has_result) {
        this.has_result = has_result;
    }

    public String getResult_id() {
        return result_id;
    }

    public void setResult_id(String result_id) {
        this.result_id = result_id;
    }

    public String getSource() {
        return source;
    }

    public String getHall_ticket_no() {
        return hall_ticket_no;
    }

    public void setHall_ticket_no(String hall_ticket_no) {
        this.hall_ticket_no = hall_ticket_no;
    }

    public String getStudent_name() {
        return student_name;
    }

    public void setStudent_name(String student_name) {
        this.student_name = student_name;
    }

    public String getRegulation() {
        return regulation;
    }

    public void setRegulation(String regulation) {
        this.regulation = regulation;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCurrent_cgpa() {
        return current_cgpa;
    }

    public void setCurrent_cgpa(String current_cgpa) {
        this.current_cgpa = current_cgpa;
    }

    public String getTotal_credits() {
        return total_credits;
    }

    public void setTotal_credits(String total_credits) {
        this.total_credits = total_credits;
    }

    public String getBacklogs_count() {
        return backlogs_count;
    }

    public void setBacklogs_count(String backlogs_count) {
        this.backlogs_count = backlogs_count;
    }

    public int getVerified() {
        return verified;
    }

    public void setVerified(int verified) {
        this.verified = verified;
    }

    public int getLocked() {
        return locked;
    }

    public void setLocked(int locked) {
        this.locked = locked;
    }

    public String getShare_url() {
        return share_url;
    }

    public void setShare_url(String share_url) {
        this.share_url = share_url;
    }

    public String getReport_image_url() {
        return report_image_url;
    }

    public void setReport_image_url(String report_image_url) {
        this.report_image_url = report_image_url;
    }

    public List<SemesterItem> getSemesters() {
        if (semesters == null) semesters = new ArrayList<>();
        return semesters;
    }

    public void setSemesters(List<SemesterItem> semesters) {
        this.semesters = semesters;
    }
}

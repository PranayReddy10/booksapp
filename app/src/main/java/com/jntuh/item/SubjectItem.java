package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * One subject row inside a semester of a student's result.
 * Mirrors the objects inside result_get -> semesters[].subjects[].
 * Used both for rendering (result_get) and for building the save payload
 * (result_save -> semesters_json) via Gson.
 */
public class SubjectItem implements Serializable {

    @SerializedName("subject_code")
    private String subject_code;

    @SerializedName("subject_name")
    private String subject_name;

    // Doubles so null (absent) vs 0 stay distinguishable and Gson omits nulls.
    @SerializedName("credits")
    private Double credits;

    @SerializedName("grade")
    private String grade;

    @SerializedName("grade_points")
    private Double grade_points;

    @SerializedName("is_backlog")
    private int is_backlog;

    @SerializedName("internal")
    private Integer internal;

    @SerializedName("external")
    private Integer external;

    @SerializedName("total")
    private Integer total;

    public SubjectItem() {
    }

    public String getSubject_code() {
        return subject_code;
    }

    public void setSubject_code(String subject_code) {
        this.subject_code = subject_code;
    }

    public String getSubject_name() {
        return subject_name;
    }

    public void setSubject_name(String subject_name) {
        this.subject_name = subject_name;
    }

    public Double getCredits() {
        return credits;
    }

    public void setCredits(Double credits) {
        this.credits = credits;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Double getGrade_points() {
        return grade_points;
    }

    public void setGrade_points(Double grade_points) {
        this.grade_points = grade_points;
    }

    public int getIs_backlog() {
        return is_backlog;
    }

    public void setIs_backlog(int is_backlog) {
        this.is_backlog = is_backlog;
    }

    public Integer getInternal() {
        return internal;
    }

    public void setInternal(Integer internal) {
        this.internal = internal;
    }

    public Integer getExternal() {
        return external;
    }

    public void setExternal(Integer external) {
        this.external = external;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}

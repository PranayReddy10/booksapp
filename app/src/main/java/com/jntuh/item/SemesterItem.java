package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One semester of a student's result. Mirrors result_get -> semesters[].
 * Also reused (with Gson) to build the semesters_json string for result_save.
 */
public class SemesterItem implements Serializable {

    @SerializedName("sem_code")
    private String sem_code;

    @SerializedName("sem_id")
    private String sem_id;

    @SerializedName("verified")
    private int verified;

    @SerializedName("locked")
    private int locked;

    @SerializedName("sgpa")
    private Double sgpa;

    @SerializedName("credits_earned")
    private Double credits_earned;

    @SerializedName("exam_month_year")
    private String exam_month_year;

    @SerializedName("subjects")
    private List<SubjectItem> subjects = new ArrayList<>();

    public SemesterItem() {
    }

    public String getSem_code() {
        return sem_code;
    }

    public void setSem_code(String sem_code) {
        this.sem_code = sem_code;
    }

    public String getSem_id() {
        return sem_id;
    }

    public void setSem_id(String sem_id) {
        this.sem_id = sem_id;
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

    public Double getSgpa() {
        return sgpa;
    }

    public void setSgpa(Double sgpa) {
        this.sgpa = sgpa;
    }

    public Double getCredits_earned() {
        return credits_earned;
    }

    public void setCredits_earned(Double credits_earned) {
        this.credits_earned = credits_earned;
    }

    public String getExam_month_year() {
        return exam_month_year;
    }

    public void setExam_month_year(String exam_month_year) {
        this.exam_month_year = exam_month_year;
    }

    public List<SubjectItem> getSubjects() {
        if (subjects == null) subjects = new ArrayList<>();
        return subjects;
    }

    public void setSubjects(List<SubjectItem> subjects) {
        this.subjects = subjects;
    }
}

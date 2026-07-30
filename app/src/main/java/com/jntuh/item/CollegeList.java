package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CollegeList implements Serializable {

    @SerializedName("college_id")
    private String college_id;

    @SerializedName("college_name")
    private String college_name;

    public String getCollege_id() {
        return college_id;
    }

    public void setCollege_id(String college_id) {
        this.college_id = college_id;
    }

    public String getCollege_name() {
        return college_name;
    }

    public void setCollege_name(String college_name) {
        this.college_name = college_name;
    }

    @Override
    public String toString() {
        return college_name;
    }
}

package com.jntuh.item;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DepartmentList implements Serializable {

    @SerializedName("department_id")
    private String department_id;

    @SerializedName("department_name")
    private String department_name;

    @SerializedName("university_id")
    private String university_id;

    public String getDepartment_id() {
        return department_id;
    }

    public void setDepartment_id(String department_id) {
        this.department_id = department_id;
    }

    public String getDepartment_name() {
        return department_name;
    }

    public void setDepartment_name(String department_name) {
        this.department_name = department_name;
    }

    public String getUniversity_id() {
        return university_id;
    }

    public void setUniversity_id(String university_id) {
        this.university_id = university_id;
    }

    @Override
    public String toString() {
        return department_name;
    }
}

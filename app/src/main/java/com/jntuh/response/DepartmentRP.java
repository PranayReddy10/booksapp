package com.jntuh.response;

import com.jntuh.item.DepartmentList;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class DepartmentRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<DepartmentList> departmentLists;

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

    public List<DepartmentList> getDepartmentLists() {
        return departmentLists;
    }

    public void setDepartmentLists(List<DepartmentList> departmentLists) {
        this.departmentLists = departmentLists;
    }
}

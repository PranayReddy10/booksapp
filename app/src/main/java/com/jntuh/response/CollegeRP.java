package com.jntuh.response;

import com.jntuh.item.CollegeList;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class CollegeRP implements Serializable {

    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<CollegeList> collegeLists;

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

    public List<CollegeList> getCollegeLists() {
        return collegeLists;
    }

    public void setCollegeLists(List<CollegeList> collegeLists) {
        this.collegeLists = collegeLists;
    }
}

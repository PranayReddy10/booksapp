package com.jntuh.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class LoginRP implements Serializable {


    @SerializedName("status_code")
    private String status_code;

    @SerializedName("success")
    private String success;

    @SerializedName("EBOOK_APP")
    private List<ItemUser> itemUserList;

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

    public List<ItemUser> getItemUserList() {
        return itemUserList;
    }

    public void setItemUserList(List<ItemUser> itemUserList) {
        this.itemUserList = itemUserList;
    }



    public static class ItemUser implements Serializable {

        @SerializedName("user_id")
        String user_id;

        @SerializedName("name")
        String name;

        @SerializedName("email")
        String email;

        @SerializedName("phone")
        String phone;

        @SerializedName("username")
        String username;

        @SerializedName("profile_complete")
        String profile_complete;

        @SerializedName("user_image")
        String user_image;

        @SerializedName("university")
        String university;

        @SerializedName("department")
        String department;

        @SerializedName("department_id")
        String department_id;

        @SerializedName("college")
        String college;

        @SerializedName("gender")
        String gender;

        @SerializedName("year")
        String year;

        @SerializedName("rollnumber")
        String rollnumber;

        @SerializedName("msg")
        String msg;

        @SerializedName("success")
        String success;

        public String getUser_id() {
            return user_id;
        }

        public void setUser_id(String user_id) {
            this.user_id = user_id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getUsername() {
            return username;
        }

        public String getProfile_complete() {
            return profile_complete;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUser_image() {
            return user_image;
        }

        public void setUser_image(String user_image) {
            this.user_image = user_image;
        }

        public String getUniversity() {
            return university;
        }

        public String getDepartment() {
            return department;
        }

        public String getDepartment_id() {
            return department_id;
        }

        public String getCollege() {
            return college;
        }

        public String getGender() {
            return gender;
        }

        public String getYear() {
            return year;
        }

        public String getRollnumber() {
            return rollnumber;
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


    }
    }

package com.jntuh.books;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jntuh.books.databinding.ActivityRegisterBinding;
import com.jntuh.item.CollegeList;
import com.jntuh.item.DepartmentList;
import com.jntuh.item.UniversityList;
import com.jntuh.response.CollegeRP;
import com.jntuh.response.DepartmentRP;
import com.jntuh.response.RegisterRP;
import com.jntuh.response.UniversityRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Constant;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    ProgressDialog progressDialog;
    ActivityRegisterBinding viewRegisterBinding;
    Method method;

    private final List<UniversityList> universityList = new ArrayList<>();
    private final List<DepartmentList> departmentListAll = new ArrayList<>();
    private final List<DepartmentList> departmentListFiltered = new ArrayList<>();
    private final List<CollegeList> collegeList = new ArrayList<>();

    private ArrayAdapter<String> universityAdapter, departmentAdapter, collegeAdapter, genderAdapter;
    private final List<String> universityNames = new ArrayList<>();
    private final List<String> departmentNames = new ArrayList<>();
    private final List<String> collegeNames = new ArrayList<>();

    private final String[] genders = {"", "Male", "Female", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewRegisterBinding = ActivityRegisterBinding.inflate(getLayoutInflater());
        View view = viewRegisterBinding.getRoot();
        setContentView(view);

        method = new Method(this);
        method.forceRTLIfSupported();
        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);

        setupSpinners();

        viewRegisterBinding.tvRegPrivacyTerms.setOnClickListener(v -> {
            Intent intentPage = new Intent(RegisterActivity.this, PagesActivity.class);
            intentPage.putExtra("PAGE_TITLE", Constant.appListData.getPageList().get(1).getPageTitle());
            intentPage.putExtra("PAGE_DESC", Constant.appListData.getPageList().get(1).getPageContent());
            startActivity(intentPage);
        });

        viewRegisterBinding.cbRegPrivacyTerms.setOnCheckedChangeListener((checkBox, isChecked) -> {
        });

        viewRegisterBinding.btnRegister.setOnClickListener(v -> form());

        viewRegisterBinding.tvRegLogIn.setOnClickListener(v -> {
            Intent intent_login = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent_login);
        });

        if (method.isNetworkAvailable()) {
            loadUniversities();
            loadDepartments(null);
            loadColleges();
        } else {
            method.alertBox(getResources().getString(R.string.internet_connection));
        }
    }

    private void setupSpinners() {
        // University
        universityNames.add(getString(R.string.lbl_select_university));
        universityAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, universityNames);
        universityAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        viewRegisterBinding.spRegUniversity.setAdapter(universityAdapter);

        // Department
        departmentNames.add(getString(R.string.lbl_select_department));
        departmentAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, departmentNames);
        departmentAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        viewRegisterBinding.spRegDepartment.setAdapter(departmentAdapter);

        // College
        collegeNames.add(getString(R.string.lbl_select_college));
        collegeAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, collegeNames);
        collegeAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        viewRegisterBinding.spRegCollege.setAdapter(collegeAdapter);

        // Gender (static)
        List<String> genderLabels = new ArrayList<>();
        genderLabels.add(getString(R.string.lbl_select_gender));
        genderLabels.add(getString(R.string.lbl_gender_male));
        genderLabels.add(getString(R.string.lbl_gender_female));
        genderLabels.add(getString(R.string.lbl_gender_other));
        genderAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, genderLabels);
        genderAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        viewRegisterBinding.spRegGender.setAdapter(genderAdapter);

        // Cascade: when a university is chosen, refresh departments for it
        viewRegisterBinding.spRegUniversity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String universityId = null;
                if (position > 0 && (position - 1) < universityList.size()) {
                    universityId = universityList.get(position - 1).getUniversity_id();
                }
                filterDepartmentsByUniversity(universityId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadUniversities() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(RegisterActivity.this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<UniversityRP> call = apiService.getUniversityListData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<UniversityRP>() {
            @Override
            public void onResponse(@NotNull Call<UniversityRP> call, @NotNull Response<UniversityRP> response) {
                try {
                    UniversityRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess()) && body.getUniversityLists() != null) {
                        universityList.clear();
                        universityList.addAll(body.getUniversityLists());
                        universityNames.clear();
                        universityNames.add(getString(R.string.lbl_select_university));
                        for (UniversityList u : universityList) universityNames.add(u.getUniversity_name());
                        universityAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<UniversityRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
            }
        });
    }

    // universityId null = load all departments (used as initial cache); cascade filters locally.
    private void loadDepartments(String universityId) {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(RegisterActivity.this));
        if (universityId != null) jsObj.addProperty("university_id", universityId);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<DepartmentRP> call = apiService.getDepartmentListData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<DepartmentRP>() {
            @Override
            public void onResponse(@NotNull Call<DepartmentRP> call, @NotNull Response<DepartmentRP> response) {
                try {
                    DepartmentRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess()) && body.getDepartmentLists() != null) {
                        departmentListAll.clear();
                        departmentListAll.addAll(body.getDepartmentLists());
                        // refresh filtered view with current university selection
                        int uPos = viewRegisterBinding.spRegUniversity.getSelectedItemPosition();
                        String uId = (uPos > 0 && (uPos - 1) < universityList.size())
                                ? universityList.get(uPos - 1).getUniversity_id() : null;
                        filterDepartmentsByUniversity(uId);
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<DepartmentRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
            }
        });
    }

    private void filterDepartmentsByUniversity(String universityId) {
        departmentListFiltered.clear();
        for (DepartmentList d : departmentListAll) {
            if (universityId == null || universityId.equals(d.getUniversity_id())) {
                departmentListFiltered.add(d);
            }
        }
        departmentNames.clear();
        departmentNames.add(getString(R.string.lbl_select_department));
        for (DepartmentList d : departmentListFiltered) departmentNames.add(d.getDepartment_name());
        departmentAdapter.notifyDataSetChanged();
        viewRegisterBinding.spRegDepartment.setSelection(0);
    }

    private void loadColleges() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(RegisterActivity.this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<CollegeRP> call = apiService.getCollegeListData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<CollegeRP>() {
            @Override
            public void onResponse(@NotNull Call<CollegeRP> call, @NotNull Response<CollegeRP> response) {
                try {
                    CollegeRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess()) && body.getCollegeLists() != null) {
                        collegeList.clear();
                        collegeList.addAll(body.getCollegeLists());
                        collegeNames.clear();
                        collegeNames.add(getString(R.string.lbl_select_college));
                        for (CollegeList c : collegeList) collegeNames.add(c.getCollege_name());
                        collegeAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<CollegeRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
            }
        });
    }

    private boolean isValidMail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void form() {

        String name = viewRegisterBinding.edtRegName.getText().toString();
        String email = viewRegisterBinding.edtRegEmail.getText().toString();
        String password = viewRegisterBinding.edtRegPass.getText().toString();
        String phoneNo = viewRegisterBinding.edtRegPhone.getText().toString();
        String rollNo = viewRegisterBinding.edtRegRoll.getText().toString();

        int uPos = viewRegisterBinding.spRegUniversity.getSelectedItemPosition();
        int dPos = viewRegisterBinding.spRegDepartment.getSelectedItemPosition();
        int cPos = viewRegisterBinding.spRegCollege.getSelectedItemPosition();
        int gPos = viewRegisterBinding.spRegGender.getSelectedItemPosition();

        viewRegisterBinding.edtRegName.setError(null);
        viewRegisterBinding.edtRegEmail.setError(null);
        viewRegisterBinding.edtRegPass.setError(null);
        viewRegisterBinding.edtRegPhone.setError(null);

        if (name.equals("") || name.isEmpty()) {
            viewRegisterBinding.edtRegName.requestFocus();
            viewRegisterBinding.edtRegName.setError(getResources().getString(R.string.please_enter_name));
        } else if (!isValidMail(email) || email.isEmpty()) {
            viewRegisterBinding.edtRegEmail.requestFocus();
            viewRegisterBinding.edtRegEmail.setError(getResources().getString(R.string.please_enter_email));
        } else if (password.equals("") || password.isEmpty()) {
            viewRegisterBinding.edtRegPass.requestFocus();
            viewRegisterBinding.edtRegPass.setError(getResources().getString(R.string.please_enter_password));
        } else if (uPos <= 0) {
            Toast.makeText(this, getString(R.string.please_select_university), Toast.LENGTH_SHORT).show();
        } else if (dPos <= 0) {
            Toast.makeText(this, getString(R.string.please_select_department), Toast.LENGTH_SHORT).show();
        } else if (cPos <= 0) {
            Toast.makeText(this, getString(R.string.please_select_college), Toast.LENGTH_SHORT).show();
        } else if (gPos <= 0) {
            Toast.makeText(this, getString(R.string.please_select_gender), Toast.LENGTH_SHORT).show();
        } else {
            viewRegisterBinding.edtRegName.clearFocus();
            viewRegisterBinding.edtRegEmail.clearFocus();
            viewRegisterBinding.edtRegPass.clearFocus();

            String universityName = universityList.get(uPos - 1).getUniversity_name();
            String departmentId = departmentListFiltered.get(dPos - 1).getDepartment_id();
            String collegeName = collegeList.get(cPos - 1).getCollege_name();
            String gender = genders[gPos];

            if (viewRegisterBinding.cbRegPrivacyTerms.isChecked()) {
                if (method.isNetworkAvailable()) {
                    register(name, email, password, phoneNo, universityName, departmentId, collegeName, gender, rollNo);
                } else {
                    method.alertBox(getResources().getString(R.string.internet_connection));
                }
            } else {
                Toast.makeText(RegisterActivity.this, getString(R.string.please_accept), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void register(String sendName, String sendEmail, String sendPassword, String sendPhone,
                         String sendUniversity, String sendDepartmentId, String sendCollege,
                         String sendGender, String sendRoll) {

        progressDialog.show();
        progressDialog.setMessage(getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(RegisterActivity.this));
        jsObj.addProperty("name", sendName);
        jsObj.addProperty("email", sendEmail);
        jsObj.addProperty("password", sendPassword);
        jsObj.addProperty("phone", sendPhone);
        jsObj.addProperty("university", sendUniversity);
        jsObj.addProperty("department_id", sendDepartmentId);
        jsObj.addProperty("college", sendCollege);
        jsObj.addProperty("gender", sendGender);
        jsObj.addProperty("rollnumber", sendRoll);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<RegisterRP> call = apiService.getRegisterData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<RegisterRP>() {
            @Override
            public void onResponse(@NotNull Call<RegisterRP> call, @NotNull Response<RegisterRP> response) {

                try {
                    RegisterRP registerRP = response.body();

                    if (registerRP != null && registerRP.getSuccess().equals("1")) {
                        if (registerRP.getItemUserListRegister().get(0).getSuccess().equals("1")) {
                            method.savePhone(sendPhone);
                            Toast.makeText(RegisterActivity.this, registerRP.getItemUserListRegister().get(0).getMsg(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finishAffinity();
                        } else {
                            method.alertBox(registerRP.getItemUserListRegister().get(0).getMsg());
                        }
                    } else {
                        method.alertBox(getResources().getString(R.string.failed_try_again));
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }

                progressDialog.dismiss();
            }

            @Override
            public void onFailure(@NotNull Call<RegisterRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }
}

package com.jntuh.books;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.books.databinding.ActivityCompleteProfileBinding;
import com.jntuh.item.CollegeList;
import com.jntuh.item.DepartmentList;
import com.jntuh.item.UniversityList;
import com.jntuh.response.CollegeRP;
import com.jntuh.response.DepartmentRP;
import com.jntuh.response.EditProfileRP;
import com.jntuh.response.UniversityRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shown once, right after a Google user signs in without University/Department/College.
 * They must pick all three before entering the app.
 */
public class CompleteProfileActivity extends AppCompatActivity {

    private ActivityCompleteProfileBinding binding;
    private Method method;
    private String userId;

    private final List<UniversityList> universityList = new ArrayList<>();
    private final List<DepartmentList> departmentAll = new ArrayList<>();
    private final List<DepartmentList> departmentFiltered = new ArrayList<>();
    private final List<CollegeList> collegeList = new ArrayList<>();

    private final List<String> universityNames = new ArrayList<>();
    private final List<String> departmentNames = new ArrayList<>();
    private final List<String> collegeNames = new ArrayList<>();

    private ArrayAdapter<String> universityAdapter, departmentAdapter, collegeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompleteProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        userId = getIntent().getStringExtra("uId");

        setupSpinners();

        binding.btnSaveProfile.setOnClickListener(v -> save());

        if (method.isNetworkAvailable()) {
            loadUniversities();
            loadDepartments();
            loadColleges();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }
    }

    private void setupSpinners() {
        universityNames.add(getString(R.string.lbl_select_university));
        universityAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item_light, universityNames);
        universityAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        binding.spUniversity.setAdapter(universityAdapter);

        departmentNames.add(getString(R.string.lbl_select_department));
        departmentAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item_light, departmentNames);
        departmentAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        binding.spDepartment.setAdapter(departmentAdapter);

        collegeNames.add(getString(R.string.lbl_select_college));
        collegeAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item_light, collegeNames);
        collegeAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        binding.spCollege.setAdapter(collegeAdapter);

        // Long lists → searchable dialog on tap (same as registration/upload).
        com.jntuh.util.SearchableSpinner.attach(binding.spUniversity, getString(R.string.lbl_select_university));
        com.jntuh.util.SearchableSpinner.attach(binding.spDepartment, getString(R.string.lbl_select_department));
        com.jntuh.util.SearchableSpinner.attach(binding.spCollege, getString(R.string.lbl_select_college));

        binding.spUniversity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String universityId = null;
                if (position > 0 && (position - 1) < universityList.size()) {
                    universityId = universityList.get(position - 1).getUniversity_id();
                }
                filterDepartments(universityId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadUniversities() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getUniversityListData(API.toBase64(jsObj.toString())).enqueue(new Callback<UniversityRP>() {
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

    private void loadDepartments() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getDepartmentListData(API.toBase64(jsObj.toString())).enqueue(new Callback<DepartmentRP>() {
            @Override
            public void onResponse(@NotNull Call<DepartmentRP> call, @NotNull Response<DepartmentRP> response) {
                try {
                    DepartmentRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess()) && body.getDepartmentLists() != null) {
                        departmentAll.clear();
                        departmentAll.addAll(body.getDepartmentLists());
                        filterDepartments(null);
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

    private void filterDepartments(String universityId) {
        departmentFiltered.clear();
        for (DepartmentList d : departmentAll) {
            if (universityId == null || universityId.equals(d.getUniversity_id())) {
                departmentFiltered.add(d);
            }
        }
        departmentNames.clear();
        departmentNames.add(getString(R.string.lbl_select_department));
        for (DepartmentList d : departmentFiltered) departmentNames.add(d.getDepartment_name());
        departmentAdapter.notifyDataSetChanged();
        binding.spDepartment.setSelection(0);
    }

    private void loadColleges() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getCollegeListData(API.toBase64(jsObj.toString())).enqueue(new Callback<CollegeRP>() {
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

    private void save() {
        int uPos = binding.spUniversity.getSelectedItemPosition();
        int dPos = binding.spDepartment.getSelectedItemPosition();
        int cPos = binding.spCollege.getSelectedItemPosition();

        if (uPos <= 0 || dPos <= 0 || cPos <= 0) {
            method.alertBox(getString(R.string.lbl_complete_all_fields));
            return;
        }

        String university = universityList.get(uPos - 1).getUniversity_name();
        String departmentId = departmentFiltered.get(dPos - 1).getDepartment_id();
        String college = collegeList.get(cPos - 1).getCollege_name();

        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }

        binding.progressComplete.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", userId);
        jsObj.addProperty("name", method.getUserName());
        jsObj.addProperty("email", method.getUserEmail());
        jsObj.addProperty("phone", "");
        jsObj.addProperty("password", "");
        jsObj.addProperty("university", university);
        jsObj.addProperty("department_id", departmentId);
        jsObj.addProperty("college", college);

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getCompleteProfileData(API.toBase64(jsObj.toString())).enqueue(new Callback<EditProfileRP>() {
            @Override
            public void onResponse(@NotNull Call<EditProfileRP> call, @NotNull Response<EditProfileRP> response) {
                binding.progressComplete.setVisibility(View.GONE);
                try {
                    EditProfileRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess())) {
                        goToApp();
                    } else {
                        method.alertBox(getString(R.string.failed_try_again));
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getString(R.string.failed_try_again));
                }
            }

            @Override
            public void onFailure(@NotNull Call<EditProfileRP> call, @NotNull Throwable t) {
                binding.progressComplete.setVisibility(View.GONE);
                Log.e("fail", t.toString());
                method.alertBox(getString(R.string.failed_try_again));
            }
        });
    }

    private void goToApp() {
        method.saveProfileComplete("1");
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Force completion — can't skip by pressing back.
        method.alertBox(getString(R.string.lbl_complete_all_fields));
    }
}

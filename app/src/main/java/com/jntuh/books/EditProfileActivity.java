package com.jntuh.books;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.jntuh.books.databinding.ActivityEditProfileBinding;
import com.jntuh.fragment.ProfileImage;
import com.jntuh.response.EditProfileRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Events;
import com.jntuh.util.GlobalBus;
import com.jntuh.util.Method;
import com.jntuh.item.UniversityList;
import com.jntuh.item.DepartmentList;
import com.jntuh.item.CollegeList;
import com.jntuh.response.UniversityRP;
import com.jntuh.response.DepartmentRP;
import com.jntuh.response.CollegeRP;
import java.util.ArrayList;
import java.util.List;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class EditProfileActivity extends AppCompatActivity {

    ActivityEditProfileBinding viewEditProfile;
    Method method;
    String uId, uName, uEmail, uImage, uPhone, uType, uUsername;
    String uUniversity, uDepartment, uCollege, uGender, uYear, uRoll;
    String uBranch, uRegulation, uDegree;

    // University -> Department (= Branch) -> College cascade (reused from CompleteProfile).
    private final List<UniversityList> universityList = new ArrayList<>();
    private final List<DepartmentList> departmentAll = new ArrayList<>();
    private final List<DepartmentList> departmentFiltered = new ArrayList<>();
    private final List<CollegeList> collegeList = new ArrayList<>();
    private final List<String> universityNames = new ArrayList<>();
    private final List<String> departmentNames = new ArrayList<>();
    private final List<String> collegeNames = new ArrayList<>();
    private ArrayAdapter<String> universityAdapter, departmentAdapter, collegeAdapter;
    private boolean universitiesLoaded = false, departmentsLoaded = false;
    private boolean suppressUniListener = false;
    private String savedDepartment = null; // captured once; never nulled by the listener
    boolean isProfile = false;
    ProgressDialog progressDialog;
    String imageProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewEditProfile = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(viewEditProfile.getRoot());

        method = new Method(EditProfileActivity.this);
        method.forceRTLIfSupported();

        GlobalBus.getBus().register(this);
        progressDialog = new ProgressDialog(EditProfileActivity.this,R.style.MyAlertDialogStyle);

        Intent intent = getIntent();
        uId = intent.getStringExtra("uId");
        uName = intent.getStringExtra("uName");
        uEmail = intent.getStringExtra("uEmail");
        uUsername = intent.getStringExtra("uUsername");
        uImage = intent.getStringExtra("uImage");
        uPhone = intent.getStringExtra("uPhone");
        uType = intent.getStringExtra("uType");
        uUniversity = intent.getStringExtra("uUniversity");
        uDepartment = intent.getStringExtra("uDepartment");
        savedDepartment = uDepartment; // stable copy for prefill
        uCollege = intent.getStringExtra("uCollege");
        uGender = intent.getStringExtra("uGender");
        uYear = intent.getStringExtra("uYear");
        uRoll = intent.getStringExtra("uRoll");
        uBranch = intent.getStringExtra("uBranch");
        uRegulation = intent.getStringExtra("uRegulation");
        uDegree = intent.getStringExtra("uDegree");

        // Callers that already hold the profile pass it in as extras. Callers that
        // don't (the profile shortcut, "Complete profile" on results) get an empty
        // screen unless we fetch it ourselves, so do that before populating.
        if (uId == null || uId.trim().isEmpty()) {
            fetchProfileThenPopulate();
        } else {
            populateProfile();
        }
    }

    /** Pull the signed-in user's profile, then fill the form with it. */
    private void fetchProfileThenPopulate() {
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            populateProfile();
            return;
        }

        com.google.gson.JsonObject jsObj =
                (com.google.gson.JsonObject) new com.google.gson.Gson().toJsonTree(new com.jntuh.util.API(this));
        jsObj.addProperty("user_id", method.getUserId());
        com.jntuh.rest.ApiClient.getClient().create(com.jntuh.rest.ApiInterface.class)
                .getProfileData(com.jntuh.util.API.toBase64(jsObj.toString()))
                .enqueue(new retrofit2.Callback<com.jntuh.response.LoginRP>() {
                    @Override
                    public void onResponse(@NotNull retrofit2.Call<com.jntuh.response.LoginRP> call,
                                           @NotNull retrofit2.Response<com.jntuh.response.LoginRP> response) {
                        try {
                            com.jntuh.response.LoginRP body = response.body();
                            if (body != null && "1".equals(body.getSuccess())
                                    && body.getItemUserList() != null && !body.getItemUserList().isEmpty()) {
                                com.jntuh.response.LoginRP.ItemUser user = body.getItemUserList().get(0);
                                uId = user.getUser_id();
                                uName = user.getName();
                                uEmail = user.getEmail();
                                uUsername = user.getUsername();
                                uImage = user.getUser_image();
                                uPhone = user.getPhone();
                                uType = method.getUserType();
                                uUniversity = user.getUniversity();
                                uDepartment = user.getDepartment();
                                savedDepartment = uDepartment; // stable copy for prefill
                                uCollege = user.getCollege();
                                uGender = user.getGender();
                                uYear = user.getYear();
                                uRoll = user.getRollnumber();
                                uBranch = user.getBranch();
                                uRegulation = user.getRegulation();
                                uDegree = user.getDegree();
                            }
                        } catch (Exception e) {
                            Log.d("exception_error", e.toString());
                        }
                        populateProfile();
                    }

                    @Override
                    public void onFailure(@NotNull retrofit2.Call<com.jntuh.response.LoginRP> call,
                                          @NotNull Throwable t) {
                        Log.e("fail", t.toString());
                        populateProfile();
                    }
                });
    }

    /** Fill every field from whatever profile data we ended up with. */
    private void populateProfile() {
        viewEditProfile.toolbarMain.tvToolbarTitle.setText(getString(R.string.profile_edt_title));
        viewEditProfile.toolbarMain.ivSearch.setVisibility(View.GONE);
        viewEditProfile.toolbarMain.imageFilter.setVisibility(View.GONE);
        viewEditProfile.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        Glide.with(EditProfileActivity.this.getApplicationContext()).load(uImage)
                .placeholder(R.drawable.user_profile)
                .into(viewEditProfile.ivUser);
        viewEditProfile.tvProfileName.setText(uName);
        viewEditProfile.tvProfileEmail.setText(uEmail);
        viewEditProfile.edtName.setText(uName);
        viewEditProfile.edtEmail.setText(uEmail);
        viewEditProfile.edtUsername.setText(
                (uUsername != null && !uUsername.isEmpty()) ? "@" + uUsername : "");
        viewEditProfile.edtPhone.setText(uPhone);

        // University / Department (= Branch) / College become editable dropdowns.
        viewEditProfile.llUniversityRow.setVisibility(View.VISIBLE);
        viewEditProfile.llDepartmentRow.setVisibility(View.VISIBLE);
        viewEditProfile.llCollegeRow.setVisibility(View.VISIBLE);
        // Year + gender stay as-is (read-only rows, shown only if present).
        bindDetailRow(viewEditProfile.llYearRow, viewEditProfile.tvYearValue, uYear);
        bindDetailRow(viewEditProfile.llGenderRow, viewEditProfile.tvGenderValue, uGender);

        setupCascadeSpinners();

        // Editable extras. The hall ticket is set once: results are keyed on it,
        // so changing it would orphan this student's marks. Still enterable while
        // the account has none, for profiles that predate it being collected.
        if (uRoll != null) viewEditProfile.edtRoll.setText(uRoll);
        boolean rollLocked = uRoll != null && !uRoll.trim().isEmpty();
        viewEditProfile.edtRoll.setEnabled(!rollLocked);
        viewEditProfile.edtRoll.setFocusable(!rollLocked);
        viewEditProfile.edtRoll.setFocusableInTouchMode(!rollLocked);
        viewEditProfile.edtRoll.setTextColor(androidx.core.content.ContextCompat.getColor(
                this, rollLocked ? R.color.gray_trans_99 : R.color.gray));
        viewEditProfile.tvRollLockedNote.setVisibility(rollLocked ? View.VISIBLE : View.GONE);
        setupAcademicSpinner(viewEditProfile.spProfileRegulation, viewEditProfile.edtRegulationOther,
                R.array.result_regulation_entries, uRegulation);
        setupAcademicSpinner(viewEditProfile.spProfileDegree, viewEditProfile.edtDegreeOther,
                R.array.result_degree_entries, uDegree);

        viewEditProfile.btnSave.setOnClickListener(v -> save());

        viewEditProfile.ivEditIcon.setOnClickListener(v -> {
            BottomSheetDialogFragment fragment = new ProfileImage();
            fragment.show(getSupportFragmentManager(), "Bottom Sheet Dialog Fragment");
        });

    }

    @Subscribe
    public void getData(Events.ProImage proImage) {
        isProfile = proImage.isProfile();
        if (proImage.isProfile()) {
            imageProfile = proImage.getImagePath();
            Uri uri = Uri.fromFile(new File(imageProfile));
            Glide.with(getApplicationContext()).load(uri)
                    .placeholder(R.drawable.user_profile)
                    .into(viewEditProfile.ivUser);
        }

    }

    // Show a read-only detail row only when the profile API returned a value for it.
    private void bindDetailRow(android.widget.LinearLayout row, android.widget.TextView value, String text) {
        if (text != null && !text.trim().isEmpty()) {
            value.setText(text);
            row.setVisibility(View.VISIBLE);
        } else {
            row.setVisibility(View.GONE);
        }
    }

    // ---- University -> Department (=Branch) -> College cascade ----

    private void setupCascadeSpinners() {
        universityNames.add(getString(R.string.lbl_select_university));
        universityAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, universityNames);
        viewEditProfile.spEditUniversity.setAdapter(universityAdapter);

        departmentNames.add(getString(R.string.lbl_select_department));
        departmentAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, departmentNames);
        viewEditProfile.spEditDepartment.setAdapter(departmentAdapter);

        collegeNames.add(getString(R.string.lbl_select_college));
        collegeAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, collegeNames);
        viewEditProfile.spEditCollege.setAdapter(collegeAdapter);

        com.jntuh.util.SearchableSpinner.attach(viewEditProfile.spEditUniversity, getString(R.string.lbl_select_university));
        com.jntuh.util.SearchableSpinner.attach(viewEditProfile.spEditDepartment, getString(R.string.lbl_select_department));
        com.jntuh.util.SearchableSpinner.attach(viewEditProfile.spEditCollege, getString(R.string.lbl_select_college));

        viewEditProfile.spEditUniversity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String universityId = null;
                if (position > 0 && (position - 1) < universityList.size()) {
                    universityId = universityList.get(position - 1).getUniversity_id();
                }
                filterDepartments(universityId);
                // When the user (not prefill) changes university, clear the saved
                // department so it doesn't wrongly re-select under a new list.
                if (!suppressUniListener) {
                    uDepartment = null;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        if (method.isNetworkAvailable()) {
            loadUniversities();
            loadDepartments();
            loadColleges();
        }
    }

    private void loadUniversities() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getUniversityListData(API.toBase64(jsObj.toString())).enqueue(new Callback<UniversityRP>() {
            @Override public void onResponse(@NotNull Call<UniversityRP> call, @NotNull Response<UniversityRP> response) {
                UniversityRP body = response.body();
                if (body != null && "1".equals(body.getSuccess()) && body.getUniversityLists() != null) {
                    universityList.clear();
                    universityList.addAll(body.getUniversityLists());
                    universityNames.clear();
                    universityNames.add(getString(R.string.lbl_select_university));
                    for (UniversityList u : universityList) universityNames.add(u.getUniversity_name());
                    universityAdapter.notifyDataSetChanged();
                    universitiesLoaded = true;
                    prefillCascade();
                }
            }
            @Override public void onFailure(@NotNull Call<UniversityRP> call, @NotNull Throwable t) { }
        });
    }

    private void loadDepartments() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getDepartmentListData(API.toBase64(jsObj.toString())).enqueue(new Callback<DepartmentRP>() {
            @Override public void onResponse(@NotNull Call<DepartmentRP> call, @NotNull Response<DepartmentRP> response) {
                DepartmentRP body = response.body();
                if (body != null && "1".equals(body.getSuccess()) && body.getDepartmentLists() != null) {
                    departmentAll.clear();
                    departmentAll.addAll(body.getDepartmentLists());
                    departmentsLoaded = true;
                    prefillCascade();
                }
            }
            @Override public void onFailure(@NotNull Call<DepartmentRP> call, @NotNull Throwable t) { }
        });
    }

    private void loadColleges() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getCollegeListData(API.toBase64(jsObj.toString())).enqueue(new Callback<CollegeRP>() {
            @Override public void onResponse(@NotNull Call<CollegeRP> call, @NotNull Response<CollegeRP> response) {
                CollegeRP body = response.body();
                if (body != null && "1".equals(body.getSuccess()) && body.getCollegeLists() != null) {
                    collegeList.clear();
                    collegeList.addAll(body.getCollegeLists());
                    collegeNames.clear();
                    collegeNames.add(getString(R.string.lbl_select_college));
                    for (CollegeList c : collegeList) collegeNames.add(c.getCollege_name());
                    collegeAdapter.notifyDataSetChanged();
                    // Prefill college selection by name.
                    selectByName(viewEditProfile.spEditCollege, collegeNames, uCollege);
                }
            }
            @Override public void onFailure(@NotNull Call<CollegeRP> call, @NotNull Throwable t) { }
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
        // Re-apply the saved department selection within the (re)built list.
        selectByName(viewEditProfile.spEditDepartment, departmentNames, savedDepartment);
    }

    // Once both universities + departments are loaded, prefill the saved selections.
    private void prefillCascade() {
        // Suppress the university listener for the ENTIRE prefill so its async
        // firing can't null out the saved department mid-way.
        suppressUniListener = true;
        if (universitiesLoaded) {
            selectByName(viewEditProfile.spEditUniversity, universityNames, uUniversity);
        }
        if (universitiesLoaded && departmentsLoaded) {
            String uniId = null;
            int uPos = viewEditProfile.spEditUniversity.getSelectedItemPosition();
            if (uPos > 0 && (uPos - 1) < universityList.size()) {
                uniId = universityList.get(uPos - 1).getUniversity_id();
            }
            filterDepartments(uniId);
            selectByName(viewEditProfile.spEditDepartment, departmentNames, savedDepartment);
            // Fallback: saved department not in the filtered list -> show ALL and pick it.
            if (savedDepartment != null && !savedDepartment.trim().isEmpty()
                    && viewEditProfile.spEditDepartment.getSelectedItemPosition() == 0) {
                filterDepartments(null);
                selectByName(viewEditProfile.spEditDepartment, departmentNames, savedDepartment);
            }
        }
        // Release the guard after the current message loop so any queued
        // onItemSelected from the programmatic selection above runs while still
        // suppressed (won't clear the saved department).
        viewEditProfile.spEditUniversity.post(() -> suppressUniListener = false);
    }

    private void selectByName(Spinner sp, List<String> names, String value) {
        if (value == null || value.trim().isEmpty()) return;
        for (int i = 0; i < names.size(); i++) {
            if (value.trim().equalsIgnoreCase(names.get(i).trim())) {
                sp.setSelection(i);
                return;
            }
        }
    }

    // Populate a dropdown from a string-array; if the stored value isn't in the
    // list (or "Others" is chosen) reveal the free-text box. The last array item
    // is expected to be "Others".
    private void setupAcademicSpinner(Spinner spinner, EditText otherBox, int arrayRes, String current) {
        final java.util.List<String> items = new java.util.ArrayList<>();
        for (String s : getResources().getStringArray(arrayRes)) items.add(s);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, items);
        spinner.setAdapter(adapter);

        final String othersLabel = items.get(items.size() - 1); // "Others"
        int sel = -1;
        if (current != null && !current.trim().isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).equalsIgnoreCase(current.trim())) { sel = i; break; }
            }
            if (sel == -1) {
                // Value not in list -> select Others and prefill the box.
                sel = items.size() - 1;
                otherBox.setText(current.trim());
                otherBox.setVisibility(View.VISIBLE);
            }
        }
        if (sel >= 0) spinner.setSelection(sel);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                boolean isOthers = othersLabel.equalsIgnoreCase(items.get(pos));
                otherBox.setVisibility(isOthers ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    // Resolve the effective value: the free-text box when "Others" is selected,
    // otherwise the spinner selection. Empty string when nothing chosen.
    private String resolveSpinnerValue(Spinner spinner, EditText otherBox, int arrayRes) {
        Object sel = spinner.getSelectedItem();
        String value = sel == null ? "" : String.valueOf(sel);
        String[] arr = getResources().getStringArray(arrayRes);
        String othersLabel = arr.length > 0 ? arr[arr.length - 1] : "Others";
        if (othersLabel.equalsIgnoreCase(value)) {
            return otherBox.getText().toString().trim();
        }
        return value;
    }

    private void save() {

        String name = viewEditProfile.edtName.getText().toString();
        String phoneNo = viewEditProfile.edtPhone.getText().toString();

        viewEditProfile.edtName.setError(null);
        viewEditProfile.edtPhone.setError(null);

        if (name.equals("") || name.isEmpty()) {
            viewEditProfile.edtName.requestFocus();
            viewEditProfile.edtName.setError(getResources().getString(R.string.please_enter_name));
        } else if (phoneNo.equals("") || phoneNo.isEmpty()) {
            viewEditProfile.edtPhone.requestFocus();
            viewEditProfile.edtPhone.setError(getResources().getString(R.string.please_enter_phone));
        } else {
            if (method.isNetworkAvailable()) {

                viewEditProfile.edtName.clearFocus();
                viewEditProfile.edtPhone.clearFocus();
                String pass=viewEditProfile.edtPassword.getText().toString();
                if (pass.isEmpty()){
                    profileUpdate(method.getUserId(), name,uEmail,"", phoneNo, imageProfile);
                }else {
                    profileUpdate(method.getUserId(), name,uEmail,pass, phoneNo, imageProfile);
                }

            } else {
                method.alertBox(getResources().getString(R.string.internet_connection));
            }
        }

    }

    public void profileUpdate(String userId, String sendName,String sendEmail,String sendPass, String sendPhone, String profile_image) {


        progressDialog.show();
        progressDialog.setMessage(getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        MultipartBody.Part body = null;

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(EditProfileActivity.this));
        jsObj.addProperty("user_id", userId);
        jsObj.addProperty("name", sendName);
        jsObj.addProperty("email", sendEmail);
        jsObj.addProperty("password", sendPass);
        jsObj.addProperty("phone", sendPhone);
        // University -> Department (= Branch) -> College from the cascade.
        int uPos = viewEditProfile.spEditUniversity.getSelectedItemPosition();
        if (uPos > 0 && (uPos - 1) < universityList.size()) {
            jsObj.addProperty("university", universityList.get(uPos - 1).getUniversity_name());
        }
        int dPos = viewEditProfile.spEditDepartment.getSelectedItemPosition();
        if (dPos > 0 && (dPos - 1) < departmentFiltered.size()) {
            jsObj.addProperty("department_id", departmentFiltered.get(dPos - 1).getDepartment_id());
        }
        int cPos = viewEditProfile.spEditCollege.getSelectedItemPosition();
        if (cPos > 0 && (cPos - 1) < collegeList.size()) {
            jsObj.addProperty("college", collegeList.get(cPos - 1).getCollege_name());
        }
        // Extras used by My Results.
        jsObj.addProperty("rollnumber", viewEditProfile.edtRoll.getText().toString().trim());
        jsObj.addProperty("regulation", resolveSpinnerValue(viewEditProfile.spProfileRegulation,
                viewEditProfile.edtRegulationOther, R.array.result_regulation_entries));
        jsObj.addProperty("degree", resolveSpinnerValue(viewEditProfile.spProfileDegree,
                viewEditProfile.edtDegreeOther, R.array.result_degree_entries));
        if (isProfile) {
            RequestBody requestFile =
                    RequestBody.create(MediaType.parse("multipart/form-data"), new File(profile_image));
            // MultipartBody.Part is used to send also the actual file name
            body = MultipartBody.Part.createFormData("user_image", new File(profile_image).getName(), requestFile);
        }
        // add another part within the multipart request
        RequestBody requestBody_data =
                RequestBody.create(MediaType.parse("multipart/form-data"), API.toBase64(jsObj.toString()));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<EditProfileRP> call = apiService.getEditProfileData(requestBody_data, body);
        call.enqueue(new Callback<EditProfileRP>() {
            @Override
            public void onResponse(@NotNull Call<EditProfileRP> call, @NotNull Response<EditProfileRP> response) {

                try {
                    EditProfileRP editProfileRP = response.body();

                    if (editProfileRP !=null && editProfileRP.getSuccess().equals("1")) {
                        if (editProfileRP.getItemUserEdits().get(0).getSuccess().equals("1")) {
                            method.savePhone(sendPhone);
                            Events.ProfileUpdate profileUpdate = new Events.ProfileUpdate(editProfileRP.getItemUserEdits().get(0).getUser_image(),viewEditProfile.edtName.getText().toString(),sendPhone);
                            GlobalBus.getBus().post(profileUpdate);
                            Toast.makeText(EditProfileActivity.this, editProfileRP.getItemUserEdits().get(0).getMsg(), Toast.LENGTH_SHORT).show();
                            onBackPressed();
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
            public void onFailure(@NotNull Call<EditProfileRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }
}

package com.jntuh.books;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

    private static final int LAST_STEP = 4;
    private int currentStep = 1;

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
        // Privacy/terms is pre-selected by default.
        viewRegisterBinding.cbRegPrivacyTerms.setChecked(true, false);

        // The primary button advances through the steps and only submits on the last one.
        viewRegisterBinding.btnRegister.setOnClickListener(v -> {
            if (currentStep < LAST_STEP) {
                if (validateStep(currentStep)) showStep(currentStep + 1);
            } else {
                form();
            }
        });

        viewRegisterBinding.btnRegBack.setOnClickListener(v -> {
            if (currentStep > 1) showStep(currentStep - 1);
        });

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Back walks the wizard backwards before it leaves the screen.
                        if (currentStep > 1) {
                            showStep(currentStep - 1);
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });

        showStep(1);

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
        universityAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item_light, universityNames);
        universityAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        viewRegisterBinding.spRegUniversity.setAdapter(universityAdapter);

        // Department
        departmentNames.add(getString(R.string.lbl_select_department));
        departmentAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item_light, departmentNames);
        departmentAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        viewRegisterBinding.spRegDepartment.setAdapter(departmentAdapter);

        // College
        collegeNames.add(getString(R.string.lbl_select_college));
        collegeAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item_light, collegeNames);
        collegeAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        viewRegisterBinding.spRegCollege.setAdapter(collegeAdapter);

        // Gender (static)
        List<String> genderLabels = new ArrayList<>();
        genderLabels.add(getString(R.string.lbl_select_gender));
        genderLabels.add(getString(R.string.lbl_gender_male));
        genderLabels.add(getString(R.string.lbl_gender_female));
        genderLabels.add(getString(R.string.lbl_gender_other));
        genderAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item_light, genderLabels);
        genderAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        viewRegisterBinding.spRegGender.setAdapter(genderAdapter);

        // Gender is picked from the three cards; the (hidden) spinner keeps holding the
        // value so validation and submission read it exactly as before.
        viewRegisterBinding.llGenderMale.setOnClickListener(v -> selectGender(1));
        viewRegisterBinding.llGenderFemale.setOnClickListener(v -> selectGender(2));
        viewRegisterBinding.llGenderOther.setOnClickListener(v -> selectGender(3));
        paintGender();

        // Long lists get a searchable dialog on tap (gender stays a normal spinner).
        com.jntuh.util.SearchableSpinner.attach(viewRegisterBinding.spRegUniversity, getString(R.string.lbl_select_university));
        com.jntuh.util.SearchableSpinner.attach(viewRegisterBinding.spRegDepartment, getString(R.string.lbl_select_department));
        com.jntuh.util.SearchableSpinner.attach(viewRegisterBinding.spRegCollege, getString(R.string.lbl_select_college));

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

    // ---- Gender choice ----

    private void selectGender(int spinnerPosition) {
        viewRegisterBinding.spRegGender.setSelection(spinnerPosition);
        paintGender();
    }

    /** Fill the chosen disc with the brand colour and leave the others neutral. */
    private void paintGender() {
        int selected = viewRegisterBinding.spRegGender.getSelectedItemPosition();
        paintGenderChoice(viewRegisterBinding.ivGenderMaleDisc, viewRegisterBinding.ivGenderMale,
                viewRegisterBinding.ivGenderMaleLabel, selected == 1);
        paintGenderChoice(viewRegisterBinding.ivGenderFemaleDisc, viewRegisterBinding.ivGenderFemale,
                viewRegisterBinding.ivGenderFemaleLabel, selected == 2);
        paintGenderChoice(viewRegisterBinding.ivGenderOtherDisc, viewRegisterBinding.ivGenderOther,
                viewRegisterBinding.ivGenderOtherLabel, selected == 3);
    }

    private void paintGenderChoice(View disc, android.widget.ImageView icon,
                                   android.widget.TextView label, boolean selected) {
        disc.setBackgroundResource(selected
                ? R.drawable.bg_gender_circle_selected : R.drawable.bg_gender_circle);
        icon.setColorFilter(androidx.core.content.ContextCompat.getColor(this,
                selected ? R.color.white : R.color.home_by_author));
        label.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                selected ? R.color.app_bg_orange : R.color.home_by_author));
    }

    // ---- Step flow ----

    /** Swap to a step: its fields, its heading, the progress rail and the button label. */
    private void showStep(int step) {
        currentStep = step;

        viewRegisterBinding.llRegStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        viewRegisterBinding.llRegStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        viewRegisterBinding.llRegStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        viewRegisterBinding.llRegStep4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);

        int titleRes, subRes;
        if (step == 1) {
            titleRes = R.string.lbl_reg_step1_title;
            subRes = R.string.lbl_reg_step1_sub;
        } else if (step == 2) {
            titleRes = R.string.lbl_reg_step2_title;
            subRes = R.string.lbl_reg_step2_sub;
        } else if (step == 3) {
            titleRes = R.string.lbl_reg_step3_title;
            subRes = R.string.lbl_reg_step3_sub;
        } else {
            titleRes = R.string.lbl_reg_step4_title;
            subRes = R.string.lbl_reg_step4_sub;
        }
        int iconRes;
        if (step == 1) {
            iconRes = R.drawable.ic_reg_person;
        } else if (step == 2) {
            iconRes = R.drawable.ic_reg_mail;
        } else if (step == 3) {
            iconRes = R.drawable.ic_result_school;
        } else {
            iconRes = R.drawable.ic_picker_check;
        }
        viewRegisterBinding.ivRegStepIcon.setImageResource(iconRes);
        viewRegisterBinding.txtWelcomeBack.setText(getString(titleRes));
        viewRegisterBinding.txtLetsLoginA.setText(getString(subRes));
        viewRegisterBinding.tvRegStepCount.setText(getString(R.string.lbl_step_of, step, LAST_STEP));

        paintProgress(step);

        viewRegisterBinding.btnRegBack.setVisibility(step == 1 ? View.INVISIBLE : View.VISIBLE);
        viewRegisterBinding.btnRegister.setText(
                getString(step == LAST_STEP ? R.string.lbl_create_account : R.string.lbl_continue));

        if (step == LAST_STEP) buildSummary();
    }

    private void paintProgress(int step) {
        View[] segments = {
                viewRegisterBinding.vRegProgress1,
                viewRegisterBinding.vRegProgress2,
                viewRegisterBinding.vRegProgress3,
                viewRegisterBinding.vRegProgress4
        };
        for (int i = 0; i < segments.length; i++) {
            segments[i].setBackgroundResource(i < step
                    ? R.drawable.bg_reg_progress_on : R.drawable.bg_reg_progress_off);
        }
    }

    /** What the last step shows back to the user before they commit. */
    private void buildSummary() {
        StringBuilder sb = new StringBuilder();
        appendSummary(sb, getString(R.string.lbl_name), viewRegisterBinding.edtRegName.getText().toString());
        appendSummary(sb, getString(R.string.lbl_email), viewRegisterBinding.edtRegEmail.getText().toString());
        appendSummary(sb, getString(R.string.lbl_phone), viewRegisterBinding.edtRegPhone.getText().toString());

        int uPos = viewRegisterBinding.spRegUniversity.getSelectedItemPosition();
        if (uPos > 0) {
            appendSummary(sb, getString(R.string.lbl_university),
                    universityList.get(uPos - 1).getUniversity_name());
        }
        int cPos = viewRegisterBinding.spRegCollege.getSelectedItemPosition();
        if (cPos > 0) {
            appendSummary(sb, getString(R.string.lbl_college), collegeList.get(cPos - 1).getCollege_name());
        }
        appendSummary(sb, getString(R.string.lbl_roll_number),
                viewRegisterBinding.edtRegRoll.getText().toString());

        viewRegisterBinding.tvRegSummary.setText(sb.toString().trim());
    }

    private void appendSummary(StringBuilder sb, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        sb.append(getString(R.string.lbl_summary_line, label, value.trim())).append("\n");
    }

    /** Per-step checks, so a mistake is caught on the step that owns the field. */
    private boolean validateStep(int step) {
        if (step == 1) {
            String name = viewRegisterBinding.edtRegName.getText().toString();
            if (name.trim().isEmpty()) {
                viewRegisterBinding.edtRegName.requestFocus();
                viewRegisterBinding.edtRegName.setError(getString(R.string.please_enter_name));
                return false;
            }
            if (viewRegisterBinding.spRegGender.getSelectedItemPosition() <= 0) {
                Toast.makeText(this, getString(R.string.please_select_gender), Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        if (step == 2) {
            String email = viewRegisterBinding.edtRegEmail.getText().toString();
            if (email.isEmpty() || !isValidMail(email)) {
                viewRegisterBinding.edtRegEmail.requestFocus();
                viewRegisterBinding.edtRegEmail.setError(getString(R.string.please_enter_email));
                return false;
            }
            if (viewRegisterBinding.edtRegPhone.getText().toString().trim().isEmpty()) {
                viewRegisterBinding.edtRegPhone.requestFocus();
                viewRegisterBinding.edtRegPhone.setError(getString(R.string.please_enter_phone));
                return false;
            }
            if (viewRegisterBinding.edtRegPass.getText().toString().isEmpty()) {
                viewRegisterBinding.edtRegPass.requestFocus();
                viewRegisterBinding.edtRegPass.setError(getString(R.string.please_enter_password));
                return false;
            }
            return true;
        }

        if (step == 3) {
            if (viewRegisterBinding.spRegUniversity.getSelectedItemPosition() <= 0) {
                Toast.makeText(this, getString(R.string.please_select_university), Toast.LENGTH_SHORT).show();
                return false;
            }
            if (viewRegisterBinding.spRegDepartment.getSelectedItemPosition() <= 0) {
                Toast.makeText(this, getString(R.string.please_select_department), Toast.LENGTH_SHORT).show();
                return false;
            }
            if (viewRegisterBinding.spRegCollege.getSelectedItemPosition() <= 0) {
                Toast.makeText(this, getString(R.string.please_select_college), Toast.LENGTH_SHORT).show();
                return false;
            }
            if (viewRegisterBinding.edtRegRoll.getText().toString().trim().isEmpty()) {
                viewRegisterBinding.edtRegRoll.requestFocus();
                viewRegisterBinding.edtRegRoll.setError(getString(R.string.please_enter_roll));
                return false;
            }
            return true;
        }

        return true;
    }

    private boolean isValidMail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void form() {

        viewRegisterBinding.edtRegName.setError(null);
        viewRegisterBinding.edtRegEmail.setError(null);
        viewRegisterBinding.edtRegPass.setError(null);
        viewRegisterBinding.edtRegPhone.setError(null);

        // Re-run each step's checks; anything missing sends the user back to the
        // step that owns it rather than failing silently on a hidden field.
        for (int step = 1; step < LAST_STEP; step++) {
            if (!validateStep(step)) {
                showStep(step);
                validateStep(step);
                return;
            }
        }

        String name = viewRegisterBinding.edtRegName.getText().toString();
        String email = viewRegisterBinding.edtRegEmail.getText().toString();
        String password = viewRegisterBinding.edtRegPass.getText().toString();
        String phoneNo = viewRegisterBinding.edtRegPhone.getText().toString();
        String rollNo = viewRegisterBinding.edtRegRoll.getText().toString();

        int uPos = viewRegisterBinding.spRegUniversity.getSelectedItemPosition();
        int dPos = viewRegisterBinding.spRegDepartment.getSelectedItemPosition();
        int cPos = viewRegisterBinding.spRegCollege.getSelectedItemPosition();
        int gPos = viewRegisterBinding.spRegGender.getSelectedItemPosition();

        viewRegisterBinding.edtRegName.clearFocus();
        viewRegisterBinding.edtRegEmail.clearFocus();
        viewRegisterBinding.edtRegPass.clearFocus();

        String universityName = universityList.get(uPos - 1).getUniversity_name();
        String departmentId = departmentListFiltered.get(dPos - 1).getDepartment_id();
        String collegeName = collegeList.get(cPos - 1).getCollege_name();
        String gender = genders[gPos];

        if (!viewRegisterBinding.cbRegPrivacyTerms.isChecked()) {
            Toast.makeText(RegisterActivity.this, getString(R.string.please_accept), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!method.isNetworkAvailable()) {
            method.alertBox(getResources().getString(R.string.internet_connection));
            return;
        }
        register(name, email, password, phoneNo, universityName, departmentId, collegeName, gender, rollNo);
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

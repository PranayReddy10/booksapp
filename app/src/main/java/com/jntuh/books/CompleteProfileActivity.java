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
import com.jntuh.response.LoginRP;
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
 * Shown right after a Google user signs in whose account is missing any of what
 * sign-up asks for: phone, gender, University, Department, College. Google gives
 * only the name and email, so they must fill the rest before entering the app.
 * Whatever the account already has is filled in for them.
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

    /** Stored values, matched to the gender cards (0 = none picked). */
    private final String[] genders = {"", "Male", "Female", "Other"};
    private int selectedGender;

    // What the account already has, applied once the lists it points into have loaded.
    private String pendingUniversity = "";
    private String pendingDepartmentId = "";
    private String pendingCollege = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompleteProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        userId = getIntent().getStringExtra("uId");

        setupSpinners();
        setupAboutYou();

        binding.btnSaveProfile.setOnClickListener(v -> save());
        binding.btnCpFetch.setOnClickListener(v -> lookupHallTicket());

        if (method.isNetworkAvailable()) {
            loadProfile();
            loadUniversities();
            loadDepartments();
            loadColleges();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }
    }

    private void setupAboutYou() {
        binding.tvCpEmail.setText(method.getUserEmail());
        binding.edtCpPhone.setText(Method.normalizePhone(method.getUserPhone()));

        binding.llCpGenderMale.setOnClickListener(v -> selectGender(1));
        binding.llCpGenderFemale.setOnClickListener(v -> selectGender(2));
        binding.llCpGenderOther.setOnClickListener(v -> selectGender(3));
        paintGender();
    }

    private void selectGender(int position) {
        selectedGender = position;
        paintGender();
    }

    /** Fill the chosen disc with the brand colour and leave the others neutral. */
    private void paintGender() {
        paintGenderChoice(binding.ivCpGenderMaleDisc, binding.ivCpGenderMale,
                binding.tvCpGenderMale, selectedGender == 1);
        paintGenderChoice(binding.ivCpGenderFemaleDisc, binding.ivCpGenderFemale,
                binding.tvCpGenderFemale, selectedGender == 2);
        paintGenderChoice(binding.ivCpGenderOtherDisc, binding.ivCpGenderOther,
                binding.tvCpGenderOther, selectedGender == 3);
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

    /**
     * Pre-fill from the account: someone who already gave their college before
     * phone and gender became required only has to add those two.
     */
    private void loadProfile() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", userId);
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.getProfileData(API.toBase64(jsObj.toString())).enqueue(new Callback<LoginRP>() {
            @Override
            public void onResponse(@NotNull Call<LoginRP> call, @NotNull Response<LoginRP> response) {
                LoginRP body = response.body();
                if (binding == null || body == null || body.getItemUserList() == null
                        || body.getItemUserList().isEmpty()) {
                    return;
                }
                LoginRP.ItemUser u = body.getItemUserList().get(0);

                if (binding.edtCpPhone.getText().toString().trim().isEmpty()) {
                    binding.edtCpPhone.setText(Method.normalizePhone(u.getPhone()));
                }
                if (selectedGender == 0 && u.getGender() != null) {
                    for (int i = 1; i < genders.length; i++) {
                        if (genders[i].equalsIgnoreCase(u.getGender().trim())) selectGender(i);
                    }
                }
                // The hall ticket is fixed once set (results are keyed on it).
                String roll = u.getRollnumber() == null ? "" : u.getRollnumber().trim();
                if (!roll.isEmpty()) {
                    binding.edtCpRoll.setText(roll);
                    binding.edtCpRoll.setEnabled(false);
                }
                pendingUniversity = u.getUniversity() == null ? "" : u.getUniversity().trim();
                pendingDepartmentId = u.getDepartment_id() == null ? "" : u.getDepartment_id().trim();
                pendingCollege = u.getCollege() == null ? "" : u.getCollege().trim();
                applyPendingCampus();
            }

            @Override
            public void onFailure(@NotNull Call<LoginRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
            }
        });
    }

    /** Select the account's university and college once their lists are in. */
    private void applyPendingCampus() {
        if (!pendingUniversity.isEmpty() && binding.spUniversity.getSelectedItemPosition() <= 0) {
            for (int i = 0; i < universityList.size(); i++) {
                if (pendingUniversity.equalsIgnoreCase(universityList.get(i).getUniversity_name())) {
                    binding.spUniversity.setSelection(i + 1);
                    pendingUniversity = "";
                    break;
                }
            }
        }
        if (!pendingCollege.isEmpty() && binding.spCollege.getSelectedItemPosition() <= 0) {
            for (int i = 0; i < collegeList.size(); i++) {
                if (pendingCollege.equalsIgnoreCase(collegeList.get(i).getCollege_name())) {
                    binding.spCollege.setSelection(i + 1);
                    pendingCollege = "";
                    break;
                }
            }
        }
        applyPendingDepartment();
    }

    private void applyPendingDepartment() {
        if (pendingDepartmentId.isEmpty() || binding.spDepartment.getSelectedItemPosition() > 0) return;
        for (int i = 0; i < departmentFiltered.size(); i++) {
            if (pendingDepartmentId.equals(departmentFiltered.get(i).getDepartment_id())) {
                binding.spDepartment.setSelection(i + 1);
                pendingDepartmentId = "";
                return;
            }
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
                applyPendingDepartment();
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
                        applyPendingCampus();
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
                        int uPos = binding.spUniversity.getSelectedItemPosition();
                        filterDepartments(uPos > 0 ? universityList.get(uPos - 1).getUniversity_id() : null);
                        applyPendingDepartment();
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
        // Keep the chosen department if it belongs to this university too, so a
        // late re-filter (the spinner listener runs after setSelection) does not undo it.
        int dPos = binding.spDepartment.getSelectedItemPosition();
        String keepId = dPos > 0 && (dPos - 1) < departmentFiltered.size()
                ? departmentFiltered.get(dPos - 1).getDepartment_id() : null;

        departmentFiltered.clear();
        for (DepartmentList d : departmentAll) {
            if (universityId == null || universityId.equals(d.getUniversity_id())) {
                departmentFiltered.add(d);
            }
        }
        departmentNames.clear();
        departmentNames.add(getString(R.string.lbl_select_department));
        int select = 0;
        for (int i = 0; i < departmentFiltered.size(); i++) {
            DepartmentList d = departmentFiltered.get(i);
            departmentNames.add(d.getDepartment_name());
            if (keepId != null && keepId.equals(d.getDepartment_id())) select = i + 1;
        }
        departmentAdapter.notifyDataSetChanged();
        binding.spDepartment.setSelection(select);
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
                        applyPendingCampus();
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

    /** Filled by the hall-ticket lookup, sent on save when we have them. */
    private String fetchedRegulation = "";
    private String fetchedFather = "";
    private String lookedUpRoll = "";
    private boolean lookupInFlight;

    /**
     * Same lookup the sign-up flow uses: the hall ticket is enough to name the
     * student, so Google users do not have to type their branch and regulation.
     * A queued answer means the university feed is still fetching — tap again.
     */
    private void lookupHallTicket() {
        String roll = binding.edtCpRoll.getText().toString().trim().toUpperCase(java.util.Locale.US);
        if (roll.length() != 10) {
            binding.edtCpRoll.requestFocus();
            binding.edtCpRoll.setError(getString(R.string.result_fetch_invalid));
            return;
        }
        if (lookupInFlight || roll.equals(lookedUpRoll)) return;
        // The feed only covers JNTUH; other universities fill this in by hand.
        int uPos = binding.spUniversity.getSelectedItemPosition();
        String uni = uPos > 0 ? universityList.get(uPos - 1).getUniversity_name() : "";
        if (uni == null || !uni.toUpperCase(java.util.Locale.US).contains("JNTU")) {
            setFetchStatus(getString(R.string.msg_reg_lookup_other_uni));
            return;
        }
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }

        lookupInFlight = true;
        setFetchStatus(getString(R.string.msg_reg_looking_up));

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("hall_ticket_no", roll);

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.lookupHallTicket(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<com.jntuh.response.HallTicketRP>() {
            @Override
            public void onResponse(@NotNull Call<com.jntuh.response.HallTicketRP> call,
                                   @NotNull Response<com.jntuh.response.HallTicketRP> resp) {
                lookupInFlight = false;

                com.jntuh.item.HallTicketItem item = null;
                com.jntuh.response.HallTicketRP body = resp.body();
                if (body != null && body.getEbookApp() != null && !body.getEbookApp().isEmpty()) {
                    item = body.getEbookApp().get(0);
                }
                if (item == null) {
                    setFetchStatus(getString(R.string.msg_reg_lookup_manual));
                    return;
                }

                String state = item.getState() == null ? "" : item.getState();
                if ("queued".equals(state)) {
                    setFetchStatus(getString(R.string.msg_reg_lookup_queued));
                    return;
                }
                if (!"ready".equals(state)) {
                    setFetchStatus(item.getMsg() == null || item.getMsg().trim().isEmpty()
                            ? getString(R.string.msg_reg_lookup_manual) : item.getMsg());
                    return;
                }

                lookedUpRoll = roll;
                fetchedRegulation = item.getRegulation() == null ? "" : item.getRegulation().trim();
                fetchedFather = item.getFather_name() == null ? "" : item.getFather_name().trim();
                selectDepartmentByName(item.getBranch());
                setFetchStatus(getString(R.string.msg_reg_lookup_ok));
            }

            @Override
            public void onFailure(@NotNull Call<com.jntuh.response.HallTicketRP> call, @NotNull Throwable t) {
                lookupInFlight = false;
                Log.e("lookup_fail", t.toString());
                setFetchStatus(getString(R.string.msg_reg_lookup_manual));
            }
        });
    }

    /** Point the department spinner at the fetched branch, if it is in the list. */
    private void selectDepartmentByName(String branch) {
        if (branch == null || branch.trim().isEmpty()) return;
        if (binding.spDepartment.getSelectedItemPosition() > 0) return;
        for (int i = 0; i < departmentFiltered.size(); i++) {
            String name = departmentFiltered.get(i).getDepartment_name();
            if (name != null && name.trim().equalsIgnoreCase(branch.trim())) {
                binding.spDepartment.setSelection(i + 1);
                return;
            }
        }
    }

    private void setFetchStatus(String text) {
        if (text == null || text.trim().isEmpty()) {
            binding.tvCpFetchStatus.setVisibility(View.GONE);
        } else {
            binding.tvCpFetchStatus.setText(text);
            binding.tvCpFetchStatus.setVisibility(View.VISIBLE);
        }
    }

    private void save() {
        int uPos = binding.spUniversity.getSelectedItemPosition();
        int dPos = binding.spDepartment.getSelectedItemPosition();
        int cPos = binding.spCollege.getSelectedItemPosition();

        String roll = binding.edtCpRoll.getText().toString().trim().toUpperCase(java.util.Locale.US);
        String phone = Method.normalizePhone(binding.edtCpPhone.getText().toString());

        if (phone.isEmpty()) {
            binding.edtCpPhone.requestFocus();
            binding.edtCpPhone.setError(getString(R.string.please_enter_valid_phone));
            return;
        }
        if (selectedGender <= 0) {
            method.alertBox(getString(R.string.please_select_gender));
            return;
        }
        if (uPos <= 0 || dPos <= 0 || cPos <= 0) {
            method.alertBox(getString(R.string.lbl_complete_all_fields));
            return;
        }
        if (roll.length() != 10) {
            binding.edtCpRoll.requestFocus();
            binding.edtCpRoll.setError(getString(R.string.result_fetch_invalid));
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
        jsObj.addProperty("phone", phone);
        jsObj.addProperty("gender", genders[selectedGender]);
        jsObj.addProperty("password", "");
        jsObj.addProperty("university", university);
        jsObj.addProperty("department_id", departmentId);
        jsObj.addProperty("college", college);
        jsObj.addProperty("rollnumber", roll);
        if (!fetchedRegulation.isEmpty()) {
            jsObj.addProperty("regulation", fetchedRegulation);
        }
        if (!fetchedFather.isEmpty()) {
            jsObj.addProperty("father_name", fetchedFather);
        }

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getCompleteProfileData(API.toBase64(jsObj.toString())).enqueue(new Callback<EditProfileRP>() {
            @Override
            public void onResponse(@NotNull Call<EditProfileRP> call, @NotNull Response<EditProfileRP> response) {
                binding.progressComplete.setVisibility(View.GONE);
                try {
                    EditProfileRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess())) {
                        method.savePhone(phone);
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

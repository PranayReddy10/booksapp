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
        uCollege = intent.getStringExtra("uCollege");
        uGender = intent.getStringExtra("uGender");
        uYear = intent.getStringExtra("uYear");
        uRoll = intent.getStringExtra("uRoll");
        uBranch = intent.getStringExtra("uBranch");
        uRegulation = intent.getStringExtra("uRegulation");
        uDegree = intent.getStringExtra("uDegree");

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

        bindDetailRow(viewEditProfile.llUniversityRow, viewEditProfile.tvUniversityValue, uUniversity);
        bindDetailRow(viewEditProfile.llDepartmentRow, viewEditProfile.tvDepartmentValue, uDepartment);
        bindDetailRow(viewEditProfile.llCollegeRow, viewEditProfile.tvCollegeValue, uCollege);
        bindDetailRow(viewEditProfile.llYearRow, viewEditProfile.tvYearValue, uYear);
        bindDetailRow(viewEditProfile.llGenderRow, viewEditProfile.tvGenderValue, uGender);
        bindDetailRow(viewEditProfile.llRollRow, viewEditProfile.tvRollValue, uRoll);

        // Editable academic fields.
        if (uRoll != null) viewEditProfile.edtRoll.setText(uRoll);
        setupAcademicSpinner(viewEditProfile.spProfileBranch, viewEditProfile.edtBranchOther,
                R.array.result_branch_entries, uBranch);
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
        // Academic fields (source of truth for My Results).
        jsObj.addProperty("rollnumber", viewEditProfile.edtRoll.getText().toString().trim());
        jsObj.addProperty("branch", resolveSpinnerValue(viewEditProfile.spProfileBranch,
                viewEditProfile.edtBranchOther, R.array.result_branch_entries));
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

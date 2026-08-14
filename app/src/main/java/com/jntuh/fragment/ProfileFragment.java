package com.jntuh.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.bumptech.glide.Glide;
import com.jntuh.books.DownloadActivity;
import com.jntuh.books.EditProfileActivity;
import com.jntuh.books.LoginActivity;
import com.jntuh.books.MyUploadsActivity;
import com.jntuh.books.ProfileSectionActivity;
import com.jntuh.books.UploadBookActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.FragmentProfileBinding;
import com.jntuh.response.DeleteAccRP;
import com.jntuh.response.LoginRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Events;
import com.jntuh.util.GlobalBus;
import com.jntuh.util.Method;
import com.jntuh.util.StatusBarUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ProfileFragment extends Fragment {

    FragmentProfileBinding viewProfile;
    Method method;
    LoginRP.ItemUser itemUser;
    FragmentManager childFragmentManager;
    boolean isContinue = false;
    String imageProfile;
    int movePos;
    // Deep-linked sections open once, not again on every return to this tab.
    private boolean sectionOpened = false;
    ProgressDialog progressDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        StatusBarUtil.setStatusBar(requireActivity(), "profile");

//        requireActivity().getWindow().setNavigationBarColor(Color.BLACK);

//        setSystemBars(requireActivity(), R.color.purple_200, ContextCompat.getColor(requireContext(), R.color.bg_status_bar));

        viewProfile = FragmentProfileBinding.inflate(inflater, container, false);


        ViewCompat.setOnApplyWindowInsetsListener(viewProfile.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        progressDialog = new ProgressDialog(requireActivity(),R.style.MyAlertDialogStyle);
        if (getActivity() != null) {
            method = new Method(requireActivity());
            childFragmentManager = getChildFragmentManager();
            GlobalBus.getBus().register(this);

            if (getArguments() != null) {
                isContinue = getArguments().getBoolean("isContinue");
                movePos = getArguments().getInt("movePos");
            }

            viewProfile.ivMoreMenu.setOnClickListener(v -> profileMenuDialog());

            if (method.getIsLogin()) {
                viewProfile.llLogin.setVisibility(View.GONE);
                viewProfile.llProfile.setVisibility(View.GONE);
                viewProfile.llProfile2.setVisibility(View.GONE);
                viewProfile.ivMoreMenu.setVisibility(View.VISIBLE);
                viewProfile.progressProfile.setVisibility(View.GONE);
                viewProfile.llNoData.clNoDataFound.setVisibility(View.GONE);
                userProfile();
            } else {
                viewProfile.llLogin.setVisibility(View.VISIBLE);
                viewProfile.llProfile.setVisibility(View.GONE);
                viewProfile.llProfile2.setVisibility(View.GONE);
                viewProfile.ivMoreMenu.setVisibility(View.GONE);
                viewProfile.progressProfile.setVisibility(View.GONE);
                viewProfile.llNoData.clNoDataFound.setVisibility(View.GONE);
            }

            // Shelves that used to hide in the overflow sheet.
            viewProfile.llProfDownloads.setOnClickListener(v -> openLibrary("isDown"));
            viewProfile.llProfSaved.setOnClickListener(v -> openLibrary("isFav"));
            viewProfile.llProfEdit.setOnClickListener(v ->
                    startActivity(new Intent(requireActivity(), EditProfileActivity.class)));

            // A caller asked for a specific section (home's "View all", quick access):
            // open that page straight away.
            if (isContinue && !sectionOpened) {
                sectionOpened = true;
                openSection(sectionForPosition(movePos));
            }

            // Each section is its own page now, opened from a row.
            viewProfile.llSecResults.setOnClickListener(v ->
                    openSection(ProfileSectionActivity.SECTION_RESULTS));
            viewProfile.llSecContinue.setOnClickListener(v ->
                    openSection(ProfileSectionActivity.SECTION_CONTINUE));
            viewProfile.llSecUploads.setOnClickListener(v ->
                    openSection(ProfileSectionActivity.SECTION_UPLOADS));
            viewProfile.llSecSubscription.setOnClickListener(v ->
                    openSection(ProfileSectionActivity.SECTION_SUBSCRIPTION));
            viewProfile.llSecRent.setOnClickListener(v ->
                    openSection(ProfileSectionActivity.SECTION_RENT));

            viewProfile.btnLogIn.setOnClickListener(v -> {
                Intent intentLogin = new Intent(requireActivity(), LoginActivity.class);
                startActivity(intentLogin);
                requireActivity().finishAffinity();
            });

        }
        return viewProfile.getRoot();
    }

    /** Downloads ("isDown") and favourites ("isFav") share one screen. */
    private void openLibrary(String mode) {
        startActivity(new Intent(requireActivity(), DownloadActivity.class)
                .putExtra("isDown", mode));
    }

    /** Map the legacy tab index used by callers onto a section key. */
    private String sectionForPosition(int position) {
        switch (position) {
            case 1:
                return ProfileSectionActivity.SECTION_CONTINUE;
            case 2:
                return ProfileSectionActivity.SECTION_UPLOADS;
            case 3:
                return ProfileSectionActivity.SECTION_SUBSCRIPTION;
            case 4:
                return ProfileSectionActivity.SECTION_RENT;
            default:
                return ProfileSectionActivity.SECTION_RESULTS;
        }
    }

    /** Open one profile section as a full page. */
    private void openSection(String section) {
        startActivity(new Intent(requireActivity(), ProfileSectionActivity.class)
                .putExtra(ProfileSectionActivity.EXTRA_SECTION, section));
    }

    public void userProfile() {
        if (getActivity() != null) {
            viewProfile.progressProfile.setVisibility(View.VISIBLE);

            JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
            jsObj.addProperty("user_id", method.getUserId());
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
            Call<LoginRP> call = apiService.getProfileData(API.toBase64(jsObj.toString()));
            call.enqueue(new Callback<LoginRP>() {
                @Override
                public void onResponse(@NotNull Call<LoginRP> call, @NotNull Response<LoginRP> response) {
                    if (getActivity() != null) {
                        try {
                            LoginRP loginRPSocial = response.body();

                            if (loginRPSocial != null && loginRPSocial.getSuccess().equals("1")) {
                                if (loginRPSocial.getItemUserList().get(0).getSuccess().equals("1")) {
                                    itemUser = loginRPSocial.getItemUserList().get(0);
                                    viewProfile.llProfile.setVisibility(View.VISIBLE);
                                    viewProfile.llProfile2.setVisibility(View.VISIBLE);
                                    viewProfile.ivMoreMenu.setVisibility(View.VISIBLE);
                                    imageProfile = itemUser.getUser_image();
                                    if (!imageProfile.isEmpty()) {
                                        Glide.with(requireActivity().getApplicationContext()).load(imageProfile)
                                                .placeholder(R.drawable.user_profile)
                                                .into(viewProfile.ivUser);
                                    }

                                    viewProfile.tvProfileName.setText(itemUser.getName());
                                    viewProfile.tvProfileEmail.setText(itemUser.getEmail());

                                    String uname = itemUser.getUsername();
                                    String emailLocal = itemUser.getEmail() != null && itemUser.getEmail().contains("@")
                                            ? itemUser.getEmail().substring(0, itemUser.getEmail().indexOf('@'))
                                            : "";
                                    // Show the @username only when it's real and adds
                                    // information (not empty, not just a copy of the
                                    // name or the email's local part).
                                    if (uname != null && !uname.trim().isEmpty()
                                            && !uname.equalsIgnoreCase(itemUser.getName())
                                            && !uname.equalsIgnoreCase(emailLocal)) {
                                        viewProfile.tvProfileUsername.setVisibility(View.VISIBLE);
                                        viewProfile.tvProfileUsername.setText("@" + uname);
                                        method.saveMediaProfile(uname, itemUser.getUser_image());
                                    } else {
                                        viewProfile.tvProfileUsername.setVisibility(View.GONE);
                                        if (uname != null && !uname.trim().isEmpty()) {
                                            method.saveMediaProfile(uname, itemUser.getUser_image());
                                        }
                                    }
                                } else {
                                    method.alertBox(loginRPSocial.getItemUserList().get(0).getMsg());
                                }
                            } else {
                                viewProfile.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                                viewProfile.progressProfile.setVisibility(View.GONE);
                                method.alertBox(getString(R.string.failed_try_again));
                            }

                        } catch (Exception e) {
                            Log.d("exception_error", e.toString());
                            method.alertBox(getString(R.string.failed_try_again));
                        }
                    }
                    viewProfile.progressProfile.setVisibility(View.GONE);

                }

                @Override
                public void onFailure(@NotNull Call<LoginRP> call, @NotNull Throwable t) {
                    // Log error here since request failed
                    Log.e("fail", t.toString());
                    viewProfile.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                    viewProfile.progressProfile.setVisibility(View.GONE);
                    method.alertBox(getString(R.string.failed_try_again));
                }
            });
        }
    }


    @Subscribe
    public void getName(Events.ProfileUpdate profileUpdate) {
        imageProfile = profileUpdate.getImage();
        Glide.with(requireActivity().getApplicationContext()).load(imageProfile)
                .placeholder(R.drawable.placeholder_portable)
                .into(viewProfile.ivUser);
        viewProfile.tvProfileName.setText(profileUpdate.getName());
        itemUser.setName(profileUpdate.getName());
        itemUser.setPhone(profileUpdate.getPhone());
        // Re-fetch the full profile so ALL edited fields (university, department,
        // college, roll, regulation, degree) refresh in the cache and the UI,
        // not just name/phone.
        if (method != null && method.isNetworkAvailable()) {
            userProfile();
        }
    }

    public void profileMenuDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
        dialog.setContentView(R.layout.layout_option_profile);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        RelativeLayout layoutLogout = dialog.findViewById(R.id.layLogout);
        RelativeLayout layoutEditProfile = dialog.findViewById(R.id.layEditProfile);
        RelativeLayout layoutDeleteAcc = dialog.findViewById(R.id.layDeleteAcc);
        RelativeLayout layoutDownload = dialog.findViewById(R.id.layDownload);
        RelativeLayout layoutFav = dialog.findViewById(R.id.layFav);
        RelativeLayout layoutUploadBook = dialog.findViewById(R.id.layUploadBook);
        RelativeLayout layoutMyUploads = dialog.findViewById(R.id.layMyUploads);
        RelativeLayout layoutMyMedia = dialog.findViewById(R.id.layMyMedia);

        assert layoutDownload != null;
        layoutDownload.setOnClickListener(v -> {
            Intent intentDown = new Intent(requireActivity(), DownloadActivity.class);
            intentDown.putExtra("isDown", "isDown");
            startActivity(intentDown);
            dialog.dismiss();
        });

        assert layoutFav != null;
        layoutFav.setOnClickListener(v -> {
            Intent intentFav = new Intent(requireActivity(), DownloadActivity.class);
            intentFav.putExtra("isDown", "isFav");
            startActivity(intentFav);
            dialog.dismiss();
        });

        assert layoutUploadBook != null;
        layoutUploadBook.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), UploadBookActivity.class));
            dialog.dismiss();
        });

        assert layoutMyUploads != null;
        layoutMyUploads.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), MyUploadsActivity.class));
            dialog.dismiss();
        });

        assert layoutMyMedia != null;
        layoutMyMedia.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), com.jntuh.books.MyMediaActivity.class));
            dialog.dismiss();
        });

        assert layoutLogout != null;
        layoutLogout.setOnClickListener(v -> {
            logoutDialog();
            dialog.dismiss();
        });

        assert layoutEditProfile != null;
        layoutEditProfile.setOnClickListener(v -> {
            Intent intentProfile = new Intent(requireActivity(), EditProfileActivity.class);
            intentProfile.putExtra("uId", itemUser.getUser_id());
            intentProfile.putExtra("uName", itemUser.getName());
            intentProfile.putExtra("uEmail", itemUser.getEmail());
            intentProfile.putExtra("uUsername", itemUser.getUsername());
            intentProfile.putExtra("uImage", imageProfile);
            intentProfile.putExtra("uPhone", itemUser.getPhone());
            intentProfile.putExtra("uType", method.getUserType());
            intentProfile.putExtra("uUniversity", itemUser.getUniversity());
            intentProfile.putExtra("uDepartment", itemUser.getDepartment());
            intentProfile.putExtra("uCollege", itemUser.getCollege());
            intentProfile.putExtra("uGender", itemUser.getGender());
            intentProfile.putExtra("uYear", itemUser.getYear());
            intentProfile.putExtra("uRoll", itemUser.getRollnumber());
            intentProfile.putExtra("uBranch", itemUser.getBranch());
            intentProfile.putExtra("uRegulation", itemUser.getRegulation());
            intentProfile.putExtra("uDegree", itemUser.getDegree());
            startActivity(intentProfile);
            dialog.dismiss();
        });

        assert layoutDeleteAcc != null;
        layoutDeleteAcc.setOnClickListener(v -> {
            dialog.dismiss();
            deleteDialog();
        });


        dialog.show();
    }

    public void logoutDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
        dialog.setContentView(R.layout.layout_logout);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        AppCompatButton buttonCancel = dialog.findViewById(R.id.btnMaybeLater);
        AppCompatButton buttonYes = dialog.findViewById(R.id.btnSubmit);

        assert buttonCancel != null;
        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        assert buttonYes != null;
        buttonYes.setOnClickListener(v -> {
            if (method.getUserType().equals("google")) {

                // Configure sign-in to request the ic_user_login's ID, email address, and basic
                // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();

                // Build a GoogleSignInClient with the options specified by gso.
                //Google login
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

                mGoogleSignInClient.signOut()
                        .addOnCompleteListener(requireActivity(), task -> {
                            method.saveIsLogin(false);
                            method.setUserId("");
                            startActivity(new Intent(requireActivity(), LoginActivity.class));
                            requireActivity().finishAffinity();
                        });
            } else {
                method.saveIsLogin(false);
                method.setUserId("");
                startActivity(new Intent(requireActivity(), LoginActivity.class));
                requireActivity().finishAffinity();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    public void deleteDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
        dialog.setContentView(R.layout.layout_logout);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        AppCompatButton buttonCancel = dialog.findViewById(R.id.btnMaybeLater);
        AppCompatButton buttonYes = dialog.findViewById(R.id.btnSubmit);
        TextView tvTitle=dialog.findViewById(R.id.tvBookReview);
        TextView tvMsg=dialog.findViewById(R.id.txtMsg);
        buttonYes.setText(getString(R.string.delete_acc_btn));
        tvTitle.setText(getString(R.string.delete_acc));
        tvMsg.setText(getString(R.string.delete_acc_info));

        assert buttonCancel != null;
        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonYes.setOnClickListener(v -> {
            deleteAcc();
            dialog.dismiss();
        });

        dialog.show();
    }

    public void deleteAcc() {

        progressDialog.show();
        progressDialog.setMessage(getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<DeleteAccRP> call = apiService.getDeleteData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<DeleteAccRP>() {
            @Override
            public void onResponse(@NotNull Call<DeleteAccRP> call, @NotNull Response<DeleteAccRP> response) {

                try {
                    DeleteAccRP dataRP = response.body();
                    assert dataRP != null;
                    if (dataRP.getItemDeleteAcc().get(0).getDeleteSuccess().equals("1")) {
                            method.saveIsLogin(false);
                            method.setUserId("");
                            // Fully sign out of Google so a later Google login starts
                            // fresh (otherwise Google silently returns the deleted
                            // account and login gets stuck).
                            try {
                                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                                        GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
                                GoogleSignIn.getClient(requireActivity(), gso).signOut();
                            } catch (Exception ignored) {
                            }
                            startActivity(new Intent(requireActivity(), LoginActivity.class));
                            requireActivity().finishAffinity();
                            Toast.makeText(requireActivity(), dataRP.getItemDeleteAcc().get(0).getDeleteMsg(), Toast.LENGTH_SHORT).show();
                        }else {
                            method.alertBox(dataRP.getItemDeleteAcc().get(0).getDeleteMsg());
                        }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }

                progressDialog.dismiss();

            }

            @Override
            public void onFailure(@NotNull Call<DeleteAccRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }
}

package com.jntuh.books;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.jntuh.books.databinding.ActivityLoginBinding;
import com.jntuh.response.LoginRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Constant;
import com.jntuh.util.Method;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {


    ActivityLoginBinding viewLoginBinding;
    public static final String mypreference = "mypref";
    public static final String pref_email = "pref_email";
    public static final String pref_password = "pref_password";
    public static final String pref_check = "pref_check";
    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;
    GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 007;
    Method method;
    ProgressDialog progressDialog;
    LoginRP.ItemUser itemUser;
    boolean isWhichScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewLoginBinding = ActivityLoginBinding.inflate(getLayoutInflater());

        View view = viewLoginBinding.getRoot();
        setContentView(view);

        // Ambient drift for the mesh background blobs.
        com.jntuh.util.AuthAnimator.floatView(viewLoginBinding.authBlobTeal, 40f, -55f, 4200);
        com.jntuh.util.AuthAnimator.floatView(viewLoginBinding.authBlobViolet, -45f, 35f, 5200);

        pref = getSharedPreferences(mypreference, 0); // 0 - for private mode
        editor = pref.edit();
        method = new Method(LoginActivity.this);
        method.saveFirstIsLogin(true);
        method.forceRTLIfSupported();

        progressDialog = new ProgressDialog(LoginActivity.this,R.style.MyAlertDialogStyle);

        Intent intent = getIntent();
        isWhichScreen = intent.getBooleanExtra("isFromDetail", false);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        viewLoginBinding.cbRemMe.setChecked(false);
        // Privacy/terms is pre-selected by default.
        viewLoginBinding.cbPrivacyTerms.setChecked(true, false);
        if (pref.getBoolean(pref_check, false)) {
            viewLoginBinding.edtEmail.setText(pref.getString(pref_email, null));
            viewLoginBinding.edtPass.setText(pref.getString(pref_password, null));
            viewLoginBinding.cbRemMe.setChecked(true);
        } else {
            viewLoginBinding.edtEmail.setText("");
            viewLoginBinding.edtPass.setText("");
            viewLoginBinding.cbRemMe.setChecked(false);
        }

        viewLoginBinding.tvPrivacyTerms.setOnClickListener(v -> {
                Intent intentPage = new Intent(LoginActivity.this, PagesActivity.class);
                intentPage.putExtra("PAGE_TITLE", Constant.appListData.getPageList().get(1).getPageTitle());
                intentPage.putExtra("PAGE_DESC", Constant.appListData.getPageList().get(1).getPageContent());
                startActivity(intentPage);
        });

        viewLoginBinding.btnSkip.setOnClickListener(v -> {
            Intent intent_skip = new Intent(LoginActivity.this, MainActivity.class);
            intent_skip.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent_skip);
            finishAffinity();
        });

        viewLoginBinding.tvSignUp.setOnClickListener(v -> {
            Intent intent_register = new Intent(LoginActivity.this, RegisterActivity.class);
            intent_register.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent_register);
        });

        viewLoginBinding.cbRemMe.setOnCheckedChangeListener((checkBox, isChecked) -> {

        });

        viewLoginBinding.btnLogIn.setOnClickListener(v -> {
            String strEmail = viewLoginBinding.edtEmail.getText().toString();
            String strPassword = viewLoginBinding.edtPass.getText().toString();

            viewLoginBinding.edtEmail.setError(null);
            viewLoginBinding.edtPass.setError(null);

            if (!isValidMail(strEmail) || strEmail.isEmpty()) {
                viewLoginBinding.edtEmail.requestFocus();
                viewLoginBinding.edtEmail.setError(getResources().getString(R.string.please_enter_email));
            } else if (strPassword.isEmpty()) {
                viewLoginBinding.edtPass.requestFocus();
                viewLoginBinding.edtPass.setError(getResources().getString(R.string.please_enter_password));
            } else {
                if (viewLoginBinding.cbPrivacyTerms.isChecked()) {

                    if (viewLoginBinding.cbRemMe.isChecked()) {
                        editor.putString(pref_email, viewLoginBinding.edtEmail.getText().toString());
                        editor.putString(pref_password, viewLoginBinding.edtPass.getText().toString());
                        editor.putBoolean(pref_check, true);
                        editor.commit();
                    } else {
                        editor.putBoolean(pref_check, false);
                        editor.commit();
                    }

                    if (method.isNetworkAvailable()) {
                        login(strEmail, strPassword);
                    } else {
                        method.alertBox(getResources().getString(R.string.internet_connection));
                    }
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.please_accept), Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewLoginBinding.cbPrivacyTerms.setOnCheckedChangeListener((checkBox, isChecked) -> {

        });

        viewLoginBinding.llGoogle.setOnClickListener(v -> {
            if (viewLoginBinding.cbPrivacyTerms.isChecked()) {
                signIn();
            } else {
                Toast.makeText(LoginActivity.this, getString(R.string.please_accept), Toast.LENGTH_SHORT).show();
            }
        });

        viewLoginBinding.tvForgotPassword.setOnClickListener(v -> {
            Intent intent_forgot = new Intent(LoginActivity.this, ForgotPassActivity.class);
            intent_forgot.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent_forgot);
        });

    }

    private boolean isValidMail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void login(final String sendEmail, final String sendPassword) {

        progressDialog.show();
        progressDialog.setMessage(getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(LoginActivity.this));
        jsObj.addProperty("email", sendEmail);
        jsObj.addProperty("password", sendPassword);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<LoginRP> call = apiService.getLoginData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<LoginRP>() {
            @Override
            public void onResponse(@NotNull Call<LoginRP> call, @NotNull Response<LoginRP> response) {

                try {
                    LoginRP loginRP = response.body();

                    if (loginRP !=null && loginRP.getSuccess().equals("1")) {
                        if (loginRP.getItemUserList().get(0).getSuccess().equals("1")) {
                            itemUser = loginRP.getItemUserList().get(0);
                            method.saveIsLogin(true);
                            method.saveLogin(itemUser.getUser_id(), itemUser.getName(), itemUser.getEmail(), "normal", "");
                            method.saveMediaProfile(itemUser.getUsername(), itemUser.getUser_image());
                            if (isWhichScreen) {
                                finish();
                            } else {
                                ActivityCompat.finishAffinity(LoginActivity.this);
                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(i);
                                finish();
                            }

                        } else {
                            method.alertBox(loginRP.getItemUserList().get(0).getMsg());
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
            public void onFailure(@NotNull Call<LoginRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }

    public void registerSocialNetwork(String aid, String sendName, String sendEmail, String type, String photoUrl) {

        progressDialog.show();
        progressDialog.setMessage(getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(LoginActivity.this));
        jsObj.addProperty("name", sendName);
        jsObj.addProperty("email", sendEmail);
        jsObj.addProperty("social_id", aid);
        jsObj.addProperty("login_type", type);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            jsObj.addProperty("user_image", photoUrl);
        }
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<LoginRP> call = apiService.getLoginSocialData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<LoginRP>() {
            @Override
            public void onResponse(@NotNull Call<LoginRP> call, @NotNull Response<LoginRP> response) {

                try {
                    LoginRP loginRPSocial = response.body();

                    if (loginRPSocial !=null && loginRPSocial.getSuccess().equals("1")) {
                        if (loginRPSocial.getItemUserList().get(0).getSuccess().equals("1")) {
                            itemUser = loginRPSocial.getItemUserList().get(0);
                            method.saveIsLogin(true);
                            method.saveLogin(itemUser.getUser_id(), itemUser.getName(), itemUser.getEmail(), type, aid);
                            method.saveMediaProfile(itemUser.getUsername(), itemUser.getUser_image());

                            // Google users must complete University/Department/College
                            // before entering the app.
                            if ("0".equals(itemUser.getProfile_complete())) {
                                progressDialog.dismiss();
                                Intent ci = new Intent(LoginActivity.this, CompleteProfileActivity.class);
                                ci.putExtra("uId", itemUser.getUser_id());
                                startActivity(ci);
                                finish();
                                return;
                            }

                            if (isWhichScreen) {
                                finish();
                            } else {
                                ActivityCompat.finishAffinity(LoginActivity.this);
                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(i);
                                finish();
                            }

                        } else {
                            method.alertBox(loginRPSocial.getItemUserList().get(0).getMsg());
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
            public void onFailure(@NotNull Call<LoginRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }


    //Google login
    private void signIn() {
        if (method.isNetworkAvailable()) {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } else {
            method.alertBox(getResources().getString(R.string.internet_connection));
        }

    }

    //Google login get callback
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }

    }

    //Google login
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.

            assert account != null;
            String id = account.getId();
            String name = account.getDisplayName();
            String email = account.getEmail();
            String photo = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

            registerSocialNetwork(id, name, email, "google", photo);

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
        }
    }

}
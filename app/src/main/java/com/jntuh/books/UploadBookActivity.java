package com.jntuh.books;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.jntuh.books.databinding.ActivityUploadBookBinding;
import com.jntuh.item.CategoryList;
import com.jntuh.item.DepartmentList;
import com.jntuh.response.CatRP;
import com.jntuh.response.DepartmentRP;
import com.jntuh.response.UserUploadRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadBookActivity extends AppCompatActivity {

    private ActivityUploadBookBinding binding;
    private Method method;
    private ProgressDialog progressDialog;

    private final List<CategoryList> categoryList = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    private final List<DepartmentList> departmentList = new ArrayList<>();
    private final List<String> departmentNames = new ArrayList<>();
    private ArrayAdapter<String> departmentAdapter;

    private String coverPath = null;   // copied local path for cover
    private String filePath = null;    // copied local path for book file
    private String fileName = null;

    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // 50MB

    // Keys to persist picker state across activity recreation (fixes cover disappearing
    // when the system file picker recreates this activity).
    private static final String KEY_COVER = "state_cover_path";
    private static final String KEY_FILE = "state_file_path";
    private static final String KEY_FILE_NAME = "state_file_name";

    private ActivityResultLauncher<Intent> coverPicker;
    private ActivityResultLauncher<Intent> filePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        method.forceRTLIfSupported();
        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);

        binding.toolbarMain.tvToolbarTitle.setText(getString(R.string.lbl_upload_book));
        binding.toolbarMain.ivSearch.setVisibility(View.GONE);
        binding.toolbarMain.imageFilter.setVisibility(View.GONE);
        binding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Restore picker state if the activity was recreated.
        if (savedInstanceState != null) {
            coverPath = savedInstanceState.getString(KEY_COVER, null);
            filePath = savedInstanceState.getString(KEY_FILE, null);
            fileName = savedInstanceState.getString(KEY_FILE_NAME, null);
        }

        registerPickers();
        setupCategorySpinner();
        setupDepartmentSpinner();
        setupSourceToggle();
        restorePreviews();

        binding.btnPickCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            coverPicker.launch(Intent.createChooser(intent, getString(R.string.lbl_pick_cover)));
        });

        binding.btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"application/pdf", "application/epub+zip"});
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePicker.launch(Intent.createChooser(intent, getString(R.string.lbl_pick_file)));
        });

        binding.btnSubmitUpload.setOnClickListener(v -> validateAndSubmit());

        if (method.isNetworkAvailable()) {
            loadCategories();
            loadDepartments();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Persist the picked cover/file so they survive recreation while the
        // system file picker is in the foreground.
        if (coverPath != null) outState.putString(KEY_COVER, coverPath);
        if (filePath != null) outState.putString(KEY_FILE, filePath);
        if (fileName != null) outState.putString(KEY_FILE_NAME, fileName);
    }

    // Re-show the cover thumbnail and file name after a restore.
    private void restorePreviews() {
        if (coverPath != null && new File(coverPath).exists()) {
            binding.ivCoverPreview.setVisibility(View.VISIBLE);
            Glide.with(getApplicationContext()).load(new File(coverPath))
                    .into(binding.ivCoverPreview);
        }
        if (fileName != null) {
            binding.tvFileName.setVisibility(View.VISIBLE);
            binding.tvFileName.setText(fileName);
        }
    }

    private void registerPickers() {
        coverPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String copied = copyUriToCache(uri, "cover_");
                            if (copied != null) {
                                coverPath = copied;
                                binding.ivCoverPreview.setVisibility(View.VISIBLE);
                                Glide.with(getApplicationContext()).load(new File(coverPath))
                                        .into(binding.ivCoverPreview);
                            }
                        }
                    }
                });

        filePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) handlePickedFile(uri);
                    }
                });
    }

    private void handlePickedFile(Uri uri) {
        String displayName = queryDisplayName(uri);
        if (displayName == null) displayName = "book";
        String lower = displayName.toLowerCase();
        if (!lower.endsWith(".pdf") && !lower.endsWith(".epub")) {
            Toast.makeText(this, getString(R.string.invalid_file_type), Toast.LENGTH_LONG).show();
            return;
        }
        long size = querySize(uri);
        if (size > MAX_FILE_BYTES) {
            method.alertBox(getString(R.string.file_too_large));
            return;
        }
        String copied = copyUriToCache(uri, "book_");
        if (copied != null) {
            filePath = copied;
            fileName = displayName;
            binding.tvFileName.setVisibility(View.VISIBLE);
            binding.tvFileName.setText(displayName);
            // NOTE: cover preview is intentionally left untouched here so picking a
            // file never clears a previously chosen cover.
        }
    }

    private void setupCategorySpinner() {
        categoryNames.add(getString(R.string.lbl_select_category));
        categoryAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        binding.spCategory.setAdapter(categoryAdapter);
        com.jntuh.util.SearchableSpinner.attach(binding.spCategory, getString(R.string.lbl_select_category));
    }

    private void setupDepartmentSpinner() {
        departmentNames.add(getString(R.string.lbl_select_department));
        departmentAdapter = new ArrayAdapter<>(this, R.layout.row_spinner_item, departmentNames);
        departmentAdapter.setDropDownViewResource(R.layout.row_spinner_dropdown_item);
        binding.spDepartment.setAdapter(departmentAdapter);
        com.jntuh.util.SearchableSpinner.attach(binding.spDepartment, getString(R.string.lbl_select_department));
    }

    private void setupSourceToggle() {
        binding.rgSource.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isFile = checkedId == R.id.rbFile;
            binding.btnPickFile.setVisibility(isFile ? View.VISIBLE : View.GONE);
            binding.tvFileName.setVisibility(isFile && fileName != null ? View.VISIBLE : View.GONE);
            binding.edtLink.setVisibility(isFile ? View.GONE : View.VISIBLE);
        });
    }

    private void loadCategories() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(UploadBookActivity.this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<CatRP> call = apiService.getAllCatData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<CatRP>() {
            @Override
            public void onResponse(@NotNull Call<CatRP> call, @NotNull Response<CatRP> response) {
                try {
                    CatRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess()) && body.getCategoryLists() != null) {
                        categoryList.clear();
                        categoryList.addAll(body.getCategoryLists());
                        categoryNames.clear();
                        categoryNames.add(getString(R.string.lbl_select_category));
                        for (CategoryList c : categoryList) categoryNames.add(c.getPost_title());
                        categoryAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<CatRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
            }
        });
    }

    private void loadDepartments() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(UploadBookActivity.this));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<DepartmentRP> call = apiService.getDepartmentListData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<DepartmentRP>() {
            @Override
            public void onResponse(@NotNull Call<DepartmentRP> call, @NotNull Response<DepartmentRP> response) {
                try {
                    DepartmentRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess()) && body.getDepartmentLists() != null) {
                        departmentList.clear();
                        departmentList.addAll(body.getDepartmentLists());
                        departmentNames.clear();
                        departmentNames.add(getString(R.string.lbl_select_department));
                        for (DepartmentList d : departmentList) departmentNames.add(d.getDepartment_name());
                        departmentAdapter.notifyDataSetChanged();
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

    private void validateAndSubmit() {
        String title = binding.edtTitle.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        int catPos = binding.spCategory.getSelectedItemPosition();
        int deptPos = binding.spDepartment.getSelectedItemPosition();
        boolean isFileMode = binding.rbFile.isChecked();
        String link = binding.edtLink.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_title), Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_description), Toast.LENGTH_SHORT).show();
            return;
        }
        if (catPos <= 0) {
            Toast.makeText(this, getString(R.string.please_select_category), Toast.LENGTH_SHORT).show();
            return;
        }
        if (deptPos <= 0) {
            Toast.makeText(this, getString(R.string.please_select_department), Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFileMode && filePath == null) {
            Toast.makeText(this, getString(R.string.please_pick_file), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isFileMode && (link.isEmpty() || !android.util.Patterns.WEB_URL.matcher(link).matches())) {
            Toast.makeText(this, getString(R.string.please_enter_link), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }

        String catId = categoryList.get(catPos - 1).getPost_id();
        String deptId = departmentList.get(deptPos - 1).getDepartment_id();
        submit(title, description, catId, deptId, isFileMode, link);
    }

    private void submit(String title, String description, String catId, String deptId,
                        boolean isFileMode, String link) {
        progressDialog.show();
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(UploadBookActivity.this));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("title", title);
        jsObj.addProperty("description", description);
        jsObj.addProperty("cat_id", catId);
        jsObj.addProperty("department_id", deptId);
        jsObj.addProperty("url_type", isFileMode ? "local" : "server_url");
        if (!isFileMode) jsObj.addProperty("book_url", link);

        RequestBody dataBody = RequestBody.create(
                MediaType.parse("multipart/form-data"), API.toBase64(jsObj.toString()));

        MultipartBody.Part coverPart = null;
        if (coverPath != null) {
            File f = new File(coverPath);
            RequestBody rb = RequestBody.create(MediaType.parse("multipart/form-data"), f);
            coverPart = MultipartBody.Part.createFormData("book_image", f.getName(), rb);
        }

        MultipartBody.Part filePart = null;
        if (isFileMode && filePath != null) {
            File f = new File(filePath);
            RequestBody rb = RequestBody.create(MediaType.parse("multipart/form-data"), f);
            filePart = MultipartBody.Part.createFormData("book_file", f.getName(), rb);
        }

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<UserUploadRP> call = apiService.getUserUploadBookData(dataBody, coverPart, filePart);
        call.enqueue(new Callback<UserUploadRP>() {
            @Override
            public void onResponse(@NotNull Call<UserUploadRP> call, @NotNull Response<UserUploadRP> response) {
                progressDialog.dismiss();
                try {
                    UserUploadRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess())
                            && body.getItemUserUploadList() != null
                            && !body.getItemUserUploadList().isEmpty()
                            && "1".equals(body.getItemUserUploadList().get(0).getSuccess())) {
                        String msg = body.getItemUserUploadList().get(0).getMsg();
                        Toast.makeText(UploadBookActivity.this,
                                msg != null ? msg : getString(R.string.upload_submitted),
                                Toast.LENGTH_LONG).show();
                        startActivity(new Intent(UploadBookActivity.this, MyUploadsActivity.class));
                        finish();
                    } else {
                        String msg = (body != null && body.getItemUserUploadList() != null
                                && !body.getItemUserUploadList().isEmpty())
                                ? body.getItemUserUploadList().get(0).getMsg() : null;
                        method.alertBox(msg != null ? msg : getString(R.string.failed_try_again));
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getString(R.string.failed_try_again));
                }
            }

            @Override
            public void onFailure(@NotNull Call<UserUploadRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(getString(R.string.failed_try_again));
            }
        });
    }

    // ---- URI helpers ----

    private String queryDisplayName(Uri uri) {
        String name = null;
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception e) {
            Log.d("uri_name", e.toString());
        }
        if (name == null) name = uri.getLastPathSegment();
        return name;
    }

    private long querySize(Uri uri) {
        long size = 0;
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0 && !c.isNull(idx)) size = c.getLong(idx);
            }
        } catch (Exception e) {
            Log.d("uri_size", e.toString());
        }
        return size;
    }

    // Copy content:// stream into cache dir so we have a real File path for multipart.
    private String copyUriToCache(Uri uri, String prefix) {
        try {
            String display = queryDisplayName(uri);
            String safe = (display != null) ? display.replaceAll("[^a-zA-Z0-9._-]", "_") : "file";
            File out = new File(getCacheDir(), prefix + System.currentTimeMillis() + "_" + safe);
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(out)) {
                if (in == null) return null;
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            Log.d("copy_uri", e.toString());
            return null;
        }
    }
}

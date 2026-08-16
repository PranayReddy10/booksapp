package com.jntuh.books;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.jntuh.books.databinding.ActivityMediaUploadBinding;
import com.jntuh.response.MediaUploadRP;
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

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MediaUploadActivity extends AppCompatActivity {

    private ActivityMediaUploadBinding binding;
    private Method method;
    private ProgressDialog progressDialog;

    private String mediaPath = null;   // copied local path for the picked media
    private String mediaName = null;
    private boolean isVideo = false;

    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;   // 10MB
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;  // 100MB

    private static final String KEY_MEDIA = "state_media_path";
    private static final String KEY_MEDIA_NAME = "state_media_name";
    private static final String KEY_IS_VIDEO = "state_is_video";
    private static final String KEY_MEDIA_LIST = "state_media_list";

    /** Photos a single post may carry. */
    private static final int MAX_PHOTOS = 6;

    /** Picked photo paths (videos stay single, so this holds at most one). */
    private final java.util.ArrayList<String> mediaPaths = new java.util.ArrayList<>();

    /** Optional: "photo" or "video" — which attachment the picker should offer. */
    public static final String EXTRA_ATTACH_TYPE = "attach_type";

    private ActivityResultLauncher<Intent> mediaPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        method.forceRTLIfSupported();
        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);

        binding.ivComposeClose.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // The composer shows who is posting, like the feed card it becomes.
        String avatar = method.getUserImage();
        if (avatar != null && !avatar.trim().isEmpty()) {
            Glide.with(getApplicationContext()).load(avatar)
                    .placeholder(R.drawable.img_user)
                    .into(binding.ivComposeAvatar);
        }

        if (savedInstanceState != null) {
            java.util.ArrayList<String> saved = savedInstanceState.getStringArrayList(KEY_MEDIA_LIST);
            if (saved != null) mediaPaths.addAll(saved);
            mediaPath = savedInstanceState.getString(KEY_MEDIA, null);
            mediaName = savedInstanceState.getString(KEY_MEDIA_NAME, null);
            isVideo = savedInstanceState.getBoolean(KEY_IS_VIDEO, false);
        }

        // Home's photo / video chips arrive with the type already chosen.
        String attachType = getIntent().getStringExtra(EXTRA_ATTACH_TYPE);
        if (savedInstanceState == null && attachType != null) {
            isVideo = "video".equalsIgnoreCase(attachType);
        }

        registerPicker();
        restorePreview();

        binding.rgMediaType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean nowVideo = checkedId == R.id.rbVideo;
            if (nowVideo != isVideo) {
                // Switching type invalidates any previously picked file.
                isVideo = nowVideo;
                clearMedia();
            }
        });

        binding.btnPickMedia.setOnClickListener(v -> {
            if (!isVideo && mediaPaths.size() >= MAX_PHOTOS) {
                Toast.makeText(this, getString(R.string.msg_photo_limit, MAX_PHOTOS),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(isVideo ? "video/*" : "image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // Photos can come in batches; a post carries at most one video.
            if (!isVideo) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            mediaPicker.launch(Intent.createChooser(intent, getString(R.string.lbl_pick_media)));
        });

        binding.btnSubmitMedia.setOnClickListener(v -> validateAndSubmit());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(KEY_MEDIA_LIST, mediaPaths);
        if (mediaPath != null) outState.putString(KEY_MEDIA, mediaPath);
        if (mediaName != null) outState.putString(KEY_MEDIA_NAME, mediaName);
        outState.putBoolean(KEY_IS_VIDEO, isVideo);
    }

    /** Drop every attachment; the post reverts to text only. */
    private void clearMedia() {
        mediaPaths.clear();
        mediaPath = null;
        mediaName = null;
        renderStrip();
    }

    private void restorePreview() {
        binding.rbVideo.setChecked(isVideo);
        binding.rbPhoto.setChecked(!isVideo);
        // Anything the cache lost between sessions is dropped rather than shown broken.
        for (int i = mediaPaths.size() - 1; i >= 0; i--) {
            if (!new File(mediaPaths.get(i)).exists()) mediaPaths.remove(i);
        }
        mediaPath = mediaPaths.isEmpty() ? null : mediaPaths.get(0);
        renderStrip();
    }

    private void registerPicker() {
        mediaPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        // Multi-select returns a clip; take them in the order picked.
                        android.content.ClipData clip = data.getClipData();
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            handlePickedMedia(clip.getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        handlePickedMedia(data.getData());
                    }
                });
    }

    private void handlePickedMedia(Uri uri) {
        String displayName = queryDisplayName(uri);
        if (displayName == null) displayName = isVideo ? "video" : "photo";
        String lower = displayName.toLowerCase();

        boolean okType = isVideo
                ? (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".m4v"))
                : (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                    || lower.endsWith(".gif") || lower.endsWith(".webp"));
        if (!okType) {
            Toast.makeText(this, getString(R.string.invalid_media_type), Toast.LENGTH_LONG).show();
            return;
        }

        long size = querySize(uri);
        long cap = isVideo ? MAX_VIDEO_BYTES : MAX_PHOTO_BYTES;
        if (size > cap) {
            method.alertBox(getString(isVideo ? R.string.video_too_large : R.string.photo_too_large));
            return;
        }

        if (!isVideo && mediaPaths.size() >= MAX_PHOTOS) {
            Toast.makeText(this, getString(R.string.msg_photo_limit, MAX_PHOTOS),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String copied = copyUriToCache(uri, isVideo ? "video_" : "photo_");
        if (copied != null) {
            // A video replaces whatever was there; photos accumulate.
            if (isVideo) mediaPaths.clear();
            mediaPaths.add(copied);
            mediaPath = mediaPaths.get(0);
            mediaName = displayName;
            renderStrip();
        }
    }

    /**
     * Redraw the attachment strip from the picked paths: a thumbnail each, a remove
     * button, and a "cover" badge on the first photo (the one feeds show).
     */
    private void renderStrip() {
        binding.llMediaStrip.removeAllViews();

        for (int i = 0; i < mediaPaths.size(); i++) {
            final String path = mediaPaths.get(i);
            View tile = getLayoutInflater().inflate(
                    R.layout.row_compose_media, binding.llMediaStrip, false);

            android.widget.ImageView thumb = tile.findViewById(R.id.ivComposeThumb);
            if (isVideo) {
                Bitmap frame = firstFrame(path);
                if (frame != null) {
                    thumb.setImageBitmap(frame);
                } else {
                    thumb.setImageResource(R.color.card_view_bg);
                }
            } else {
                Glide.with(getApplicationContext()).load(new File(path)).into(thumb);
            }

            // Only a multi-photo post needs to say which one leads.
            tile.findViewById(R.id.tvComposeCover)
                    .setVisibility(i == 0 && mediaPaths.size() > 1 ? View.VISIBLE : View.GONE);

            tile.findViewById(R.id.ivComposeRemove).setOnClickListener(v -> {
                mediaPaths.remove(path);
                mediaPath = mediaPaths.isEmpty() ? null : mediaPaths.get(0);
                if (mediaPaths.isEmpty()) mediaName = null;
                renderStrip();
            });

            binding.llMediaStrip.addView(tile);
        }

        // The file name only makes sense for a lone attachment.
        if (mediaPaths.size() == 1 && mediaName != null) {
            binding.tvMediaName.setVisibility(View.VISIBLE);
            binding.tvMediaName.setText(mediaName);
        } else {
            binding.tvMediaName.setVisibility(View.GONE);
        }
    }

    private void validateAndSubmit() {
        String title = binding.edtTitle.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();

        // Text-only posts are allowed; an empty post is not.
        if (mediaPath == null && description.isEmpty() && title.isEmpty()) {
            binding.edtDescription.requestFocus();
            Toast.makeText(this, getString(R.string.please_write_something), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }
        submit(title, description);
    }

    private void submit(String title, String description) {
        progressDialog.show();
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(MediaUploadActivity.this));
        jsObj.addProperty("user_id", method.getUserId());
        // No attachment → a text post; the server stores it without a file.
        String mediaType = (mediaPath == null) ? "text" : (isVideo ? "video" : "photo");
        jsObj.addProperty("media_type", mediaType);
        jsObj.addProperty("title", title);
        jsObj.addProperty("description", description);

        RequestBody dataBody = RequestBody.create(
                MediaType.parse("multipart/form-data"), API.toBase64(jsObj.toString()));

        // First file goes as "media_file" (what the server reads today); any further
        // photos ride along as "media_file[]" for a server that understands them.
        java.util.List<MultipartBody.Part> parts = new java.util.ArrayList<>();
        for (int i = 0; i < mediaPaths.size(); i++) {
            File f = new File(mediaPaths.get(i));
            RequestBody rb = RequestBody.create(MediaType.parse("multipart/form-data"), f);
            parts.add(MultipartBody.Part.createFormData(
                    i == 0 ? "media_file" : "media_file[]", f.getName(), rb));
        }

        // Optional: generate + attach a thumbnail for videos.
        if (isVideo && mediaPath != null) {
            File thumbFile = generateThumbFile(mediaPath);
            if (thumbFile != null) {
                RequestBody trb = RequestBody.create(MediaType.parse("multipart/form-data"), thumbFile);
                parts.add(MultipartBody.Part.createFormData("thumb_file", thumbFile.getName(), trb));
            }
        }

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        // A text post sends no file parts at all.
        Call<MediaUploadRP> call = apiService.getMediaUploadMultiData(dataBody, parts);
        call.enqueue(new Callback<MediaUploadRP>() {
            @Override
            public void onResponse(@NotNull Call<MediaUploadRP> call, @NotNull Response<MediaUploadRP> response) {
                progressDialog.dismiss();
                try {
                    MediaUploadRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess())
                            && body.getItemMediaUploadList() != null
                            && !body.getItemMediaUploadList().isEmpty()
                            && "1".equals(body.getItemMediaUploadList().get(0).getSuccess())) {

                        MediaUploadRP.ItemMediaUpload item = body.getItemMediaUploadList().get(0);
                        boolean autoApproved = "1".equals(item.getAuto_approved());
                        Toast.makeText(MediaUploadActivity.this,
                                autoApproved ? getString(R.string.media_live) : getString(R.string.media_pending),
                                Toast.LENGTH_LONG).show();

                        Intent next = autoApproved
                                ? new Intent(MediaUploadActivity.this, MediaFeedActivity.class)
                                : new Intent(MediaUploadActivity.this, MyMediaActivity.class);
                        startActivity(next);
                        finish();
                    } else {
                        String msg = (body != null && body.getItemMediaUploadList() != null
                                && !body.getItemMediaUploadList().isEmpty())
                                ? body.getItemMediaUploadList().get(0).getMsg() : null;
                        method.alertBox(msg != null ? msg : getString(R.string.failed_try_again));
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getString(R.string.failed_try_again));
                }
            }

            @Override
            public void onFailure(@NotNull Call<MediaUploadRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(getString(R.string.failed_try_again));
            }
        });
    }

    // ---- Thumbnail helpers ----

    private Bitmap firstFrame(String path) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(path);
            return r.getFrameAtTime(0);
        } catch (Exception e) {
            Log.d("thumb", e.toString());
            return null;
        } finally {
            try {
                r.release();
            } catch (Exception ignored) {
            }
        }
    }

    private File generateThumbFile(String path) {
        Bitmap frame = firstFrame(path);
        if (frame == null) return null;
        try {
            File out = new File(getCacheDir(), "thumb_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                frame.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            }
            return out;
        } catch (Exception e) {
            Log.d("thumb_save", e.toString());
            return null;
        }
    }

    // ---- URI helpers (same as UploadBookActivity) ----

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

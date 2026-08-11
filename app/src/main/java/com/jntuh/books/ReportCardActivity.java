package com.jntuh.books;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.item.CardItem;
import com.jntuh.response.ReportCardRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Full-screen viewer for the server-generated report card PNG. Never draws the
 * card on the client. Supports regenerating (report_generate) and sharing the
 * image file (via FileProvider) together with the public share URL text.
 */
public class ReportCardActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "image_url";
    public static final String EXTRA_SHARE_URL = "share_url";

    private Method method;
    private String imageUrl;
    private String shareUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_card);
        method = new Method(this);

        imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        shareUrl = getIntent().getStringExtra(EXTRA_SHARE_URL);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        findViewById(R.id.ivRefresh).setOnClickListener(v -> generate());
        findViewById(R.id.btnShareReport).setOnClickListener(v -> shareImage());

        if (TextUtils.isEmpty(imageUrl)) {
            generate();
        } else {
            loadImage(imageUrl);
        }
    }

    private void loadImage(String url) {
        progress(true);
        empty(false);
        final android.widget.ImageView iv = findViewById(R.id.ivReport);
        Glide.with(getApplicationContext())
                .load(url)
                .into(new CustomTarget<android.graphics.drawable.Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                @Nullable Transition<? super android.graphics.drawable.Drawable> transition) {
                        progress(false);
                        empty(false);
                        iv.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                        progress(false);
                        empty(true);
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                        iv.setImageDrawable(placeholder);
                    }
                });
    }

    private void generate() {
        progress(true);
        empty(false);
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.generateReportCard(API.toBase64(jsObj.toString())).enqueue(new Callback<ReportCardRP>() {
            @Override
            public void onResponse(@NotNull Call<ReportCardRP> call, @NotNull Response<ReportCardRP> resp) {
                ReportCardRP body = resp.body();
                if (body != null && body.getEbookApp() != null && !body.getEbookApp().isEmpty()) {
                    CardItem card = body.getEbookApp().get(0);
                    if (!TextUtils.isEmpty(card.getReport_image_url())) {
                        imageUrl = card.getReport_image_url();
                        if (!TextUtils.isEmpty(card.getShare_url())) shareUrl = card.getShare_url();
                        loadImage(imageUrl);
                        return;
                    }
                }
                progress(false);
                empty(true);
            }

            @Override
            public void onFailure(@NotNull Call<ReportCardRP> call, @NotNull Throwable t) {
                progress(false);
                empty(true);
            }
        });
    }

    /** Download the PNG to cache, then share the file + the share URL text. */
    private void shareImage() {
        if (TextUtils.isEmpty(imageUrl)) {
            // Nothing loaded yet — fall back to a plain link share if we have one.
            if (!TextUtils.isEmpty(shareUrl)) shareTextOnly();
            else Toast.makeText(this, getString(R.string.result_report_generating), Toast.LENGTH_SHORT).show();
            return;
        }
        shareProgress(true);
        Glide.with(getApplicationContext())
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                        shareProgress(false);
                        try {
                            File dir = new File(getCacheDir(), "shared_reports");
                            //noinspection ResultOfMethodCallIgnored
                            dir.mkdirs();
                            File file = new File(dir, "report_card.png");
                            FileOutputStream fos = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            fos.flush();
                            fos.close();

                            Uri uri = FileProvider.getUriForFile(
                                    ReportCardActivity.this,
                                    getPackageName() + ".fileprovider",
                                    file);

                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setType("image/png");
                            share.putExtra(Intent.EXTRA_STREAM, uri);
                            if (!TextUtils.isEmpty(shareUrl)) {
                                share.putExtra(Intent.EXTRA_TEXT, shareUrl);
                            }
                            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(share, getString(R.string.share_via)));
                        } catch (Exception e) {
                            Toast.makeText(ReportCardActivity.this,
                                    getString(R.string.result_share_failed), Toast.LENGTH_SHORT).show();
                            shareTextOnly();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) { }

                    @Override
                    public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                        shareProgress(false);
                        shareTextOnly();
                    }
                });
    }

    private void shareTextOnly() {
        if (TextUtils.isEmpty(shareUrl)) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, shareUrl);
        startActivity(Intent.createChooser(share, getString(R.string.share_via)));
    }

    private void progress(boolean show) {
        findViewById(R.id.progressReport).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void empty(boolean show) {
        findViewById(R.id.tvReportEmpty).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void shareProgress(boolean show) {
        findViewById(R.id.progressShare).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.ivShareIcon).setVisibility(show ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnShareReport).setEnabled(!show);
    }
}

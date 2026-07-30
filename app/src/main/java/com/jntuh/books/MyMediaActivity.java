package com.jntuh.books;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jntuh.adapter.MyMediaAdapter;
import com.jntuh.books.databinding.ActivityMyMediaBinding;
import com.jntuh.item.MyMediaItem;
import com.jntuh.response.MyMediaRP;
import com.jntuh.response.PostRateRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyMediaActivity extends AppCompatActivity implements MyMediaAdapter.OnDeleteListener {

    private ActivityMyMediaBinding binding;
    private Method method;
    private final List<MyMediaItem> mediaList = new ArrayList<>();
    private MyMediaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyMediaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        method.forceRTLIfSupported();

        binding.toolbarMain.tvToolbarTitle.setText(getString(R.string.lbl_my_media));
        binding.toolbarMain.ivSearch.setVisibility(View.GONE);
        binding.toolbarMain.imageFilter.setVisibility(View.GONE);
        binding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        adapter = new MyMediaAdapter(this, mediaList, this);
        binding.rvMyMedia.setHasFixedSize(true);
        binding.rvMyMedia.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvMyMedia.setAdapter(adapter);

        binding.swipeMedia.setOnRefreshListener(this::loadMedia);

        loadMedia();
    }

    private void loadMedia() {
        if (!method.isNetworkAvailable()) {
            binding.swipeMedia.setRefreshing(false);
            binding.progressMedia.setVisibility(View.GONE);
            method.alertBox(getString(R.string.internet_connection));
            return;
        }

        if (!binding.swipeMedia.isRefreshing()) {
            binding.progressMedia.setVisibility(View.VISIBLE);
        }
        binding.tvNoMedia.setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(MyMediaActivity.this));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MyMediaRP> call = apiService.getMyUploadedMediaData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<MyMediaRP>() {
            @Override
            public void onResponse(@NotNull Call<MyMediaRP> call, @NotNull Response<MyMediaRP> response) {
                binding.progressMedia.setVisibility(View.GONE);
                binding.swipeMedia.setRefreshing(false);
                try {
                    MyMediaRP body = response.body();
                    mediaList.clear();
                    if (body != null && "1".equals(body.getSuccess()) && body.getMyMediaItems() != null) {
                        mediaList.addAll(body.getMyMediaItems());
                    }
                    adapter.notifyDataSetChanged();
                    binding.tvNoMedia.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    binding.tvNoMedia.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(@NotNull Call<MyMediaRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                binding.progressMedia.setVisibility(View.GONE);
                binding.swipeMedia.setRefreshing(false);
                binding.tvNoMedia.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDelete(MyMediaItem item, int position) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_delete_media))
                .setPositiveButton(android.R.string.ok, (d, w) -> deleteMedia(item, position))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteMedia(MyMediaItem item, int position) {
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(MyMediaActivity.this));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("post_id", item.getPost_id());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<PostRateRP> call = apiService.getMediaDeleteData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<PostRateRP>() {
            @Override
            public void onResponse(@NotNull Call<PostRateRP> call, @NotNull Response<PostRateRP> response) {
                try {
                    PostRateRP body = response.body();
                    boolean ok = body != null && "1".equals(body.getSuccess());
                    if (ok && position >= 0 && position < mediaList.size()) {
                        mediaList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, mediaList.size());
                        binding.tvNoMedia.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        // Fall back to a full refresh if the row index drifted.
                        loadMedia();
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    loadMedia();
                }
            }

            @Override
            public void onFailure(@NotNull Call<PostRateRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                method.alertBox(getString(R.string.failed_try_again));
            }
        });
    }
}

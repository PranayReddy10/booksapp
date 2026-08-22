package com.jntuh.books;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jntuh.adapter.MyUploadAdapter;
import com.jntuh.books.databinding.ActivityMyUploadsBinding;
import com.jntuh.item.MyUploadList;
import com.jntuh.response.MyUploadRP;
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

public class MyUploadsActivity extends AppCompatActivity {

    private ActivityMyUploadsBinding binding;
    private Method method;
    private final List<MyUploadList> uploadList = new ArrayList<>();
    private MyUploadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyUploadsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        method.forceRTLIfSupported();

        binding.toolbarMain.tvToolbarTitle.setText(getString(R.string.lbl_my_uploads));
        binding.toolbarMain.ivSearch.setVisibility(View.GONE);
        binding.toolbarMain.imageFilter.setVisibility(View.GONE);
        binding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        adapter = new MyUploadAdapter(this, uploadList);
        binding.rvMyUploads.setHasFixedSize(true);
        binding.rvMyUploads.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvMyUploads.setAdapter(adapter);

        binding.swipeUploads.setOnRefreshListener(this::loadUploads);
        binding.llUploadCoins.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, MyCoinsActivity.class)));

        loadUploads();
    }

    /**
     * The per-book coins add up to something, and this is where a student is
     * already looking at those numbers — so total them here and offer the way
     * through to spending them. Stays hidden unless the server says coins are on.
     */
    private void bindCoinsStrip() {
        boolean coinsOn = false;
        int total = 0;
        for (com.jntuh.item.MyUploadList item : uploadList) {
            if (item.getCoins_enabled() == 1) {
                coinsOn = true;
                total += item.getCoins_earned();
            }
        }

        binding.llUploadCoins.setVisibility(coinsOn ? View.VISIBLE : View.GONE);
        if (coinsOn) {
            binding.tvUploadCoinsTotal.setText(getString(R.string.msg_upload_coins_total,
                    java.text.NumberFormat.getInstance().format(total)));
        }
    }

    private void loadUploads() {
        if (!method.isNetworkAvailable()) {
            binding.swipeUploads.setRefreshing(false);
            binding.progressUploads.setVisibility(View.GONE);
            method.alertBox(getString(R.string.internet_connection));
            return;
        }

        if (!binding.swipeUploads.isRefreshing()) {
            binding.progressUploads.setVisibility(View.VISIBLE);
        }
        binding.tvNoUploads.setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(MyUploadsActivity.this));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MyUploadRP> call = apiService.getMyUploadedBooksData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<MyUploadRP>() {
            @Override
            public void onResponse(@NotNull Call<MyUploadRP> call, @NotNull Response<MyUploadRP> response) {
                binding.progressUploads.setVisibility(View.GONE);
                binding.swipeUploads.setRefreshing(false);
                try {
                    MyUploadRP body = response.body();
                    uploadList.clear();
                    if (body != null && "1".equals(body.getSuccess()) && body.getMyUploadLists() != null) {
                        uploadList.addAll(body.getMyUploadLists());
                    }
                    adapter.notifyDataSetChanged();
                    binding.tvNoUploads.setVisibility(uploadList.isEmpty() ? View.VISIBLE : View.GONE);
                    bindCoinsStrip();
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    binding.tvNoUploads.setVisibility(uploadList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(@NotNull Call<MyUploadRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                binding.progressUploads.setVisibility(View.GONE);
                binding.swipeUploads.setRefreshing(false);
                binding.tvNoUploads.setVisibility(uploadList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }
}

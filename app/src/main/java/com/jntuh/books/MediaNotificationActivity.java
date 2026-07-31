package com.jntuh.books;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.adapter.MediaNotificationAdapter;
import com.jntuh.books.databinding.ActivityMediaNotificationBinding;
import com.jntuh.item.MediaNotificationItem;
import com.jntuh.response.MediaNotificationListRP;
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

public class MediaNotificationActivity extends AppCompatActivity
        implements MediaNotificationAdapter.OnNotificationClickListener {

    private ActivityMediaNotificationBinding binding;
    private Method method;

    private final List<MediaNotificationItem> notifications = new ArrayList<>();
    private MediaNotificationAdapter adapter;
    private LinearLayoutManager layoutManager;

    private int pageIndex = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        method.forceRTLIfSupported();

        binding.toolbarMain.tvToolbarTitle.setText(getString(R.string.lbl_notifications));
        binding.toolbarMain.ivSearch.setVisibility(View.GONE);
        // Reuse the filter icon slot as a "mark all read" action.
        binding.toolbarMain.imageFilter.setVisibility(View.VISIBLE);
        binding.toolbarMain.imageFilter.setImageResource(android.R.drawable.ic_menu_agenda);
        binding.toolbarMain.imageFilter.setOnClickListener(v -> markAllRead());
        binding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        layoutManager = new LinearLayoutManager(this);
        binding.rvNotifications.setLayoutManager(layoutManager);
        adapter = new MediaNotificationAdapter(this, notifications, this);
        binding.rvNotifications.setAdapter(adapter);

        binding.swipeNotifications.setOnRefreshListener(() -> {
            pageIndex = 1;
            notifications.clear();
            adapter.notifyDataSetChanged();
            loadNotifications(false);
        });

        binding.rvNotifications.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NotNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;
                int last = layoutManager.findLastVisibleItemPosition();
                if (!isLoading && pageIndex < totalPages && last >= notifications.size() - 3) {
                    pageIndex++;
                    loadNotifications(false);
                }
            }
        });

        if (method.isNetworkAvailable()) {
            loadNotifications(true);
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }
    }

    private void loadNotifications(boolean first) {
        if (isLoading) return;
        isLoading = true;
        if (first) binding.progressNotifications.setVisibility(View.VISIBLE);
        binding.tvNoNotifications.setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("page", pageIndex);

        ApiClient.getClient().create(ApiInterface.class)
                .getMediaNotificationListData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<MediaNotificationListRP>() {
                    @Override
                    public void onResponse(@NotNull Call<MediaNotificationListRP> call, @NotNull Response<MediaNotificationListRP> response) {
                        isLoading = false;
                        binding.progressNotifications.setVisibility(View.GONE);
                        binding.swipeNotifications.setRefreshing(false);
                        try {
                            MediaNotificationListRP body = response.body();
                            if (body != null && "1".equals(body.getSuccess())) {
                                totalPages = Math.max(body.getTotal_pages(), 1);
                                int start = notifications.size();
                                if (body.getNotificationItems() != null) {
                                    notifications.addAll(body.getNotificationItems());
                                }
                                if (start == 0) adapter.notifyDataSetChanged();
                                else adapter.notifyItemRangeInserted(start, notifications.size() - start);
                            }
                            binding.tvNoNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                        } catch (Exception e) {
                            Log.d("exception_error", e.toString());
                            binding.tvNoNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<MediaNotificationListRP> call, @NotNull Throwable t) {
                        Log.e("fail", t.toString());
                        isLoading = false;
                        binding.progressNotifications.setVisibility(View.GONE);
                        binding.swipeNotifications.setRefreshing(false);
                        binding.tvNoNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    public void onClick(MediaNotificationItem item, int position) {
        // Mark this one read (locally + server), then open the post.
        markRead(item.getNotification_id());
        if (position >= 0 && position < notifications.size()) {
            notifications.get(position).setIs_read("1");
            adapter.notifyItemChanged(position);
        }
        Intent intent = new Intent(this, MediaFeedActivity.class);
        intent.putExtra(MediaFeedActivity.EXTRA_POST_ID, item.getPost_id());
        startActivity(intent);
    }

    private void markRead(String notificationId) {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("notification_id", notificationId);
        fireRead(jsObj);
    }

    private void markAllRead() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("notification_id", "all");
        fireRead(jsObj);
        for (MediaNotificationItem n : notifications) n.setIs_read("1");
        adapter.notifyDataSetChanged();
    }

    private void fireRead(JsonObject jsObj) {
        ApiClient.getClient().create(ApiInterface.class)
                .getMediaNotificationReadData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<PostRateRP>() {
                    @Override public void onResponse(@NotNull Call<PostRateRP> c, @NotNull Response<PostRateRP> r) {}
                    @Override public void onFailure(@NotNull Call<PostRateRP> c, @NotNull Throwable t) {}
                });
    }
}

package com.jntuh.books;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.adapter.MediaFeedAdapter;
import com.jntuh.books.databinding.ActivityMediaFeedBinding;
import com.jntuh.item.MediaItem;
import com.jntuh.response.MediaFeedRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reels-style vertical feed. A RecyclerView with a PagerSnapHelper gives one
 * post per screen; the adapter owns a single ExoPlayer that plays only the
 * centered video. Pagination appends pages until current_page >= total_pages.
 */
public class MediaFeedActivity extends AppCompatActivity {

    private ActivityMediaFeedBinding binding;
    private Method method;

    private final List<MediaItem> feed = new ArrayList<>();
    private MediaFeedAdapter adapter;
    private LinearLayoutManager layoutManager;

    private int pageIndex = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    // post_ids we've already counted a view for this session (fire once each).
    private final Set<String> viewedPosts = new HashSet<>();
    private int currentCenter = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaFeedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);

        binding.ivFeedBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.fabFeedUpload.setOnClickListener(v -> {
            if (method.getIsLogin()) {
                startActivity(new Intent(MediaFeedActivity.this, MediaUploadActivity.class));
            } else {
                Toast.makeText(MediaFeedActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MediaFeedActivity.this, LoginActivity.class));
            }
        });

        binding.ivFeedMyMedia.setOnClickListener(v -> {
            if (method.getIsLogin()) {
                startActivity(new Intent(MediaFeedActivity.this, MyMediaActivity.class));
            } else {
                Toast.makeText(MediaFeedActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MediaFeedActivity.this, LoginActivity.class));
            }
        });

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.rvFeed.setLayoutManager(layoutManager);
        binding.rvFeed.setHasFixedSize(true);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(binding.rvFeed);

        adapter = new MediaFeedAdapter(this, feed);
        binding.rvFeed.setAdapter(adapter);

        binding.rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NotNull RecyclerView rv, int newState) {
                super.onScrollStateChanged(rv, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    onSettledOnPage();
                }
            }

            @Override
            public void onScrolled(@NotNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                // Prefetch the next page as we approach the end.
                int last = layoutManager.findLastVisibleItemPosition();
                if (!isLoading && pageIndex < totalPages && last >= feed.size() - 2) {
                    pageIndex++;
                    loadFeed();
                }
            }
        });

        if (method.isNetworkAvailable()) {
            loadFeed();
        } else {
            binding.progressFeed.setVisibility(View.GONE);
            method.alertBox(getString(R.string.internet_connection));
        }
    }

    // Called when the list settles on a page: switch playback + count a view.
    private void onSettledOnPage() {
        int pos = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (pos == RecyclerView.NO_POSITION) {
            pos = layoutManager.findFirstVisibleItemPosition();
        }
        if (pos == RecyclerView.NO_POSITION || pos == currentCenter) return;
        currentCenter = pos;

        adapter.playPosition(pos);
        countView(pos);
    }

    private void countView(int position) {
        if (position < 0 || position >= feed.size()) return;
        String postId = feed.get(position).getPost_id();
        if (postId == null || viewedPosts.contains(postId)) return;
        viewedPosts.add(postId);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(MediaFeedActivity.this));
        jsObj.addProperty("post_id", postId);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        // Best-effort: we don't care about the response.
        apiService.getMediaViewData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NotNull Call<JsonObject> call, @NotNull Response<JsonObject> response) {
                    }

                    @Override
                    public void onFailure(@NotNull Call<JsonObject> call, @NotNull Throwable t) {
                    }
                });
    }

    private void loadFeed() {
        if (isLoading) return;
        isLoading = true;
        if (feed.isEmpty()) binding.progressFeed.setVisibility(View.VISIBLE);
        binding.tvNoFeed.setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(MediaFeedActivity.this));
        // (optional) jsObj.addProperty("media_type", "photo"|"video");  // omit for all
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MediaFeedRP> call = apiService.getMediaFeedData(API.toBase64(jsObj.toString()), pageIndex);
        call.enqueue(new Callback<MediaFeedRP>() {
            @Override
            public void onResponse(@NotNull Call<MediaFeedRP> call, @NotNull Response<MediaFeedRP> response) {
                isLoading = false;
                binding.progressFeed.setVisibility(View.GONE);
                try {
                    MediaFeedRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess())) {
                        totalPages = Math.max(body.getTotal_pages(), 1);
                        int startCount = feed.size();
                        if (body.getMediaItems() != null) {
                            feed.addAll(body.getMediaItems());
                        }
                        if (startCount == 0) {
                            adapter.notifyDataSetChanged();
                            // Kick off playback/view for the first page once laid out.
                            binding.rvFeed.post(MediaFeedActivity.this::onSettledOnPage);
                        } else {
                            adapter.notifyItemRangeInserted(startCount, feed.size() - startCount);
                        }
                    }
                    binding.tvNoFeed.setVisibility(feed.isEmpty() ? View.VISIBLE : View.GONE);
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    binding.tvNoFeed.setVisibility(feed.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(@NotNull Call<MediaFeedRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                isLoading = false;
                binding.progressFeed.setVisibility(View.GONE);
                binding.tvNoFeed.setVisibility(feed.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop playback but keep the player around for a quick resume.
        if (adapter != null) adapter.detachPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume playback on whatever page we're sitting on.
        if (adapter != null && currentCenter >= 0) {
            binding.rvFeed.post(() -> adapter.playPosition(currentCenter));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.releasePlayer();
    }
}

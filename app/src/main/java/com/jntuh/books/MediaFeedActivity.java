package com.jntuh.books;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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
import com.jntuh.response.MediaLikeRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Constant;
import com.jntuh.util.MediaFeedHandoff;
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
 * Full-screen vertical feed (one post per screen). Reached three ways:
 *  - from the Explore grid via {@link MediaFeedHandoff} (opens focused on a tile),
 *  - from a notification / deep link via {@link #EXTRA_POST_ID} (loads that post first),
 *  - standalone (loads page 1).
 * Owns view-counting, the like toggle, and routing to comments/share/link.
 */
public class MediaFeedActivity extends AppCompatActivity implements MediaFeedAdapter.InteractionListener {

    public static final String EXTRA_FROM_HANDOFF = "from_handoff";
    public static final String EXTRA_POST_ID = "post_id";

    private ActivityMediaFeedBinding binding;
    private Method method;

    private final List<MediaItem> feed = new ArrayList<>();
    private MediaFeedAdapter adapter;
    private LinearLayoutManager layoutManager;

    private int pageIndex = 1;
    private int totalPages = 1;
    private boolean isLoading = false;
    private String mediaTypeFilter = "";

    private final Set<String> viewedPosts = new HashSet<>();
    private int currentCenter = -1;

    // Position whose comment count may have changed while the comments screen was open.
    private int pendingCommentRefreshPos = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaFeedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);

        binding.ivFeedBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.ivFeedMenu.setOnClickListener(this::showFeedMenu);

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.rvFeed.setLayoutManager(layoutManager);

        // Pull-to-refresh: reload the feed from the top.
        binding.swipeFeed.setOnRefreshListener(() -> {
            pageIndex = 1;
            feed.clear();
            adapter.notifyDataSetChanged();
            loadFeed(true);
        });
        binding.rvFeed.setHasFixedSize(true);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(binding.rvFeed);

        adapter = new MediaFeedAdapter(this, feed, this);
        binding.rvFeed.setAdapter(adapter);

        binding.rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NotNull RecyclerView rv, int newState) {
                super.onScrollStateChanged(rv, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) onSettledOnPage();
            }

            @Override
            public void onScrolled(@NotNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                int last = layoutManager.findLastVisibleItemPosition();
                if (!isLoading && pageIndex < totalPages && last >= feed.size() - 2) {
                    pageIndex++;
                    loadFeed(false);
                }
            }
        });

        // Decide how we were opened.
        boolean fromHandoff = getIntent().getBooleanExtra(EXTRA_FROM_HANDOFF, false);
        String focusPostId = getIntent().getStringExtra(EXTRA_POST_ID);

        if (fromHandoff && MediaFeedHandoff.hasData()) {
            consumeHandoff();
        } else if (focusPostId != null && !focusPostId.isEmpty()) {
            // Deep link / notification: show that post, then fill the rest.
            loadFocusedThenFeed(focusPostId);
        } else if (method.isNetworkAvailable()) {
            loadFeed(true);
        } else {
            binding.progressFeed.setVisibility(View.GONE);
            method.alertBox(getString(R.string.internet_connection));
        }
    }

    private void requireLoginThen(Class<?> target) {
        if (method.getIsLogin()) {
            startActivity(new Intent(this, target));
        } else {
            Toast.makeText(this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void showFeedMenu(android.view.View anchor) {
        androidx.appcompat.widget.PopupMenu popup =
                new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_feed, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            } else if (id == R.id.menu_upload_media) {
                requireLoginThen(MediaUploadActivity.class);
            } else if (id == R.id.menu_manage_feed) {
                requireLoginThen(MyMediaActivity.class);
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, MainActivity.class)
                        .putExtra("openProfile", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            } else if (id == R.id.menu_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            return true;
        });
        popup.show();
    }

    private void consumeHandoff() {
        feed.addAll(MediaFeedHandoff.getItems());
        pageIndex = MediaFeedHandoff.getPageIndex();
        totalPages = MediaFeedHandoff.getTotalPages();
        mediaTypeFilter = MediaFeedHandoff.getMediaTypeFilter();
        int start = MediaFeedHandoff.getStartPosition();
        MediaFeedHandoff.clear();

        binding.progressFeed.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        binding.rvFeed.scrollToPosition(start);
        binding.rvFeed.post(this::onSettledOnPage);
    }

    private void onSettledOnPage() {
        int pos = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (pos == RecyclerView.NO_POSITION) pos = layoutManager.findFirstVisibleItemPosition();
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

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("post_id", postId);
        ApiClient.getClient().create(ApiInterface.class)
                .getMediaViewData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(@NotNull Call<JsonObject> c, @NotNull Response<JsonObject> r) {}
                    @Override public void onFailure(@NotNull Call<JsonObject> c, @NotNull Throwable t) {}
                });
    }

    private void loadFeed(boolean first) {
        if (isLoading) return;
        isLoading = true;
        if (first && feed.isEmpty()) binding.progressFeed.setVisibility(View.VISIBLE);
        binding.tvNoFeed.setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        if (method.getIsLogin()) jsObj.addProperty("user_id", method.getUserId());
        if (!mediaTypeFilter.isEmpty()) jsObj.addProperty("media_type", mediaTypeFilter);

        ApiClient.getClient().create(ApiInterface.class)
                .getMediaFeedData(API.toBase64(jsObj.toString()), pageIndex)
                .enqueue(new Callback<MediaFeedRP>() {
                    @Override
                    public void onResponse(@NotNull Call<MediaFeedRP> call, @NotNull Response<MediaFeedRP> response) {
                        isLoading = false;
                        binding.progressFeed.setVisibility(View.GONE);
                        binding.swipeFeed.setRefreshing(false);
                        try {
                            MediaFeedRP body = response.body();
                            if (body != null && "1".equals(body.getSuccess())) {
                                totalPages = Math.max(body.getTotal_pages(), 1);
                                int startCount = feed.size();
                                if (body.getMediaItems() != null) feed.addAll(body.getMediaItems());
                                if (startCount == 0) {
                                    adapter.notifyDataSetChanged();
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
                        binding.swipeFeed.setRefreshing(false);
                        binding.tvNoFeed.setVisibility(feed.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    // For notifications/deep links: pull page 1 and jump to the matching post if present.
    private void loadFocusedThenFeed(String focusPostId) {
        binding.progressFeed.setVisibility(View.VISIBLE);
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        if (method.getIsLogin()) jsObj.addProperty("user_id", method.getUserId());

        ApiClient.getClient().create(ApiInterface.class)
                .getMediaFeedData(API.toBase64(jsObj.toString()), 1)
                .enqueue(new Callback<MediaFeedRP>() {
                    @Override
                    public void onResponse(@NotNull Call<MediaFeedRP> call, @NotNull Response<MediaFeedRP> response) {
                        binding.progressFeed.setVisibility(View.GONE);
                        try {
                            MediaFeedRP body = response.body();
                            if (body != null && "1".equals(body.getSuccess()) && body.getMediaItems() != null) {
                                totalPages = Math.max(body.getTotal_pages(), 1);
                                pageIndex = 1;
                                feed.addAll(body.getMediaItems());
                                adapter.notifyDataSetChanged();
                                int idx = indexOfPost(focusPostId);
                                if (idx >= 0) {
                                    binding.rvFeed.scrollToPosition(idx);
                                }
                                binding.rvFeed.post(MediaFeedActivity.this::onSettledOnPage);
                            }
                            binding.tvNoFeed.setVisibility(feed.isEmpty() ? View.VISIBLE : View.GONE);
                        } catch (Exception e) {
                            Log.d("exception_error", e.toString());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<MediaFeedRP> call, @NotNull Throwable t) {
                        Log.e("fail", t.toString());
                        binding.progressFeed.setVisibility(View.GONE);
                    }
                });
    }

    private int indexOfPost(String postId) {
        for (int i = 0; i < feed.size(); i++) {
            if (postId.equals(feed.get(i).getPost_id())) return i;
        }
        return -1;
    }

    // ---- InteractionListener ----

    @Override
    public void onLike(MediaItem item, int position) {
        if (!method.getIsLogin()) {
            requireLoginThen(LoginActivity.class);
            return;
        }
        // Optimistic flip.
        boolean wasLiked = "1".equals(item.getIs_liked());
        int count = parse(item.getLike_count());
        item.setIs_liked(wasLiked ? "0" : "1");
        item.setLike_count(String.valueOf(Math.max(0, wasLiked ? count - 1 : count + 1)));
        if (position >= 0) adapter.refreshLike(position);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("post_id", item.getPost_id());
        ApiClient.getClient().create(ApiInterface.class)
                .getMediaLikeData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<MediaLikeRP>() {
                    @Override
                    public void onResponse(@NotNull Call<MediaLikeRP> call, @NotNull Response<MediaLikeRP> response) {
                        MediaLikeRP body = response.body();
                        if (body == null || body.getItemLikeList() == null || body.getItemLikeList().isEmpty()) return;
                        MediaLikeRP.ItemLike r = body.getItemLikeList().get(0);
                        if ("1".equals(r.getSuccess())) {
                            item.setIs_liked(r.getIs_liked());
                            item.setLike_count(r.getLike_count());
                        } else {
                            // Server refused (likes off): revert.
                            item.setIs_liked(wasLiked ? "1" : "0");
                            item.setLike_count(String.valueOf(count));
                            if (r.getMsg() != null) {
                                Toast.makeText(MediaFeedActivity.this, r.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        if (position >= 0) adapter.refreshLike(position);
                    }

                    @Override
                    public void onFailure(@NotNull Call<MediaLikeRP> call, @NotNull Throwable t) {
                        // Revert on network failure.
                        item.setIs_liked(wasLiked ? "1" : "0");
                        item.setLike_count(String.valueOf(count));
                        if (position >= 0) adapter.refreshLike(position);
                    }
                });
    }

    @Override
    public void onComment(MediaItem item, int position) {
        pendingCommentRefreshPos = position;
        Intent intent = new Intent(this, MediaCommentsActivity.class);
        intent.putExtra(MediaCommentsActivity.EXTRA_POST_ID, item.getPost_id());
        intent.putExtra(MediaCommentsActivity.EXTRA_ALLOW_COMMENTS, item.getAllow_comments());
        startActivity(intent);
    }

    @Override
    public void onShare(MediaItem item) {
        // Use the backend-provided share URL (same scheme as book sharing:
        // https://<app_base>/share/media/{id}). Fall back only if missing.
        String url = item.getShare_url();
        if (url == null || url.trim().isEmpty()) {
            url = Constant.MEDIA_WEB_BASE + "/post/" + item.getPost_id();
        }
        String text = (item.getTitle() != null && !item.getTitle().isEmpty())
                ? item.getTitle() + "\n" + url : url;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, getString(R.string.share_via)));
    }

    @Override
    public void onOpenLink(MediaItem item) {
        String link = item.getLink_url();
        if (link == null || link.trim().isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim())));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.cant_open_link), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onOpenBook(MediaItem item) {
        String bookId = item.getBook_id();
        if (bookId == null || bookId.trim().isEmpty()) return;
        startActivity(new Intent(this, BookDetailsActivity.class)
                .putExtra("BOOK_ID", bookId.trim()));
    }

    private int parse(String s) {
        try { return (s == null || s.isEmpty()) ? 0 : Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) adapter.detachPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && currentCenter >= 0) {
            binding.rvFeed.post(() -> adapter.playPosition(currentCenter));
        }
        // If we came back from the comments screen, refresh that card's comment count.
        if (pendingCommentRefreshPos >= 0 && pendingCommentRefreshPos < feed.size()) {
            adapter.notifyItemChanged(pendingCommentRefreshPos);
            pendingCommentRefreshPos = -1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.releasePlayer();
    }
}

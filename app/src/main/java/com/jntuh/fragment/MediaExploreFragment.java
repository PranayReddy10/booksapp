package com.jntuh.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jntuh.adapter.MediaPostAdapter;
import com.jntuh.books.BookDetailsActivity;
import com.jntuh.books.LoginActivity;
import com.jntuh.books.MediaCommentsActivity;
import com.jntuh.books.MediaFeedActivity;
import com.jntuh.books.MediaNotificationActivity;
import com.jntuh.books.MediaUploadActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.FragmentMediaExploreBinding;
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
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reels tab: a scrolling card feed (one post per card) with the post's own
 * like / comment / share actions inline. Tapping a card's media opens the
 * full-screen player feed focused on that post.
 */
public class MediaExploreFragment extends Fragment implements MediaPostAdapter.Listener {

    private FragmentMediaExploreBinding binding;
    private Method method;

    private final List<MediaItem> items = new ArrayList<>();
    private MediaPostAdapter adapter;
    private LinearLayoutManager layoutManager;

    private int pageIndex = 1;
    private int totalPages = 1;
    private boolean isLoading = false;
    // "" = everything, otherwise the media_type the feed is filtered to.
    private String mediaTypeFilter = "";

    // Position whose comment count may have changed while the comments screen was open.
    private int pendingCommentRefreshPos = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMediaExploreBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        binding.progressExplore.setVisibility(View.GONE);
        binding.llNoData.getRoot().setVisibility(View.GONE);

        layoutManager = new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false);
        binding.rvExplore.setHasFixedSize(false);
        binding.rvExplore.setLayoutManager(layoutManager);

        adapter = new MediaPostAdapter(requireActivity(), items, this);
        binding.rvExplore.setAdapter(adapter);

        binding.swipeExplore.setOnRefreshListener(() -> reload(mediaTypeFilter));

        binding.ivExploreMenu.setOnClickListener(this::showFeedMenu);

        binding.tvChipAll.setOnClickListener(v -> selectFilter(""));
        binding.tvChipPhotos.setOnClickListener(v -> selectFilter("photo"));
        binding.tvChipVideos.setOnClickListener(v -> selectFilter("video"));
        paintChips();

        binding.flBell.setOnClickListener(v -> {
            if (method.getIsLogin()) {
                startActivity(new Intent(requireActivity(), MediaNotificationActivity.class));
            } else {
                startActivity(new Intent(requireActivity(), LoginActivity.class));
            }
        });

        binding.rvExplore.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NotNull androidx.recyclerview.widget.RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;
                int last = layoutManager.findLastVisibleItemPosition();
                if (!isLoading && pageIndex < totalPages && last >= items.size() - 3) {
                    new Handler().postDelayed(() -> {
                        pageIndex++;
                        loadExplore();
                    }, 400);
                }
            }
        });

        if (method.isNetworkAvailable()) {
            loadExplore();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }
        return binding.getRoot();
    }

    // ---- Filters ----

    private void selectFilter(String type) {
        if (type.equals(mediaTypeFilter)) return;
        reload(type);
    }

    /** Restart the feed at page 1, optionally under a different media_type filter. */
    private void reload(String type) {
        mediaTypeFilter = type;
        paintChips();
        pageIndex = 1;
        totalPages = 1;
        int had = items.size();
        items.clear();
        if (had > 0) adapter.notifyItemRangeRemoved(0, had);
        binding.rvExplore.scrollToPosition(0);
        loadExplore();
    }

    private void paintChips() {
        paintChip(binding.tvChipAll, mediaTypeFilter.isEmpty());
        paintChip(binding.tvChipPhotos, "photo".equals(mediaTypeFilter));
        paintChip(binding.tvChipVideos, "video".equals(mediaTypeFilter));
    }

    private void paintChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected
                ? R.drawable.bg_feed_chip_selected : R.drawable.bg_feed_chip_normal);
        chip.setTextColor(ContextCompat.getColor(requireActivity(),
                selected ? R.color.white : R.color.feed_name));
    }

    // ---- Feed loading ----

    private void loadExplore() {
        if (isLoading) return;
        isLoading = true;
        if (items.isEmpty() && !binding.swipeExplore.isRefreshing()) {
            binding.progressExplore.setVisibility(View.VISIBLE);
        }
        binding.llNoData.getRoot().setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        if (method.getIsLogin()) {
            jsObj.addProperty("user_id", method.getUserId());
        }
        if (!mediaTypeFilter.isEmpty()) {
            jsObj.addProperty("media_type", mediaTypeFilter);
        }
        // The filter in force when this page was asked for, so a late response
        // from a previous filter can't be appended to the current list.
        final String requestedFilter = mediaTypeFilter;

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MediaFeedRP> call = apiService.getMediaFeedData(API.toBase64(jsObj.toString()), pageIndex);
        call.enqueue(new Callback<MediaFeedRP>() {
            @Override
            public void onResponse(@NotNull Call<MediaFeedRP> call, @NotNull Response<MediaFeedRP> response) {
                isLoading = false;
                if (getActivity() == null || binding == null) return;
                binding.progressExplore.setVisibility(View.GONE);
                binding.swipeExplore.setRefreshing(false);
                if (!requestedFilter.equals(mediaTypeFilter)) return;
                try {
                    MediaFeedRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess())) {
                        totalPages = Math.max(body.getTotal_pages(), 1);
                        int start = items.size();
                        if (body.getMediaItems() != null) {
                            items.addAll(body.getMediaItems());
                        }
                        if (start == 0) {
                            adapter.notifyDataSetChanged();
                        } else {
                            adapter.notifyItemRangeInserted(start, items.size() - start);
                        }
                    }
                    binding.llNoData.getRoot().setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    binding.llNoData.getRoot().setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(@NotNull Call<MediaFeedRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                isLoading = false;
                if (getActivity() == null || binding == null) return;
                binding.progressExplore.setVisibility(View.GONE);
                binding.swipeExplore.setRefreshing(false);
                binding.llNoData.getRoot().setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    // ---- MediaPostAdapter.Listener ----

    // Open the full-screen vertical feed starting at the tapped card.
    @Override
    public void onOpenPost(int position) {
        MediaFeedHandoff.set(items, position, pageIndex, totalPages, mediaTypeFilter);
        Intent intent = new Intent(requireActivity(), MediaFeedActivity.class);
        intent.putExtra(MediaFeedActivity.EXTRA_FROM_HANDOFF, true);
        startActivity(intent);
    }

    @Override
    public void onLike(MediaItem item, int position) {
        if (!method.getIsLogin()) {
            Toast.makeText(requireActivity(), getString(R.string.login_require), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(requireActivity(), LoginActivity.class));
            return;
        }
        // Optimistic flip, reverted if the server says no.
        boolean wasLiked = "1".equals(item.getIs_liked());
        int count = parse(item.getLike_count());
        item.setIs_liked(wasLiked ? "0" : "1");
        item.setLike_count(String.valueOf(Math.max(0, wasLiked ? count - 1 : count + 1)));
        adapter.refreshLike(position);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("post_id", item.getPost_id());
        ApiClient.getClient().create(ApiInterface.class)
                .getMediaLikeData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<MediaLikeRP>() {
                    @Override
                    public void onResponse(@NotNull Call<MediaLikeRP> call, @NotNull Response<MediaLikeRP> response) {
                        if (getActivity() == null || binding == null) return;
                        MediaLikeRP body = response.body();
                        if (body == null || body.getItemLikeList() == null || body.getItemLikeList().isEmpty()) return;
                        MediaLikeRP.ItemLike r = body.getItemLikeList().get(0);
                        if ("1".equals(r.getSuccess())) {
                            item.setIs_liked(r.getIs_liked());
                            item.setLike_count(r.getLike_count());
                        } else {
                            item.setIs_liked(wasLiked ? "1" : "0");
                            item.setLike_count(String.valueOf(count));
                            if (r.getMsg() != null) {
                                Toast.makeText(getActivity(), r.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        refreshLikeFor(item);
                    }

                    @Override
                    public void onFailure(@NotNull Call<MediaLikeRP> call, @NotNull Throwable t) {
                        if (getActivity() == null || binding == null) return;
                        item.setIs_liked(wasLiked ? "1" : "0");
                        item.setLike_count(String.valueOf(count));
                        refreshLikeFor(item);
                    }
                });
    }

    @Override
    public void onComment(MediaItem item, int position) {
        pendingCommentRefreshPos = position;
        Intent intent = new Intent(requireActivity(), MediaCommentsActivity.class);
        intent.putExtra(MediaCommentsActivity.EXTRA_POST_ID, item.getPost_id());
        intent.putExtra(MediaCommentsActivity.EXTRA_ALLOW_COMMENTS, item.getAllow_comments());
        startActivity(intent);
    }

    @Override
    public void onShare(MediaItem item) {
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
            Toast.makeText(requireActivity(), getString(R.string.cant_open_link), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onOpenBook(MediaItem item) {
        String bookId = item.getBook_id();
        if (bookId == null || bookId.trim().isEmpty()) return;
        startActivity(new Intent(requireActivity(), BookDetailsActivity.class)
                .putExtra("BOOK_ID", bookId.trim()));
    }

    @Override
    public void onMore(MediaItem item, View anchor) {
        showFeedMenu(anchor);
    }

    /** Repaint the like row of whichever card currently holds this item. */
    private void refreshLikeFor(MediaItem item) {
        int idx = items.indexOf(item);
        if (idx >= 0) adapter.refreshLike(idx);
    }

    private int parse(String s) {
        try { return (s == null || s.isEmpty()) ? 0 : Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reflect any unread notifications with the bell dot.
        refreshBellDot();
        // If we came back from the comments screen, refresh that card's comment count.
        if (pendingCommentRefreshPos >= 0 && pendingCommentRefreshPos < items.size()) {
            adapter.notifyItemChanged(pendingCommentRefreshPos);
        }
        pendingCommentRefreshPos = -1;
    }

    private void refreshBellDot() {
        // Lightweight: rely on the notifications screen to clear; show dot if logged in.
        // A precise unread count would need a dedicated endpoint; the dot is a hint.
        binding.viewBellDot.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showFeedMenu(android.view.View anchor) {
        androidx.appcompat.widget.PopupMenu popup =
                new androidx.appcompat.widget.PopupMenu(requireActivity(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_feed, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(requireActivity(), com.jntuh.books.MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } else if (id == R.id.menu_upload_media) {
                requireLoginThen(MediaUploadActivity.class);
            } else if (id == R.id.menu_manage_feed) {
                requireLoginThen(com.jntuh.books.MyMediaActivity.class);
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(requireActivity(), com.jntuh.books.MainActivity.class)
                        .putExtra("openProfile", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } else if (id == R.id.menu_settings) {
                startActivity(new Intent(requireActivity(), com.jntuh.books.SettingsActivity.class));
            }
            return true;
        });
        popup.show();
    }

    private void requireLoginThen(Class<?> target) {
        if (method.getIsLogin()) {
            startActivity(new Intent(requireActivity(), target));
        } else {
            startActivity(new Intent(requireActivity(), LoginActivity.class));
        }
    }
}

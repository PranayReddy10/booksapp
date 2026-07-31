package com.jntuh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.jntuh.adapter.MediaTileAdapter;
import com.jntuh.books.LoginActivity;
import com.jntuh.books.MediaFeedActivity;
import com.jntuh.books.MediaNotificationActivity;
import com.jntuh.books.MediaUploadActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.FragmentMediaExploreBinding;
import com.jntuh.item.MediaItem;
import com.jntuh.response.MediaFeedRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
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

public class MediaExploreFragment extends Fragment {

    private FragmentMediaExploreBinding binding;
    private Method method;

    private final List<MediaItem> items = new ArrayList<>();
    private MediaTileAdapter adapter;
    private StaggeredGridLayoutManager layoutManager;

    private int pageIndex = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMediaExploreBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        binding.progressExplore.setVisibility(View.GONE);
        binding.llNoData.getRoot().setVisibility(View.GONE);

        layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        binding.rvExplore.setHasFixedSize(true);
        binding.rvExplore.setLayoutManager(layoutManager);

        adapter = new MediaTileAdapter(requireActivity(), items, this::openFullFeed);
        binding.rvExplore.setAdapter(adapter);

        binding.ivExploreUpload.setOnClickListener(v -> {
            if (method.getIsLogin()) {
                startActivity(new Intent(requireActivity(), MediaUploadActivity.class));
            } else {
                startActivity(new Intent(requireActivity(), LoginActivity.class));
            }
        });

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
                int[] lastPositions = layoutManager.findLastVisibleItemPositions(null);
                int last = 0;
                for (int p : lastPositions) last = Math.max(last, p);
                if (!isLoading && pageIndex < totalPages && last >= items.size() - 4) {
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

    private void loadExplore() {
        if (isLoading) return;
        isLoading = true;
        if (items.isEmpty()) binding.progressExplore.setVisibility(View.VISIBLE);
        binding.llNoData.getRoot().setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        if (method.getIsLogin()) {
            jsObj.addProperty("user_id", method.getUserId());
        }
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MediaFeedRP> call = apiService.getMediaFeedData(API.toBase64(jsObj.toString()), pageIndex);
        call.enqueue(new Callback<MediaFeedRP>() {
            @Override
            public void onResponse(@NotNull Call<MediaFeedRP> call, @NotNull Response<MediaFeedRP> response) {
                isLoading = false;
                if (getActivity() == null) return;
                binding.progressExplore.setVisibility(View.GONE);
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
                if (getActivity() == null) return;
                binding.progressExplore.setVisibility(View.GONE);
                binding.llNoData.getRoot().setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    // Open the full-screen vertical feed starting at the tapped tile.
    private void openFullFeed(int position) {
        MediaFeedHandoff.set(items, position, pageIndex, totalPages, "");
        Intent intent = new Intent(requireActivity(), MediaFeedActivity.class);
        intent.putExtra(MediaFeedActivity.EXTRA_FROM_HANDOFF, true);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reflect any unread notifications with the bell dot.
        refreshBellDot();
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
}

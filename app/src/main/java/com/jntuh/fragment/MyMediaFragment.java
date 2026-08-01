package com.jntuh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.adapter.MyMediaAdapter;
import com.jntuh.books.MediaFeedActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.FragmentFavoriteBinding;
import com.jntuh.item.MyMediaItem;
import com.jntuh.response.MyMediaRP;
import com.jntuh.response.PostRateRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "My Media" tab on the profile screen: a 2-column grid of the media this user has
 * uploaded (same look as the Uploaded Books tab), with delete and tap-to-open.
 */
public class MyMediaFragment extends Fragment implements MyMediaAdapter.OnDeleteListener {

    FragmentFavoriteBinding viewMedia;
    Method method;
    private final List<MyMediaItem> mediaList = new ArrayList<>();
    private MyMediaAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewMedia = FragmentFavoriteBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        viewMedia.progressFav.setVisibility(View.GONE);
        viewMedia.llNoData.clNoDataFound.setVisibility(View.GONE);
        viewMedia.laySubs.setVisibility(View.GONE);

        viewMedia.rvFav.setHasFixedSize(true);
        viewMedia.rvFav.setLayoutManager(new GridLayoutManager(requireActivity(), 2));
        viewMedia.rvFav.setFocusable(false);

        adapter = new MyMediaAdapter(requireActivity(), mediaList, this);
        viewMedia.rvFav.setAdapter(adapter);

        if (method.isNetworkAvailable()) {
            loadMedia();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }

        return viewMedia.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (method != null && method.isNetworkAvailable() && adapter != null) {
            loadMedia();
        }
    }

    private void loadMedia() {
        if (!isAdded() || getActivity() == null) return;
        viewMedia.progressFav.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MyMediaRP> call = apiService.getMyUploadedMediaData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<MyMediaRP>() {
            @Override
            public void onResponse(@NotNull Call<MyMediaRP> call, @NotNull Response<MyMediaRP> response) {
                if (!isAdded() || getActivity() == null || viewMedia == null) return;
                viewMedia.progressFav.setVisibility(View.GONE);
                try {
                    MyMediaRP body = response.body();
                    mediaList.clear();
                    if (body != null && "1".equals(body.getSuccess()) && body.getMyMediaItems() != null) {
                        mediaList.addAll(body.getMyMediaItems());
                    }
                    adapter.notifyDataSetChanged();
                    boolean empty = mediaList.isEmpty();
                    viewMedia.llNoData.clNoDataFound.setVisibility(empty ? View.VISIBLE : View.GONE);
                    viewMedia.rvFav.setVisibility(empty ? View.GONE : View.VISIBLE);
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<MyMediaRP> call, @NotNull Throwable t) {
                if (!isAdded() || getActivity() == null || viewMedia == null) return;
                Log.e("fail", t.toString());
                viewMedia.progressFav.setVisibility(View.GONE);
                viewMedia.llNoData.clNoDataFound.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDelete(MyMediaItem item, int position) {
        if (!isAdded() || getActivity() == null) return;
        new AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.confirm_delete_media))
                .setPositiveButton(android.R.string.ok, (d, w) -> deleteMedia(item, position))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onOpen(MyMediaItem item, int position) {
        if (!isAdded() || getActivity() == null) return;
        // Only approved posts are in the public feed; pending/rejected can't open.
        String status = item.getUpload_status() == null ? "" : item.getUpload_status().trim().toLowerCase();
        boolean approved = status.equals("approved") || status.equals("1");
        if (!approved) {
            Toast.makeText(requireActivity(), getString(R.string.media_not_live_yet), Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(requireActivity(), MediaFeedActivity.class)
                .putExtra(MediaFeedActivity.EXTRA_POST_ID, item.getPost_id()));
    }

    private void deleteMedia(MyMediaItem item, int position) {
        if (!isAdded() || getActivity() == null) return;
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("post_id", item.getPost_id());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<PostRateRP> call = apiService.getMediaDeleteData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<PostRateRP>() {
            @Override
            public void onResponse(@NotNull Call<PostRateRP> call, @NotNull Response<PostRateRP> response) {
                if (!isAdded() || getActivity() == null) return;
                try {
                    PostRateRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess())) {
                        if (position >= 0 && position < mediaList.size()) {
                            mediaList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, mediaList.size());
                        }
                        viewMedia.llNoData.clNoDataFound.setVisibility(
                                mediaList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        method.alertBox(getString(R.string.wrong));
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<PostRateRP> call, @NotNull Throwable t) {
                if (!isAdded() || getActivity() == null) return;
                Log.e("fail", t.toString());
                method.alertBox(getString(R.string.wrong));
            }
        });
    }
}

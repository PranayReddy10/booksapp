package com.jntuh.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jntuh.adapter.MyUploadAdapter;
import com.jntuh.books.databinding.FragmentFavoriteBinding;
import com.jntuh.item.MyUploadList;
import com.jntuh.response.MyUploadRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.books.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Manage Books" tab on the profile screen: shows the books this user uploaded,
 * with their moderation status (Pending / Approved / Rejected + reason).
 */
public class ManageBooksFragment extends Fragment {

    FragmentFavoriteBinding viewManage;
    Method method;
    private final List<MyUploadList> uploadList = new ArrayList<>();
    private MyUploadAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewManage = FragmentFavoriteBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        viewManage.progressFav.setVisibility(View.GONE);
        viewManage.llNoData.clNoDataFound.setVisibility(View.GONE);
        viewManage.laySubs.setVisibility(View.GONE);

        viewManage.rvFav.setHasFixedSize(true);
        viewManage.rvFav.setLayoutManager(new GridLayoutManager(requireActivity(), 2));
        viewManage.rvFav.setFocusable(false);

        adapter = new MyUploadAdapter(requireActivity(), uploadList);
        viewManage.rvFav.setAdapter(adapter);

        if (method.isNetworkAvailable()) {
            loadUploads();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }

        return viewManage.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when returning (e.g. after a new upload).
        if (method != null && method.isNetworkAvailable() && adapter != null) {
            loadUploads();
        }
    }

    private void loadUploads() {
        if (getActivity() == null) return;
        viewManage.progressFav.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MyUploadRP> call = apiService.getMyUploadedBooksData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<MyUploadRP>() {
            @Override
            public void onResponse(@NotNull Call<MyUploadRP> call, @NotNull Response<MyUploadRP> response) {
                if (getActivity() == null) return;
                viewManage.progressFav.setVisibility(View.GONE);
                try {
                    MyUploadRP body = response.body();
                    uploadList.clear();
                    if (body != null && "1".equals(body.getSuccess()) && body.getMyUploadLists() != null) {
                        uploadList.addAll(body.getMyUploadLists());
                    }
                    adapter.notifyDataSetChanged();
                    if (uploadList.isEmpty()) {
                        viewManage.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                        viewManage.rvFav.setVisibility(View.GONE);
                    } else {
                        viewManage.llNoData.clNoDataFound.setVisibility(View.GONE);
                        viewManage.rvFav.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<MyUploadRP> call, @NotNull Throwable t) {
                if (getActivity() == null) return;
                Log.e("fail", t.toString());
                viewManage.progressFav.setVisibility(View.GONE);
                viewManage.llNoData.clNoDataFound.setVisibility(uploadList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }
}

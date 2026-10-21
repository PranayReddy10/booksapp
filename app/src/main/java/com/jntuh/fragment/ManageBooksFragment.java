package com.jntuh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jntuh.adapter.MyUploadAdapter;
import com.jntuh.books.UploadBookActivity;
import com.jntuh.books.databinding.FragmentManageBooksBinding;
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
 * "Manage Books" on the profile: an upload shortcut, how many uploads sit in
 * each review status, and the books themselves with their status (Pending /
 * Approved / Rejected + reason) and what approved ones have earned.
 */
public class ManageBooksFragment extends Fragment {

    FragmentManageBooksBinding viewManage;
    Method method;
    private final List<MyUploadList> uploadList = new ArrayList<>();
    private MyUploadAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewManage = FragmentManageBooksBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        viewManage.rvManage.setLayoutManager(new LinearLayoutManager(requireActivity()));
        viewManage.rvManage.setFocusable(false);
        adapter = new MyUploadAdapter(requireActivity(), uploadList);
        viewManage.rvManage.setAdapter(adapter);

        viewManage.llMbUpload.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), UploadBookActivity.class)));

        viewManage.swipeManage.setColorSchemeColors(
                androidx.core.content.ContextCompat.getColor(requireActivity(), R.color.authx_coral));
        viewManage.swipeManage.setOnRefreshListener(this::loadUploads);

        if (!method.isNetworkAvailable()) {
            showMessage(R.string.internet_connection, 0);
        }
        // onResume loads the list, including on first open.
        return viewManage.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when returning (e.g. after a new upload).
        if (method != null && method.isNetworkAvailable() && viewManage != null) {
            if (uploadList.isEmpty()) viewManage.progressManage.setVisibility(View.VISIBLE);
            loadUploads();
        }
    }

    private void loadUploads() {
        if (getActivity() == null) return;

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MyUploadRP> call = apiService.getMyUploadedBooksData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<MyUploadRP>() {
            @Override
            public void onResponse(@NotNull Call<MyUploadRP> call, @NotNull Response<MyUploadRP> response) {
                if (getActivity() == null || viewManage == null) return;
                stopLoading();
                try {
                    MyUploadRP body = response.body();
                    uploadList.clear();
                    if (body != null && "1".equals(body.getSuccess()) && body.getMyUploadLists() != null) {
                        uploadList.addAll(body.getMyUploadLists());
                    }
                    adapter.notifyDataSetChanged();
                    if (uploadList.isEmpty()) {
                        showMessage(R.string.lbl_mb_empty_title, R.string.lbl_mb_empty_desc);
                    } else {
                        showList();
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<MyUploadRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                if (getActivity() == null || viewManage == null) return;
                stopLoading();
                if (uploadList.isEmpty()) showMessage(R.string.failed_try_again, 0);
            }
        });
    }

    private void showList() {
        int approved = 0, pending = 0, rejected = 0;
        for (MyUploadList item : uploadList) {
            String st = item.getUpload_status() == null ? "" : item.getUpload_status().trim().toLowerCase();
            if (st.equals("approved") || st.equals("1")) approved++;
            else if (st.equals("rejected") || st.equals("2")) rejected++;
            else pending++;
        }
        viewManage.tvMbApproved.setText(String.valueOf(approved));
        viewManage.tvMbPending.setText(String.valueOf(pending));
        viewManage.tvMbRejected.setText(String.valueOf(rejected));

        viewManage.llMbCounts.setVisibility(View.VISIBLE);
        viewManage.rvManage.setVisibility(View.VISIBLE);
        viewManage.llMbEmpty.setVisibility(View.GONE);
    }

    /** @param descRes 0 for no description line */
    private void showMessage(int titleRes, int descRes) {
        viewManage.llMbCounts.setVisibility(View.GONE);
        viewManage.rvManage.setVisibility(View.GONE);
        viewManage.llMbEmpty.setVisibility(View.VISIBLE);
        viewManage.tvMbEmptyTitle.setText(getString(titleRes));
        viewManage.tvMbEmptyDesc.setVisibility(descRes == 0 ? View.GONE : View.VISIBLE);
        if (descRes != 0) viewManage.tvMbEmptyDesc.setText(getString(descRes));
    }

    private void stopLoading() {
        viewManage.progressManage.setVisibility(View.GONE);
        viewManage.swipeManage.setRefreshing(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewManage = null;
    }
}

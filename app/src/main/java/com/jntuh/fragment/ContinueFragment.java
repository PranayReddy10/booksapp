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

import com.jntuh.adapter.ContinueAdapter;
import com.jntuh.books.BookDetailsActivity;
import com.jntuh.books.MainActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.FragmentContinueBinding;
import com.jntuh.item.SubCatListBook;
import com.jntuh.response.SubCatListBookRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/** Books the reader has opened, newest first, each reopening at its page. */
public class ContinueFragment extends Fragment {

    FragmentContinueBinding viewContinue;
    Method method;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewContinue = FragmentContinueBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        viewContinue.rvContinue.setLayoutManager(new LinearLayoutManager(requireActivity()));
        viewContinue.rvContinue.setFocusable(false);

        viewContinue.swipeContinue.setColorSchemeColors(
                androidx.core.content.ContextCompat.getColor(requireActivity(), R.color.authx_coral));
        viewContinue.swipeContinue.setOnRefreshListener(this::continueData);

        viewContinue.btnContBrowse.setOnClickListener(v -> browseBooks());

        if (method.isNetworkAvailable()) {
            viewContinue.progressContinue.setVisibility(View.VISIBLE);
            continueData();
        } else {
            showEmpty(R.string.internet_connection, 0);
        }

        return viewContinue.getRoot();
    }

    /**
     * Open the Latest tab. On the home screen that is a tab switch; from the
     * profile section page, go back to the home screen with Latest open.
     */
    private void browseBooks() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).highLightNavigation(1);
        } else {
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.putExtra("openLatest", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            requireActivity().finish();
        }
    }

    private void continueData() {
        if (getActivity() == null) return;
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getContinueData(API.toBase64(jsObj.toString())).enqueue(new Callback<SubCatListBookRP>() {
            @Override
            public void onResponse(@NotNull Call<SubCatListBookRP> call, @NotNull Response<SubCatListBookRP> response) {
                if (getActivity() == null || viewContinue == null) return;
                stopLoading();
                try {
                    SubCatListBookRP body = response.body();
                    if (body != null && "1".equals(body.getSuccess())) {
                        List<SubCatListBook> books = body.getSubCatListBooks();
                        if (books != null && !books.isEmpty()) {
                            showBooks(books);
                        } else {
                            showEmpty(R.string.lbl_cont_empty_title, R.string.lbl_cont_empty_desc);
                        }
                    } else {
                        showEmpty(R.string.failed_try_again, 0);
                    }
                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    showEmpty(R.string.failed_try_again, 0);
                }
            }

            @Override
            public void onFailure(@NotNull Call<SubCatListBookRP> call, @NotNull Throwable t) {
                Log.e("fail", t.toString());
                if (getActivity() == null || viewContinue == null) return;
                stopLoading();
                showEmpty(R.string.failed_try_again, 0);
            }
        });
    }

    private void showBooks(List<SubCatListBook> books) {
        viewContinue.llContEmpty.setVisibility(View.GONE);
        viewContinue.llContHeading.setVisibility(View.VISIBLE);
        viewContinue.rvContinue.setVisibility(View.VISIBLE);
        viewContinue.tvContCount.setText(books.size() == 1
                ? getString(R.string.lbl_cont_count_one)
                : getString(R.string.lbl_cont_count, books.size()));

        ContinueAdapter adapter = new ContinueAdapter(requireActivity(), books);
        adapter.setOnItemClickListener(position -> {
            Intent intentDetail = new Intent(requireActivity(), BookDetailsActivity.class);
            intentDetail.putExtra("BOOK_ID", books.get(position).getPost_id());
            startActivity(intentDetail);
        });
        viewContinue.rvContinue.setAdapter(adapter);
    }

    /** @param descRes 0 for no description line */
    private void showEmpty(int titleRes, int descRes) {
        viewContinue.llContHeading.setVisibility(View.GONE);
        viewContinue.rvContinue.setVisibility(View.GONE);
        viewContinue.llContEmpty.setVisibility(View.VISIBLE);
        viewContinue.tvContEmptyTitle.setText(getString(titleRes));
        viewContinue.tvContEmptyDesc.setVisibility(descRes == 0 ? View.GONE : View.VISIBLE);
        if (descRes != 0) viewContinue.tvContEmptyDesc.setText(getString(descRes));
    }

    private void stopLoading() {
        viewContinue.progressContinue.setVisibility(View.GONE);
        viewContinue.swipeContinue.setRefreshing(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewContinue = null;
    }
}

package com.jntuh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jntuh.books.PlanListActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.FragmentDashboardBinding;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DashBoardFragment extends Fragment {

    FragmentDashboardBinding viewDashBoard;
    Method method;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        viewDashBoard = FragmentDashboardBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        viewDashBoard.progressFav.setVisibility(View.GONE);
        viewDashBoard.llNoData.clNoDataFound.setVisibility(View.GONE);

        if (method.isNetworkAvailable()) {
            mySubscriptionData();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }

        viewDashBoard.btnStartSubs.setOnClickListener(v -> {
            Intent intent_subs = new Intent(requireActivity(), PlanListActivity.class);
            startActivity(intent_subs);
        });

        return viewDashBoard.getRoot();
    }

    private void mySubscriptionData() {

        viewDashBoard.progressFav.setVisibility(View.VISIBLE);
        Call<ResponseBody> call;
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        call = apiService.getMySubscriptionData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                if (getActivity() != null) {
                    String responseBody = null;
                    try {
                        responseBody = response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {

                        JSONObject mainJson = new JSONObject(responseBody);
                        assert mainJson != null;

                        JSONObject objJson = mainJson.getJSONObject("EBOOK_APP");
                        String planRes = objJson.getString("success");
                        String planMsg = objJson.getString("msg");
                        if (planRes.equals("1")) {
                            String planName = objJson.getString("current_plan");
                            String planDate = objJson.getString("expired_on");
                            viewDashBoard.laySubs.setVisibility(View.GONE);
                            viewDashBoard.llDash.setVisibility(View.VISIBLE);
                            viewDashBoard.tvPlanName.setText(planName);
                            viewDashBoard.tvPlanDate.setText(planDate);
                        } else {
                            viewDashBoard.laySubs.setVisibility(View.VISIBLE);
                            viewDashBoard.llDash.setVisibility(View.GONE);
                        }

                    } catch (
                            Exception e) {
                        Log.d("exception_error", e.toString());
                    }
                }
                viewDashBoard.progressFav.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                viewDashBoard.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                viewDashBoard.progressFav.setVisibility(View.GONE);
                viewDashBoard.llDash.setVisibility(View.GONE);
            }
        });
    }
}

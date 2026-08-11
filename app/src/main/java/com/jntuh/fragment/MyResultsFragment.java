package com.jntuh.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.adapter.SemesterAdapter;
import com.jntuh.books.EditResultActivity;
import com.jntuh.books.R;
import com.jntuh.books.ReportCardActivity;
import com.jntuh.books.databinding.FragmentMyResultsBinding;
import com.jntuh.item.ResultItem;
import com.jntuh.response.ResultRP;
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
 * "My Results" tab in the profile ViewPager. Owner-only: loads the student's own
 * result via result_get and renders a summary card + expandable semester list.
 * When locked == 1, all edit controls are hidden. The report card is a
 * server-generated PNG opened in {@link ReportCardActivity}.
 */
public class MyResultsFragment extends Fragment {

    private FragmentMyResultsBinding v;
    private Method method;
    private final List<com.jntuh.item.SemesterItem> semesters = new ArrayList<>();
    private SemesterAdapter adapter;
    private ResultItem current;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = FragmentMyResultsBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        v.rvSemesters.setLayoutManager(new LinearLayoutManager(requireActivity()));
        v.rvSemesters.setNestedScrollingEnabled(false);
        v.rvSemesters.setHasFixedSize(false);
        adapter = new SemesterAdapter(requireActivity(), semesters);
        v.rvSemesters.setAdapter(adapter);

        v.swipeResults.setColorSchemeColors(
                androidx.core.content.ContextCompat.getColor(requireActivity(), R.color.app_bg_orange));
        v.swipeResults.setOnRefreshListener(this::loadResult);

        v.btnAddResultEmpty.setOnClickListener(view -> openEditor());
        v.btnEditResult.setOnClickListener(view -> openEditor());
        v.btnViewReport.setOnClickListener(view -> openReport());

        showState(State.LOADING);
        if (method.getIsLogin()) {
            if (method.isNetworkAvailable()) {
                loadResult();
            } else {
                method.alertBox(getString(R.string.internet_connection));
                showState(State.EMPTY);
            }
        } else {
            showState(State.LOGIN);
        }
        return v.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh after returning from the editor so locked/verified stay in sync.
        if (method != null && method.getIsLogin() && method.isNetworkAvailable() && v != null) {
            loadResult();
        }
    }

    private void openEditor() {
        if (current != null && current.getLocked() == 1) {
            // Locked -> never allow edit; just refresh the read-only view.
            loadResult();
            return;
        }
        Intent i = new Intent(requireActivity(), EditResultActivity.class);
        if (current != null && current.getHas_result() == 1) {
            i.putExtra(EditResultActivity.EXTRA_HAS_RESULT, true);
        }
        startActivity(i);
    }

    private void openReport() {
        Intent i = new Intent(requireActivity(), ReportCardActivity.class);
        if (current != null) {
            i.putExtra(ReportCardActivity.EXTRA_IMAGE_URL, current.getReport_image_url());
            i.putExtra(ReportCardActivity.EXTRA_SHARE_URL, current.getShare_url());
        }
        startActivity(i);
    }

    private void loadResult() {
        if (getActivity() == null) return;
        if (!v.swipeResults.isRefreshing()) showState(State.LOADING);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<ResultRP> call = api.getResult(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<ResultRP>() {
            @Override
            public void onResponse(@NotNull Call<ResultRP> call, @NotNull Response<ResultRP> resp) {
                if (getActivity() == null) return;
                v.swipeResults.setRefreshing(false);
                try {
                    ResultRP body = resp.body();
                    if (body != null && "1".equals(body.getSuccess())
                            && body.getResultItems() != null && !body.getResultItems().isEmpty()) {
                        ResultItem item = body.getResultItems().get(0);
                        current = item;
                        if ("1".equals(item.getSuccess()) && item.getHas_result() == 1) {
                            render(item);
                        } else {
                            showState(State.EMPTY);
                        }
                    } else {
                        showState(State.EMPTY);
                    }
                } catch (Exception e) {
                    Log.d("result_error", e.toString());
                    showState(State.EMPTY);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResultRP> call, @NotNull Throwable t) {
                if (getActivity() == null) return;
                v.swipeResults.setRefreshing(false);
                Log.e("result_fail", t.toString());
                showState(current != null && current.getHas_result() == 1 ? State.CONTENT : State.EMPTY);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void render(ResultItem item) {
        boolean locked = item.getLocked() == 1;

        v.tvStudentName.setText(nn(item.getStudent_name(), getString(R.string.result_student)));
        v.tvHallTicket.setText(nn(item.getHall_ticket_no(), ""));

        StringBuilder br = new StringBuilder();
        if (item.getBranch() != null && !item.getBranch().trim().isEmpty()) br.append(item.getBranch().trim());
        if (item.getRegulation() != null && !item.getRegulation().trim().isEmpty()) {
            if (br.length() > 0) br.append("  ·  ");
            br.append(item.getRegulation().trim());
        }
        v.tvBranchReg.setText(br.toString());

        v.tvCgpa.setText(nn(item.getCurrent_cgpa(), "—"));
        v.tvTotalCredits.setText(nn(item.getTotal_credits(), "—"));
        v.tvBacklogs.setText(nn(item.getBacklogs_count(), "0"));

        v.pillVerified.setVisibility(item.getVerified() == 1 ? View.VISIBLE : View.GONE);
        v.bannerLocked.setVisibility(locked ? View.VISIBLE : View.GONE);
        // Edit controls hidden when locked (single source of truth = locked).
        v.btnEditResult.setVisibility(locked ? View.GONE : View.VISIBLE);

        semesters.clear();
        if (item.getSemesters() != null) semesters.addAll(item.getSemesters());
        adapter = new SemesterAdapter(requireActivity(), semesters);
        v.rvSemesters.setAdapter(adapter);

        LayoutAnimationController anim = AnimationUtils.loadLayoutAnimation(
                requireActivity(), R.anim.layout_animation_fall_down);
        v.rvSemesters.setLayoutAnimation(anim);
        v.rvSemesters.scheduleLayoutAnimation();

        showState(State.CONTENT);
    }

    private enum State { LOADING, CONTENT, EMPTY, LOGIN }

    private void showState(State s) {
        if (v == null) return;
        v.progressResults.setVisibility(s == State.LOADING ? View.VISIBLE : View.GONE);
        v.llResultContent.setVisibility(s == State.CONTENT ? View.VISIBLE : View.GONE);
        v.llResultEmpty.setVisibility(s == State.EMPTY ? View.VISIBLE : View.GONE);
        v.llResultLogin.setVisibility(s == State.LOGIN ? View.VISIBLE : View.GONE);
    }

    private static String nn(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s.trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        v = null;
    }
}

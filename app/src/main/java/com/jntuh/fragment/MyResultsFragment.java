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

    /** Auto-fetch polling: the upstream queues a scrape on the first request. */
    private static final int MAX_FETCH_ATTEMPTS = 4;
    private static final long FETCH_RETRY_MS = 6000L;
    private final android.os.Handler retry = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean fetching;

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

        // Auto-fetch. result_get hands back the profile's hall ticket even when
        // there is no result yet, so this normally needs no typing at all.
        v.btnFetchResultAuto.setOnClickListener(view -> {
            String known = current != null ? current.getHall_ticket_no() : null;
            if (known != null && known.trim().length() == 10) {
                startFetch(known, 1);
            } else {
                askHallTicket();
            }
        });
        v.btnSyncResult.setOnClickListener(view ->
                startFetch(current != null ? current.getHall_ticket_no() : null, 1));

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
        // Skipped mid-fetch, which would otherwise wipe the progress message.
        if (!fetching && method != null && method.getIsLogin() && method.isNetworkAvailable() && v != null) {
            loadResult();
        }
    }

    private void openEditor() {
        // Always open the editor. Locked (verified) semesters render read-only
        // inside it, but the student can still add NEW semesters.
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

    /**
     * The hall ticket is all the university feed needs. The server still prefers
     * the roll number already on the profile, so what is typed here only matters
     * the first time.
     */
    private void askHallTicket() {
        if (getActivity() == null) return;
        if (!method.getIsLogin()) { showState(State.LOGIN); return; }
        if (!method.isNetworkAvailable()) { method.alertBox(getString(R.string.internet_connection)); return; }

        final android.widget.EditText input = new android.widget.EditText(requireActivity());
        input.setHint(R.string.result_fetch_hall_hint);
        input.setSingleLine(true);
        input.setAllCaps(true);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(10)});
        if (current != null && current.getHall_ticket_no() != null) {
            input.setText(current.getHall_ticket_no().trim());
        }

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout wrap = new android.widget.FrameLayout(requireActivity());
        wrap.setPadding(pad, pad / 2, pad, 0);
        wrap.addView(input);

        new androidx.appcompat.app.AlertDialog.Builder(requireActivity())
                .setTitle(R.string.result_fetch_hall_title)
                .setMessage(R.string.result_fetch_hall_desc)
                .setView(wrap)
                .setPositiveButton(R.string.result_fetch_auto, (d, w) ->
                        startFetch(input.getText().toString(), 1))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * One round of result_fetch. The upstream scrapes the university site on a
     * cache miss and answers "queued" meanwhile, so a queued reply is retried a
     * few times before we send the student to the manual editor.
     */
    private void startFetch(String hallTicket, int attempt) {
        if (getActivity() == null || v == null) return;

        String hall = hallTicket == null ? "" : hallTicket.trim().toUpperCase(java.util.Locale.US);
        if (hall.length() != 10) {
            method.alertBox(getString(R.string.result_fetch_invalid));
            return;
        }
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }

        fetching = true;
        setFetchStatus(getString(attempt == 1 ? R.string.result_fetch_working : R.string.result_fetch_queued));

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("hall_ticket_no", hall);

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<com.jntuh.response.ResultSaveRP> call = api.fetchResult(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<com.jntuh.response.ResultSaveRP>() {
            @Override
            public void onResponse(@NotNull Call<com.jntuh.response.ResultSaveRP> call,
                                   @NotNull Response<com.jntuh.response.ResultSaveRP> resp) {
                if (getActivity() == null || v == null) return;

                com.jntuh.item.SimpleMsg msg = null;
                com.jntuh.response.ResultSaveRP body = resp.body();
                if (body != null && body.getEbookApp() != null && !body.getEbookApp().isEmpty()) {
                    msg = body.getEbookApp().get(0);
                }
                if (msg == null) {
                    finishFetch(getString(R.string.result_fetch_failed));
                    return;
                }

                String state = msg.getState() == null ? "" : msg.getState();
                if ("queued".equals(state)) {
                    if (attempt < MAX_FETCH_ATTEMPTS) {
                        setFetchStatus(getString(R.string.result_fetch_queued));
                        retry.postDelayed(() -> startFetch(hall, attempt + 1), FETCH_RETRY_MS);
                    } else {
                        finishFetch(getString(R.string.result_fetch_slow));
                    }
                    return;
                }

                if ("ready".equals(state)) {
                    fetching = false;
                    setFetchStatus(null);
                    method.alertBox(nn(msg.getMsg(), getString(R.string.result_fetch_done)));
                    loadResult();
                    return;
                }

                finishFetch(nn(msg.getMsg(), getString(R.string.result_fetch_failed)));
            }

            @Override
            public void onFailure(@NotNull Call<com.jntuh.response.ResultSaveRP> call, @NotNull Throwable t) {
                if (getActivity() == null || v == null) return;
                Log.e("result_fetch_fail", t.toString());
                finishFetch(getString(R.string.result_fetch_failed));
            }
        });
    }

    private void finishFetch(String message) {
        fetching = false;
        setFetchStatus(message);
        // The manual editor stays one tap away underneath.
    }

    private void setFetchStatus(String text) {
        if (v == null) return;
        if (text == null || text.trim().isEmpty()) {
            v.tvFetchStatus.setVisibility(View.GONE);
        } else {
            v.tvFetchStatus.setText(text);
            v.tvFetchStatus.setVisibility(View.VISIBLE);
        }
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
        // Keep the edit entry point available even when everything is locked,
        // because the student can still ADD a new (unlocked) semester.
        v.btnEditResult.setVisibility(View.VISIBLE);

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
        retry.removeCallbacksAndMessages(null);
        fetching = false;
        v = null;
    }
}

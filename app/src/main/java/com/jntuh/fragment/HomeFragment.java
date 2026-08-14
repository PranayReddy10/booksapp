package com.jntuh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.adapter.ContinueHomeAdapter;
import com.jntuh.adapter.HomeSectionAdapter;
import com.jntuh.adapter.SliderAdapter;
import com.jntuh.adapter.TrendingHomeAdapter;
import com.jntuh.books.BookDetailsActivity;
import com.jntuh.books.DownloadActivity;
import com.jntuh.books.LoginActivity;
import com.jntuh.books.MainActivity;
import com.jntuh.books.MyUploadsActivity;
import com.jntuh.books.BookListBySubCatActivity;
import com.jntuh.books.R;
import com.jntuh.books.SearchBookActivity;
import com.jntuh.books.SettingsActivity;
import com.jntuh.books.TrendingBookActivity;
import com.jntuh.books.UploadBookActivity;
import com.jntuh.books.databinding.FragmentHomeBinding;
import com.jntuh.response.HomeRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.jntuh.util.OnClick;
import com.jntuh.util.StatusBarUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HomeFragment extends Fragment {

    FragmentHomeBinding viewHome;
    Method method;
    private Timer timer;
    final long DELAY_MS = 2000;//delay in milliseconds before task is to be executed
    final long PERIOD_MS = 3000;
    Boolean isTimerStart = false;
    private final Handler handler = new Handler();
    private Runnable Update;
    OnClick onClickPos;
    SliderAdapter sliderAdapter;
    HomeSectionAdapter homeSectionAdapter;
    ContinueHomeAdapter continueHomeAdapter;
    TrendingHomeAdapter trendingHomeAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        StatusBarUtil.setStatusBar(requireActivity(), "");

        viewHome = FragmentHomeBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        viewHome.progressHome.setVisibility(View.GONE);
        viewHome.llHomeMain.setVisibility(View.GONE);
        viewHome.llNoData.clNoDataFound.setVisibility(View.GONE);

        viewHome.vpFeatureHome.useScale();
        viewHome.vpFeatureHome.removeAlpha();

        viewHome.rvContBook.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManagerContinue = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        viewHome.rvContBook.setLayoutManager(layoutManagerContinue);
        viewHome.rvContBook.setFocusable(false);

        viewHome.rvHomeSec.setHasFixedSize(true);
        viewHome.rvHomeSec.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        viewHome.rvHomeSec.setFocusable(false);
        viewHome.rvHomeSec.setNestedScrollingEnabled(false);

        viewHome.rvHomeTrendingBook.setHasFixedSize(true);
        viewHome.rvHomeTrendingBook.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        viewHome.rvHomeTrendingBook.setFocusable(false);
        viewHome.rvHomeTrendingBook.setNestedScrollingEnabled(false);

        viewHome.rvHomeLatestBook.setHasFixedSize(true);
        viewHome.rvHomeLatestBook.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        viewHome.rvHomeLatestBook.setFocusable(false);
        viewHome.rvHomeLatestBook.setNestedScrollingEnabled(false);

        viewHome.rvHomeFeed.setHasFixedSize(true);
        viewHome.rvHomeFeed.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        viewHome.rvHomeFeed.setFocusable(false);
        viewHome.rvHomeFeed.setNestedScrollingEnabled(false);

        if (method.isNetworkAvailable()) {
            if (method.getIsLogin()) {
                homeData(method.getUserId());
            } else {
                homeData("0");
            }
        } else {
            method.alertBox(getResources().getString(R.string.internet_connection));
            viewHome.progressHome.setVisibility(View.GONE);
        }

        viewHome.ibSetting.setOnClickListener(v -> {
            Intent intentSetting = new Intent(requireActivity(), SettingsActivity.class);
            startActivity(intentSetting);
        });

        bindGreeting();
        bindQuickAccess();
        bindSectionLinks();

        viewHome.ibWhatsapp.setOnClickListener(v -> openLink("https://www.whatsapp.com/channel/0029Vb5uthA7NoZyvanZPr1J"));
        viewHome.ibInstagram.setOnClickListener(v -> openLink("https://www.instagram.com/jntu_books_updates/"));

        viewHome.imageHomeSearch.setOnClickListener(v -> {
            if (viewHome.edtHomeSearch.getText().toString().isEmpty()) {
                Toast.makeText(requireActivity(), getString(R.string.search_msg), Toast.LENGTH_SHORT).show();
            } else {
                Intent intentSearch = new Intent(requireActivity(), BookListBySubCatActivity.class);
                intentSearch.putExtra("postSubCatName", viewHome.edtHomeSearch.getText().toString());
                intentSearch.putExtra("type", "SearchHome");
                startActivity(intentSearch);
                viewHome.edtHomeSearch.setText("");
                viewHome.edtHomeSearch.clearFocus();
            }
        });

        viewHome.edtHomeSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (viewHome.edtHomeSearch.getText().toString().isEmpty()) {
                    Toast.makeText(requireActivity(), getString(R.string.search_msg), Toast.LENGTH_SHORT).show();
                } else {
                    Intent intentSearch = new Intent(requireActivity(), BookListBySubCatActivity.class);
                    intentSearch.putExtra("postSubCatName", viewHome.edtHomeSearch.getText().toString());
                    intentSearch.putExtra("type", "SearchHome");
                    startActivity(intentSearch);
                    viewHome.edtHomeSearch.setText("");
                    viewHome.edtHomeSearch.clearFocus();
                }
            }
            return false;
        });

        return viewHome.getRoot();
    }

    private void homeData(String userId) {

        if (getActivity() != null) {

            viewHome.progressHome.setVisibility(View.VISIBLE);

            JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(getActivity()));
            jsObj.addProperty("user_id", userId);
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
            Call<HomeRP> call = apiService.getHomeData(API.toBase64(jsObj.toString()));
            call.enqueue(new Callback<HomeRP>() {
                @Override
                public void onResponse(@NotNull Call<HomeRP> call, @NotNull Response<HomeRP> response) {

                    if (getActivity() != null) {

                        try {

                            HomeRP homeRP = response.body();

                            if (homeRP != null && homeRP.getSuccess() == 1) {

                                if (!homeRP.getEbookApp().getFeaturedBooks().isEmpty()) {
                                    Update = () -> {
                                        isTimerStart = true;
                                        if (viewHome.vpFeatureHome.getCurrentItem() == (sliderAdapter.getCount() - 1)) {
                                            viewHome.vpFeatureHome.setCurrentItem(0, true);
                                        } else {
                                            viewHome.vpFeatureHome.setCurrentItem(viewHome.vpFeatureHome.getCurrentItem() + 1, true);
                                        }
                                    };

                                    sliderAdapter = new SliderAdapter(getActivity(), homeRP.getEbookApp().getFeaturedBooks());
                                    viewHome.vpFeatureHome.setAdapter(sliderAdapter);
                                    viewHome.vpFeatureHome.setCurrentItem(1);
                                    sliderAdapter.setOnItemClickListener(position -> {
                                        Intent intentDetail = new Intent(requireActivity(), BookDetailsActivity.class);
                                        intentDetail.putExtra("BOOK_ID", homeRP.getEbookApp().getFeaturedBooks().get(position).getPostId());
                                        startActivity(intentDetail);
                                    });

                                    if (sliderAdapter.getCount() > 1) {
                                        timer = new Timer(); // This will create a new Thread
                                        timer.schedule(new TimerTask() { // task to be scheduled
                                            @Override
                                            public void run() {
                                                handler.post(Update);
                                            }
                                        }, DELAY_MS, PERIOD_MS);
                                    }

                                } else {
                                    viewHome.tvHomeFeature.setVisibility(View.GONE);
                                    viewHome.llFeature.setVisibility(View.GONE);
                                }
                                if (homeRP.getEbookApp().getHomeSections() != null) {
                                    homeSectionAdapter = new HomeSectionAdapter(getActivity(), homeRP.getEbookApp().getHomeSections());
                                    viewHome.rvHomeSec.setAdapter(homeSectionAdapter);
                                }

                                if (homeRP.getEbookApp().getContinue_reading().size() != 0) {
                                    continueHomeAdapter = new ContinueHomeAdapter(getActivity(), homeRP.getEbookApp().getContinue_reading());
                                    viewHome.rvContBook.setAdapter(continueHomeAdapter);
                                    continueHomeAdapter.setOnItemClickListener(position -> {
                                        Intent intentDetail = new Intent(requireActivity(), BookDetailsActivity.class);
                                        intentDetail.putExtra("BOOK_ID", homeRP.getEbookApp().getContinue_reading().get(position).getPostId());
                                        intentDetail.putExtra("LAST_POS", "continuePos");
                                        intentDetail.putExtra("PAGE_NUM", homeRP.getEbookApp().getContinue_reading().get(position).getPage_num());
                                        startActivity(intentDetail);
                                    });

                                } else {
                                    viewHome.rlConSection.setVisibility(View.GONE);
                                    viewHome.rvContBook.setVisibility(View.GONE);
                                }

                                if (homeRP.getEbookApp().getTrending_books().size() != 0) {
                                    trendingHomeAdapter = new TrendingHomeAdapter(getActivity(), homeRP.getEbookApp().getTrending_books());
                                    viewHome.rvHomeTrendingBook.setAdapter(trendingHomeAdapter);
                                    trendingHomeAdapter.setOnItemClickListener(position -> {
                                        Intent intentDetail = new Intent(requireActivity(), BookDetailsActivity.class);
                                        intentDetail.putExtra("BOOK_ID", homeRP.getEbookApp().getTrending_books().get(position).getPostId());
                                        startActivity(intentDetail);
                                    });
                                } else {
                                    viewHome.rlTrendSection.setVisibility(View.GONE);
                                    viewHome.rvHomeTrendingBook.setVisibility(View.GONE);
                                }

                                // Latest Books (same design as Trending)
                                if (homeRP.getEbookApp().getLatest_books() != null
                                        && homeRP.getEbookApp().getLatest_books().size() != 0) {
                                    TrendingHomeAdapter latestAdapter =
                                            new TrendingHomeAdapter(getActivity(), homeRP.getEbookApp().getLatest_books());
                                    viewHome.rvHomeLatestBook.setAdapter(latestAdapter);
                                    latestAdapter.setOnItemClickListener(position -> {
                                        Intent d = new Intent(requireActivity(), BookDetailsActivity.class);
                                        d.putExtra("BOOK_ID", homeRP.getEbookApp().getLatest_books().get(position).getPostId());
                                        startActivity(d);
                                    });
                                } else {
                                    viewHome.rlLatestHeader.setVisibility(View.GONE);
                                    viewHome.rvHomeLatestBook.setVisibility(View.GONE);
                                }

                                // Feed (horizontal thumbnails → full-screen viewer)
                                if (homeRP.getEbookApp().getFeed_posts() != null
                                        && homeRP.getEbookApp().getFeed_posts().size() != 0) {
                                    com.jntuh.adapter.HomeFeedAdapter feedAdapter =
                                            new com.jntuh.adapter.HomeFeedAdapter(getActivity(), homeRP.getEbookApp().getFeed_posts());
                                    viewHome.rvHomeFeed.setAdapter(feedAdapter);
                                } else {
                                    viewHome.rlFeedHeader.setVisibility(View.GONE);
                                    viewHome.rvHomeFeed.setVisibility(View.GONE);
                                }

                                viewHome.llHomeMain.setVisibility(View.VISIBLE);

                            } else {
                                viewHome.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                                method.alertBox(getResources().getString(R.string.failed_try_again));
                            }

                        } catch (Exception e) {
                            Log.d("exception_error", e.toString());
                            method.alertBox(getResources().getString(R.string.failed_try_again));
                        }
                    }
                    viewHome.progressHome.setVisibility(View.GONE);

                }

                @Override
                public void onFailure(@NotNull Call<HomeRP> call, @NotNull Throwable t) {
                    // Log error here since request failed
                    Log.e("fail", t.toString());
                    viewHome.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                    viewHome.progressHome.setVisibility(View.GONE);
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }
            });
        }
    }

    /** Time-of-day greeting, personalised once the user is logged in. */
    private void bindGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = getString(hour < 12 ? R.string.lbl_greet_morning
                : hour < 17 ? R.string.lbl_greet_afternoon
                : R.string.lbl_greet_evening);

        String name = method.getIsLogin() ? method.getUserName() : null;
        viewHome.tvHomeGreeting.setText(
                (name != null && !name.trim().isEmpty())
                        ? getString(R.string.lbl_greet_named, greeting, name.trim())
                        : greeting);

        String image = method.getIsLogin() ? method.getUserImage() : null;
        if (image != null && !image.trim().isEmpty()) {
            Glide.with(requireActivity().getApplicationContext())
                    .load(image)
                    .placeholder(R.drawable.img_user)
                    .into(viewHome.ivHomeAvatar);
        }
        viewHome.cvHomeAvatar.setOnClickListener(v -> openProfileTab(0));
    }

    /**
     * The quick-access grid: one tap from home to every destination the app has,
     * including the ones that used to sit behind the profile screen's overflow menu.
     */
    private void bindQuickAccess() {
        // Row 1 — browse the catalogue.
        viewHome.llQaCategories.setOnClickListener(v -> openNavTab(2));
        viewHome.llQaLatest.setOnClickListener(v -> openNavTab(1));
        viewHome.llQaTrending.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), TrendingBookActivity.class)));
        viewHome.llQaReels.setOnClickListener(v -> openNavTab(4));

        // Row 2 — the student's own library.
        viewHome.llQaShop.setOnClickListener(v -> openNavTab(3));
        viewHome.llQaResults.setOnClickListener(v -> openProfileTab(0));
        viewHome.llQaDownloads.setOnClickListener(v -> openLibrary("isDown"));
        viewHome.llQaSaved.setOnClickListener(v -> openLibrary("isFav"));

        // Row 3 — contributing and paying, all login-gated where it matters.
        viewHome.llQaUpload.setOnClickListener(v -> requireLoginThen(UploadBookActivity.class));
        viewHome.llQaMyBooks.setOnClickListener(v -> requireLoginThen(MyUploadsActivity.class));
        viewHome.llQaPlans.setOnClickListener(v -> openProfileTab(3));
        viewHome.llQaRent.setOnClickListener(v -> openProfileTab(4));

        // Filter button beside the search field opens the dedicated search screen.
        viewHome.ivHomeFilter.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), SearchBookActivity.class)));
    }

    /** "See all" on each rail, pointing at the full screen for that section. */
    private void bindSectionLinks() {
        // Continue reading lives on the profile's "Continue Book" tab.
        viewHome.tvSeeAllContinue.setOnClickListener(v -> {
            if (onClickPos != null) onClickPos.position(1);
        });
        viewHome.tvSeeAllTrending.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), TrendingBookActivity.class)));
        viewHome.tvSeeAllLatest.setOnClickListener(v -> openNavTab(1));
        viewHome.tvSeeAllFeed.setOnClickListener(v -> openNavTab(4));
    }

    /** Switch the host's bottom navigation, reusing its own tab wiring. */
    private void openNavTab(int position) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openNavTab(position);
        }
    }

    /** Open the profile screen focused on one of its tabs (results, subscription, rent…). */
    private void openProfileTab(int tabPosition) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openProfileTab(tabPosition);
        }
    }

    /** Downloads ("isDown") and favourites ("isFav") share one screen. */
    private void openLibrary(String mode) {
        startActivity(new Intent(requireActivity(), DownloadActivity.class)
                .putExtra("isDown", mode));
    }

    private void requireLoginThen(Class<?> target) {
        if (method.getIsLogin()) {
            startActivity(new Intent(requireActivity(), target));
        } else {
            Toast.makeText(requireActivity(), getString(R.string.login_require), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(requireActivity(), LoginActivity.class));
        }
    }

    public void setOnItemClickListener(OnClick clickListener) {
        onClickPos = clickListener;
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception e) {
            Log.d("exception_error", e.toString());
        }
    }
}

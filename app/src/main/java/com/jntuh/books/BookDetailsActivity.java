package com.jntuh.books;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jntuh.adapter.RelatedAdapter;
import com.jntuh.books.databinding.ActivityBookDetailBinding;
import com.jntuh.fragment.ReportBookFragment;
import com.jntuh.fragment.WriteRateReviewFragment;
import com.jntuh.item.BookDetailList;
import com.jntuh.response.BookDetailRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.BannerAds;
import com.jntuh.util.Constant;
import com.jntuh.util.AdRewardedAds;
import com.jntuh.util.CoverHelper;
import com.jntuh.util.Events;
import com.jntuh.util.FavouriteIF;
import com.jntuh.util.GlobalBus;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class BookDetailsActivity extends AppCompatActivity {

    ActivityBookDetailBinding viewBookDetail;
    Method method;
    String postBookId;
    RelatedAdapter relatedAdapter;
    BookDetailList bookDetailListPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBookDetail = ActivityBookDetailBinding.inflate(getLayoutInflater());
        setContentView(viewBookDetail.getRoot());
        GlobalBus.getBus().register(this);
        method = new Method(BookDetailsActivity.this);
        method.forceRTLIfSupported();

        Intent intent = getIntent();
        postBookId = intent.getStringExtra("BOOK_ID");

        viewBookDetail.toolbarMain.tvToolbarTitle.setText(getString(R.string.book_detail_title));
        viewBookDetail.toolbarMain.imageFilter.setVisibility(View.VISIBLE);
        viewBookDetail.toolbarMain.imageFilter.setImageResource(R.drawable.img_share);
        viewBookDetail.toolbarMain.imageFilter.setOnClickListener(v -> {
            if (bookDetailListPos != null && bookDetailListPos.getPost_id() != null) {
                // Match the public website's share URL: read.jntubooks.in/book/{id}-{slug}
                String slug = com.jntuh.util.Constant.webSlug(bookDetailListPos.getPost_title());
                String shareUrl = com.jntuh.util.Constant.MEDIA_WEB_BASE
                        + "/book/" + bookDetailListPos.getPost_id()
                        + (slug.isEmpty() ? "" : "-" + slug);
                Intent intent1 = new Intent(Intent.ACTION_SEND);
                intent1.setType("text/plain");
                intent1.putExtra(Intent.EXTRA_TEXT, shareUrl);
                startActivity(Intent.createChooser(intent1, "Choose one"));
            } else {
                method.alertBox(getResources().getString(R.string.wrong));
            }
        });

        viewBookDetail.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        viewBookDetail.ivRateList.setOnClickListener(v -> {
            Intent intentRateList = new Intent(BookDetailsActivity.this, RateReviewActivity.class);
            startActivity(intentRateList);
        });

        viewBookDetail.progressHome.setVisibility(View.GONE);
        viewBookDetail.llNoData.clNoDataFound.setVisibility(View.GONE);
        viewBookDetail.nsView.setVisibility(View.GONE);
        viewBookDetail.btnBuyBook.setVisibility(View.GONE);

        viewBookDetail.rvRelatedBook.setHasFixedSize(true);
        viewBookDetail.rvRelatedBook.setLayoutManager(new LinearLayoutManager(BookDetailsActivity.this, LinearLayoutManager.HORIZONTAL, false));
        viewBookDetail.rvRelatedBook.setFocusable(false);
        viewBookDetail.rvRelatedBook.setNestedScrollingEnabled(false);

        if (method.isNetworkAvailable()) {
            bookDetailData();
        } else {
            method.alertBox(getResources().getString(R.string.internet_connection));
        }

        BannerAds.showBannerAds(BookDetailsActivity.this, viewBookDetail.layoutAds);
    }

    private void bookDetailData() {

        viewBookDetail.progressHome.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(BookDetailsActivity.this));
        jsObj.addProperty("book_id", postBookId);
        jsObj.addProperty("user_id", method.getIsLogin() ? method.getUserId() : "");
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<BookDetailRP> call = apiService.getBookDetailData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<BookDetailRP>() {
            @Override
            public void onResponse(@NotNull Call<BookDetailRP> call, @NotNull Response<BookDetailRP> response) {
                try {

                    BookDetailRP bookDetailRP = response.body();

                    if (bookDetailRP != null && bookDetailRP.getSuccess().equals("1")) {

                        if (bookDetailRP.getBookDetailLists().size() != 0) {
                            viewBookDetail.nsView.setVisibility(View.VISIBLE);
                            viewBookDetail.btnBuyBook.setVisibility(View.VISIBLE);
                            bookDetailListPos = bookDetailRP.getBookDetailLists().get(0);
                            bookViewData(postBookId);

                            // Video (YouTube): show Play, hide Download; plays in-app.
                            if (bookDetailListPos.isVideo()) {
                                viewBookDetail.llDownload.setVisibility(View.GONE);
                                viewBookDetail.ivReadBook.setImageResource(R.drawable.ic_play_badge);
                                viewBookDetail.tvReadBook.setText(R.string.lbl_play_video);
                            }
                            viewBookDetail.tvBookName.setText(bookDetailListPos.getPost_title());
                            viewBookDetail.tvByAuthor.setSelected(true);
                            StringBuilder strAuthor = new StringBuilder();
                            if (!bookDetailListPos.getListBookDetailAuthor().isEmpty()) {
                                String prefix = "";
                                for (int k = 0; k < bookDetailListPos.getListBookDetailAuthor().size(); k++) {
                                    strAuthor.append(prefix);
                                    prefix = ", ";
                                    strAuthor.append(bookDetailListPos.getListBookDetailAuthor().get(k).getAuthor_name());
                                }
                            }
                            viewBookDetail.tvByAuthor.setText(getString(R.string.by_author, strAuthor.toString()));
                            viewBookDetail.tvBookView.setText(Method.Format(Integer.parseInt(bookDetailListPos.getTotal_views())));

                            if (bookDetailListPos.getBook_on_rent().equals("1")) {
                                viewBookDetail.llPremium.setVisibility(View.VISIBLE);
                                viewBookDetail.tvPremium.setText(getString(R.string.rent));
                                viewBookDetail.imageLock.setImageResource(R.drawable.ic_rent);
                                viewBookDetail.tvBookPrice.setText(getString(R.string.currency_code, Constant.constantCurrency, bookDetailListPos.getBook_rent_price()));
                                viewBookDetail.tvBookPrice.setBackgroundResource(R.drawable.paid_detail_box);
                                viewBookDetail.tvBookPrice.setTextColor(ContextCompat.getColor(BookDetailsActivity.this, R.color.white));
                            } else if (bookDetailListPos.getPost_access().equals("Paid")) {
                                viewBookDetail.llPremium.setVisibility(View.VISIBLE);
                                viewBookDetail.tvPremium.setText(getString(R.string.lbl_premium));
                                viewBookDetail.imageLock.setImageResource(R.drawable.img_premium_lock);
                                viewBookDetail.tvBookPrice.setText(getString(R.string.lbl_paid));
                                viewBookDetail.tvBookPrice.setBackgroundResource(R.drawable.paid_detail_box);
                                viewBookDetail.tvBookPrice.setTextColor(ContextCompat.getColor(BookDetailsActivity.this, R.color.white));
                            } else {
                                viewBookDetail.llPremium.setVisibility(View.GONE);
                                viewBookDetail.tvBookPrice.setText(getString(R.string.lbl_free));
                                viewBookDetail.tvBookPrice.setBackgroundResource(R.drawable.free_box);
                                viewBookDetail.tvBookPrice.setTextColor(ContextCompat.getColor(BookDetailsActivity.this, R.color.free_box_text));
                            }

                            viewBookDetail.ivBook.post(() ->
                                    CoverHelper.bind(viewBookDetail.ivBook,
                                            bookDetailListPos.getPost_image(),
                                            bookDetailListPos.getPost_title(),
                                            bookDetailListPos.getCover_color()));

                            WebSettings webSettings = viewBookDetail.wvBookDesc.getSettings();
                            webSettings.setJavaScriptEnabled(true);
                            webSettings.setPluginState(WebSettings.PluginState.ON);
                            viewBookDetail.wvBookDesc.setBackgroundColor(Color.TRANSPARENT);
                            viewBookDetail.wvBookDesc.setFocusableInTouchMode(false);
                            viewBookDetail.wvBookDesc.setFocusable(false);

                            viewBookDetail.wvBookDesc.getSettings().setDefaultTextEncodingName("UTF-8");
                            String mimeType = "text/html";
                            String encoding = "utf-8";

                            String text = "<html dir=" + method.isWebViewTextRtl() + "><head>"
                                    + "<style type=\"text/css\">@font-face {font-family: MyFont;src: url(\"file:///android_asset/fonts/opensansromanregular.ttf\")}body{font-family: MyFont;color: " + method.webViewText() + "font-size: 14px;line-height:1.7;margin-left: 0px;margin-right: 0px;margin-top: 0px;margin-bottom: 0px;padding: 0px;}"
                                    + "a {color:" + method.webViewLink() + "text-decoration:none}"
                                    + "</style></head>"
                                    + "<body>"
                                    + bookDetailListPos.getPost_description()
                                    + "</body></html>";

                            viewBookDetail.wvBookDesc.loadDataWithBaseURL(null, text, mimeType, encoding, null);

                            viewBookDetail.ivRateList.setOnClickListener(v -> {
                                Intent intentDetail = new Intent(BookDetailsActivity.this, RateReviewActivity.class);
                                intentDetail.putExtra("RATE_BOOK_ID", bookDetailListPos.getPost_id());
                                startActivity(intentDetail);
                            });
                            viewBookDetail.tvRateAvg.setText(bookDetailListPos.getTotal_rate());
                            viewBookDetail.ratingView.setRating(Float.parseFloat(bookDetailListPos.getTotal_rate()));
                            viewBookDetail.tvRateTotalReviews.setText(getString(R.string.lbl_review_detail, Method.Format(Integer.parseInt(bookDetailListPos.getTotal_reviews()))));
                            viewBookDetail.btnWriteAReview.setOnClickListener(v -> {
                                if (method.getIsLogin()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("postId", bookDetailListPos.getPost_id());
                                    bundle.putString("userId", method.getUserId());
                                    WriteRateReviewFragment writeRateReviewFragment = new WriteRateReviewFragment();
                                    writeRateReviewFragment.setArguments(bundle);
                                    writeRateReviewFragment.show(getSupportFragmentManager(), writeRateReviewFragment.getTag());

                                } else {
                                    Toast.makeText(BookDetailsActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                                    Intent intentLogin = new Intent(BookDetailsActivity.this, LoginActivity.class);
                                    intentLogin.putExtra("isFromDetail", true);
                                    startActivity(intentLogin);
                                }
                            });

                            if (bookDetailListPos.getListRelatedBook().size() != 0) {
                                relatedAdapter = new RelatedAdapter(BookDetailsActivity.this, bookDetailListPos.getListRelatedBook());
                                viewBookDetail.rvRelatedBook.setAdapter(relatedAdapter);
                                relatedAdapter.setOnItemClickListener(position -> {
                                    Intent intentDetail = new Intent(BookDetailsActivity.this, BookDetailsActivity.class);
                                    intentDetail.putExtra("BOOK_ID", bookDetailListPos.getListRelatedBook().get(position).getPost_id());
                                    startActivity(intentDetail);
                                    finish();
                                });
                            } else {
                                viewBookDetail.rlRelatedSection.setVisibility(View.GONE);
                                viewBookDetail.rvRelatedBook.setVisibility(View.GONE);
                            }


                            viewBookDetail.llReportBook.setOnClickListener(v -> {
                                if (method.getIsLogin()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("postId", bookDetailListPos.getPost_id());
                                    bundle.putString("userId", method.getUserId());
                                    ReportBookFragment reportBookFragment = new ReportBookFragment();
                                    reportBookFragment.setArguments(bundle);
                                    reportBookFragment.show(getSupportFragmentManager(), reportBookFragment.getTag());

                                } else {
                                    Toast.makeText(BookDetailsActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                                    Intent intentLogin = new Intent(BookDetailsActivity.this, LoginActivity.class);
                                    intentLogin.putExtra("isFromDetail", true);
                                    startActivity(intentLogin);
                                }

                            });
                             
                            if (checkRentSubsPlanStatus()) {
                                viewBookDetail.btnBuyBook.setVisibility(View.GONE);
                            } else {
                                viewBookDetail.btnBuyBook.setVisibility(View.VISIBLE);
                            }
                            if (bookDetailListPos.getBook_on_rent().equals("1")) {
                                viewBookDetail.btnBuyBook.setText(getString(R.string.lbl_buy_book));
                            } else if (bookDetailListPos.getPost_access().equals("Paid")) {
                                viewBookDetail.btnBuyBook.setText(getString(R.string.lbl_buy_plan));
                            }
                            viewBookDetail.btnBuyBook.setOnClickListener(v -> {
                                if (method.getIsLogin()) {
                                    if (bookDetailListPos.getBook_on_rent().equals("1")) {
                                        Intent intentRent = new Intent(BookDetailsActivity.this, PaymentMethodActivity.class);
                                        intentRent.putExtra("planId", bookDetailListPos.getPost_id());
                                        intentRent.putExtra("planName", bookDetailListPos.getPost_title());
                                        intentRent.putExtra("planPrice", bookDetailListPos.getBook_rent_price());
                                        intentRent.putExtra("planPriceCurrency", Constant.constantCurrency);
                                        intentRent.putExtra("planUserId", method.getUserId());
                                        intentRent.putExtra("planDuration", bookDetailListPos.getBook_rent_time());
                                        intentRent.putExtra("isRent", true);
                                        startActivity(intentRent);
                                    } else {
                                        Intent intentPlan = new Intent(BookDetailsActivity.this, PlanListActivity.class);
                                        startActivity(intentPlan);
                                    }
                                } else {
                                    Toast.makeText(BookDetailsActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                                    Intent intentLogin = new Intent(BookDetailsActivity.this, LoginActivity.class);
                                    intentLogin.putExtra("isFromDetail", true);
                                    startActivity(intentLogin);
                                }
                            });

                            viewBookDetail.llReadBook.setOnClickListener(v -> {
                                if (bookDetailListPos.isVideo()) {
                                    openVideo();
                                } else if (checkRentSubsPlanStatus()) {
                                    maybeShowRewardedThenOpen();
                                } else {
                                    showPaidMsgDialog();
                                }
                            });

                            viewBookDetail.llDownload.setOnClickListener(v -> {
                                if (bookDetailListPos.getDownload_enable().equals("1")) {
                                    downloadPermission();
                                } else {
                                    Toast.makeText(BookDetailsActivity.this, getString(R.string.download_disable_msg), Toast.LENGTH_SHORT).show();
                                }

                            });

                            if (bookDetailListPos.isFavourite()) {
                                viewBookDetail.ivFav.setImageResource(R.drawable.img_fav_hover);
                            } else {
                                viewBookDetail.ivFav.setImageResource(R.drawable.img_fav);
                            }

                            viewBookDetail.llFavourite.setOnClickListener(v -> {
                                if (method.getIsLogin()) {
                                    FavouriteIF favouriteIF = (isFavourite, message) -> {
                                        if (isFavourite.equals("true")) {
                                            viewBookDetail.ivFav.setImageResource(R.drawable.img_fav_hover);
                                        } else {
                                            viewBookDetail.ivFav.setImageResource(R.drawable.img_fav);
                                        }
                                    };
                                    method.addToFav(bookDetailListPos.getPost_id(), method.getUserId(), "Book", favouriteIF);
                                } else {
                                    Toast.makeText(BookDetailsActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                                    Intent intentLogin = new Intent(BookDetailsActivity.this, LoginActivity.class);
                                    intentLogin.putExtra("isFromDetail", true);
                                    startActivity(intentLogin);
                                }
                            });

                        } else {
                            viewBookDetail.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                            viewBookDetail.nsView.setVisibility(View.GONE);
                            viewBookDetail.progressHome.setVisibility(View.GONE);
                        }
                    } else {
                        viewBookDetail.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                        viewBookDetail.nsView.setVisibility(View.GONE);
                        viewBookDetail.progressHome.setVisibility(View.GONE);
                        method.alertBox(getResources().getString(R.string.failed_try_again));
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }
                viewBookDetail.progressHome.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NotNull Call<BookDetailRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                viewBookDetail.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                viewBookDetail.progressHome.setVisibility(View.GONE);
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }

    // Play a YouTube video IN-APP (no redirect). content_type == "video".
    private void openVideo() {
        String vid = bookDetailListPos.getVideo_id();
        if ((vid == null || vid.isEmpty())) {
            // Fall back to extracting the id from the watch URL if needed.
            String url = bookDetailListPos.getVideo_url();
            if (url != null && url.contains("v=")) {
                vid = url.substring(url.indexOf("v=") + 2);
                int amp = vid.indexOf('&');
                if (amp > 0) vid = vid.substring(0, amp);
            }
        }
        if (vid == null || vid.isEmpty()) {
            Toast.makeText(this, getString(R.string.wrong), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, VideoPlayerActivity.class);
        i.putExtra("video_id", vid);
        i.putExtra("title", bookDetailListPos.getPost_title());
        startActivity(i);
    }

    private void downloadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dexter.withContext(BookDetailsActivity.this)
                    .withPermission(Manifest.permission.POST_NOTIFICATIONS)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            if (method.getIsLogin()) {
                                if (checkRentSubsPlanStatus()) {
                                    if (method.isNetworkAvailable()) {
                                        if (bookDetailListPos.getPost_file_url().contains(".epub")) {
                                            method.download(bookDetailListPos.getPost_id(),
                                                    bookDetailListPos.getPost_title(),
                                                    bookDetailListPos.getPost_image(),
                                                    bookDetailListPos.getListBookDetailAuthor().isEmpty() ? "" : bookDetailListPos.getListBookDetailAuthor().get(0).getAuthor_name(),
                                                    bookDetailListPos.getPost_file_url(), "epub");
                                        } else {
                                            method.download(bookDetailListPos.getPost_id(),
                                                    bookDetailListPos.getPost_title(),
                                                    bookDetailListPos.getPost_image(),
                                                    bookDetailListPos.getListBookDetailAuthor().isEmpty() ? "" : bookDetailListPos.getListBookDetailAuthor().get(0).getAuthor_name(),
                                                    bookDetailListPos.getPost_file_url(), "pdf");
                                        }
                                    }
                                } else {
                                    showDownloadPaidMsgDialog();
                                }
                            } else {
                                Toast.makeText(BookDetailsActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                                Intent intentLogin = new Intent(BookDetailsActivity.this, LoginActivity.class);
                                intentLogin.putExtra("isFromDetail", true);
                                startActivity(intentLogin);
                            }
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            // check for permanent denial of permission
                            showSettingsDialog(BookDetailsActivity.this);
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();

        } else {
            if (method.getIsLogin()) {
                if (checkRentSubsPlanStatus()) {
                    if (method.isNetworkAvailable()) {
                        if (bookDetailListPos.getPost_file_url().contains(".epub")) {
                            method.download(bookDetailListPos.getPost_id(),
                                    bookDetailListPos.getPost_title(),
                                    bookDetailListPos.getPost_image(),
                                    bookDetailListPos.getListBookDetailAuthor().isEmpty() ? "" : bookDetailListPos.getListBookDetailAuthor().get(0).getAuthor_name(),
                                    bookDetailListPos.getPost_file_url(), "epub");
                        } else {
                            method.download(bookDetailListPos.getPost_id(),
                                    bookDetailListPos.getPost_title(),
                                    bookDetailListPos.getPost_image(),
                                    bookDetailListPos.getListBookDetailAuthor().isEmpty() ? "" : bookDetailListPos.getListBookDetailAuthor().get(0).getAuthor_name(),
                                    bookDetailListPos.getPost_file_url(), "pdf");
                        }
                    }
                } else {
                    showDownloadPaidMsgDialog();
                }
            } else {
                Toast.makeText(BookDetailsActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                Intent intentLogin = new Intent(BookDetailsActivity.this, LoginActivity.class);
                intentLogin.putExtra("isFromDetail", true);
                startActivity(intentLogin);
            }
        }
    }

    private static void showSettingsDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Get Notified");
        builder.setMessage("Enable Notifications to show download notification");
        builder.setPositiveButton("GOTO SETTINGS", (dialog, which) -> {
            dialog.cancel();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private boolean checkRentSubsPlanStatus() {
        boolean isStatus;
        if (bookDetailListPos.getBook_on_rent().equals("1")) {
            isStatus = bookDetailListPos.isBook_purchased();
        } else {
            if (bookDetailListPos.getPost_access().equals("Paid")) {
                isStatus = bookDetailListPos.isUser_plan_status();
            } else {
                isStatus = true;
            }
        }
        return isStatus;
    }

    private void bookViewData(String bookId) {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(BookDetailsActivity.this));
        jsObj.addProperty("post_id", bookId);
        jsObj.addProperty("post_type", "Book");
        // Who is reading decides whether the uploader earns coins for it, and
        // stops the same reader paying out twice.
        if (method.getIsLogin()) {
            jsObj.addProperty("user_id", method.getUserId());
        }
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<JsonObject> call = apiService.getPostViewData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NotNull Call<JsonObject> call, @NotNull Response<JsonObject> response) {
            }

            @Override
            public void onFailure(@NotNull Call<JsonObject> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
            }
        });
    }

     // If this book requires a rewarded ad (and rewarded is enabled), show it first
     // and only open the book once the reward is earned. Otherwise open directly.
     private void maybeShowRewardedThenOpen() {
        boolean bookWantsRewarded = bookDetailListPos != null
                && "1".equals(bookDetailListPos.getRewarded_ad());
        if (bookWantsRewarded && Constant.isRewarded) {
            AdRewardedAds.showRewardedAd(BookDetailsActivity.this, new AdRewardedAds.OnRewardListener() {
                @Override
                public void onReward() {
                    openBook();
                }

                @Override
                public void onFailed() {
                    Toast.makeText(BookDetailsActivity.this,
                            getString(R.string.rewarded_required), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            openBook();
        }
    }

     private void openBook() {
        if (bookDetailListPos.getContinue_page_num().isEmpty()) {
            if (bookDetailListPos.getPost_file_url().contains(".epub")) {
                DownloadEpub downloadEpub = new DownloadEpub(BookDetailsActivity.this);
                downloadEpub.pathEpub(bookDetailListPos.getPost_file_url(), bookDetailListPos.getPost_id(), "");
            } else {
                startActivity(new Intent(BookDetailsActivity.this, PDFShow.class)
                        .putExtra("id", bookDetailListPos.getPost_id())
                        .putExtra("link", bookDetailListPos.getPost_file_url())
                        .putExtra("toolbarTitle", bookDetailListPos.getPost_title())
                        .putExtra("type", "link")
                        .putExtra("PAGE_NUM", ""));
            }
        } else {
            if (bookDetailListPos.getPost_file_url().contains(".epub")) {
                DownloadEpub downloadEpub = new DownloadEpub(BookDetailsActivity.this);
                downloadEpub.pathEpub(bookDetailListPos.getPost_file_url(), bookDetailListPos.getPost_id(), bookDetailListPos.getContinue_page_num());

            } else {
                startActivity(new Intent(BookDetailsActivity.this, PDFShow.class)
                        .putExtra("id", bookDetailListPos.getPost_id())
                        .putExtra("link", bookDetailListPos.getPost_file_url())
                        .putExtra("toolbarTitle", bookDetailListPos.getPost_title())
                        .putExtra("type", "link")
                        .putExtra("PAGE_NUM", bookDetailListPos.getContinue_page_num()));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GlobalBus.getBus().unregister(this);
    }

    @Subscribe
    public void onEvent(Events.UpdatePages updatePages) {
        if (method.getIsLogin()) {
            bookDetailListPos.setContinue_page_num(updatePages.getNumPages());
        }
    }

    public void showPaidMsgDialog() {
        Dialog dialog = new Dialog(BookDetailsActivity.this,R.style.Theme_AppCompat_Translucent);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rent_msg);
        dialog.setCancelable(false);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        dialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        TextView tvMsg=dialog.findViewById(R.id.txtPaidMsg);
        AppCompatButton btnBuy=dialog.findViewById(R.id.btnPaid);
        ImageView ivClose = dialog.findViewById(R.id.ivCloseReport);

        ivClose.setOnClickListener(v -> dialog.dismiss());

        if (bookDetailListPos.getBook_on_rent().equals("1")) {
            tvMsg.setText(getString(R.string.rent_book_msg));
            btnBuy.setText(getString(R.string.lbl_buy_book));
        } else {
            tvMsg.setText(getString(R.string.subs_book_msg));
            btnBuy.setText(getString(R.string.lbl_buy_plan));
       }

        btnBuy.setOnClickListener(v -> {
            dialog.dismiss();
            if (method.getIsLogin()) {
                if (bookDetailListPos.getBook_on_rent().equals("1")) {
                    Intent intentRent = new Intent(BookDetailsActivity.this, PaymentMethodActivity.class);
                    intentRent.putExtra("planId", bookDetailListPos.getPost_id());
                    intentRent.putExtra("planName", bookDetailListPos.getPost_title());
                    intentRent.putExtra("planPrice", bookDetailListPos.getBook_rent_price());
                    intentRent.putExtra("planPriceCurrency", Constant.constantCurrency);
                    intentRent.putExtra("planUserId", method.getUserId());
                    intentRent.putExtra("planDuration", bookDetailListPos.getBook_rent_time());
                    intentRent.putExtra("isRent", true);
                    startActivity(intentRent);
                } else {
                    Intent intentPlan = new Intent(BookDetailsActivity.this, PlanListActivity.class);
                    startActivity(intentPlan);
                }
            } else {
                Toast.makeText(BookDetailsActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                Intent intentLogin = new Intent(BookDetailsActivity.this, LoginActivity.class);
                intentLogin.putExtra("isFromDetail", true);
                startActivity(intentLogin);
            }
        });

        dialog.show();
    }

    public void showDownloadPaidMsgDialog() {
        Dialog dialog = new Dialog(BookDetailsActivity.this,R.style.Theme_AppCompat_Translucent);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rent_msg);
        dialog.setCancelable(false);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        dialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        TextView tvMsg=dialog.findViewById(R.id.txtPaidMsg);
        AppCompatButton btnBuy=dialog.findViewById(R.id.btnPaid);
        ImageView ivClose = dialog.findViewById(R.id.ivCloseReport);

        ivClose.setOnClickListener(v -> dialog.dismiss());

        if (bookDetailListPos.getBook_on_rent().equals("1")) {
            tvMsg.setText(getString(R.string.rent_download_book_msg));
            btnBuy.setText(getString(R.string.lbl_buy_book));
        } else {
            tvMsg.setText(getString(R.string.subs_download_book_msg));
            btnBuy.setText(getString(R.string.lbl_buy_plan));
        }

        btnBuy.setOnClickListener(v -> {
            dialog.dismiss();
            if (method.getIsLogin()) {
                if (bookDetailListPos.getBook_on_rent().equals("1")) {
                    Intent intentRent = new Intent(BookDetailsActivity.this, PaymentMethodActivity.class);
                    intentRent.putExtra("planId", bookDetailListPos.getPost_id());
                    intentRent.putExtra("planName", bookDetailListPos.getPost_title());
                    intentRent.putExtra("planPrice", bookDetailListPos.getBook_rent_price());
                    intentRent.putExtra("planPriceCurrency", Constant.constantCurrency);
                    intentRent.putExtra("planUserId", method.getUserId());
                    intentRent.putExtra("planDuration", bookDetailListPos.getBook_rent_time());
                    intentRent.putExtra("isRent", true);
                    startActivity(intentRent);
                } else {
                    Intent intentPlan = new Intent(BookDetailsActivity.this, PlanListActivity.class);
                    startActivity(intentPlan);
                }
            } else {
                Toast.makeText(BookDetailsActivity.this, getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                Intent intentLogin = new Intent(BookDetailsActivity.this, LoginActivity.class);
                intentLogin.putExtra("isFromDetail", true);
                startActivity(intentLogin);
            }
        });

        dialog.show();
    }
}
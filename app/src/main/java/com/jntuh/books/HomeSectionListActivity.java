package com.jntuh.books;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jntuh.adapter.HomeContentSingleAdapter;
import com.jntuh.books.databinding.ActivityBookListSubCatBinding;
import com.jntuh.item.HomeContent;
import com.jntuh.item.HomeSection;
import com.jntuh.response.HomeRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.BannerAds;
import com.jntuh.util.Constant;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HomeSectionListActivity extends AppCompatActivity {

    ActivityBookListSubCatBinding viewBookListSubCat;
    HomeContentSingleAdapter homeContentSingleAdapter;
    String strId, strTitle, strType;
    Method method;
    List<HomeContent> homeContentsSingle;
    int j = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewBookListSubCat = ActivityBookListSubCatBinding.inflate(getLayoutInflater());
        setContentView(viewBookListSubCat.getRoot());

        method = new Method(HomeSectionListActivity.this);
        method.forceRTLIfSupported();
        homeContentsSingle = new ArrayList<>();

        Intent intent = getIntent();
        strId = intent.getStringExtra("ID");
        strTitle = intent.getStringExtra("TITLE");
        strType = intent.getStringExtra("TYPE");

        viewBookListSubCat.toolbarMain.tvToolbarTitle.setText(strTitle);
        viewBookListSubCat.toolbarMain.ivSearch.setVisibility(View.GONE);
        viewBookListSubCat.llShowIn.setVisibility(View.GONE);
        viewBookListSubCat.toolbarMain.imageFilter.setVisibility(View.GONE);
        viewBookListSubCat.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        viewBookListSubCat.progressHome.setVisibility(View.GONE);
        viewBookListSubCat.llNoData.clNoDataFound.setVisibility(View.GONE);

        if (strType.equals("author")) {
            viewBookListSubCat.rvBookList.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(HomeSectionListActivity.this, 3);
            viewBookListSubCat.rvBookList.setLayoutManager(layoutManager);
            viewBookListSubCat.rvBookList.setFocusable(false);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (homeContentSingleAdapter.getItemViewType(position) == 5) {
                        return 3;
                    }
                    return 1;
                }
            });
        } else {
            viewBookListSubCat.rvBookList.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(HomeSectionListActivity.this, 2);
            viewBookListSubCat.rvBookList.setLayoutManager(layoutManager);
            viewBookListSubCat.rvBookList.setFocusable(false);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (homeContentSingleAdapter.getItemViewType(position) == 5) {
                        return 2;
                    }
                    return 1;
                }
            });
        }


        if (method.isNetworkAvailable()) {
            bookListGridBySubCategoryData();
        } else {
            method.alertBox(getResources().getString(R.string.internet_connection));
        }
        BannerAds.showBannerAds(HomeSectionListActivity.this, viewBookListSubCat.layoutAds);
    }

    private void bookListGridBySubCategoryData() {

        viewBookListSubCat.progressHome.setVisibility(View.VISIBLE);
        Call<HomeRP> call;
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(HomeSectionListActivity.this));
        jsObj.addProperty("id", strId);
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        call = apiService.getHomeSectionSingleData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<HomeRP>() {
            @Override
            public void onResponse(@NotNull Call<HomeRP> call, @NotNull Response<HomeRP> response) {

                try {

                    HomeRP subCatListBookRP = response.body();

                    if (subCatListBookRP !=null && subCatListBookRP.getEbookApp().getHomeSections().get(0).getHomeContent().size() != 0) {
                        viewBookListSubCat.rvBookList.setVisibility(View.VISIBLE);
                        for (int i = 0; i < subCatListBookRP.getEbookApp().getHomeSections().get(0).getHomeContent().size(); i++) {
                            if (Constant.isNative) {
                                if (j % Constant.nativePosition == 0) {
                                    homeContentsSingle.add(null);
                                    j++;
                                }
                            }
                            homeContentsSingle.add(subCatListBookRP.getEbookApp().getHomeSections().get(0).getHomeContent().get(i));
                            j++;
                        }
                        homeContentSingleAdapter = new HomeContentSingleAdapter(HomeSectionListActivity.this, homeContentsSingle, subCatListBookRP.getEbookApp().getHomeSections().get(0).getHomeType());
                        viewBookListSubCat.rvBookList.setAdapter(homeContentSingleAdapter);
                        HomeSection homeContent = subCatListBookRP.getEbookApp().getHomeSections().get(0);
                        homeContentSingleAdapter.setOnItemClickListener(position -> {
                            switch (homeContent.getHomeType()) {
                                case "book":
                                    Intent intentDetail = new Intent(HomeSectionListActivity.this, BookDetailsActivity.class);
                                    intentDetail.putExtra("BOOK_ID", homeContentsSingle.get(position).getPostId());
                                    startActivity(intentDetail);
                                    break;
                                case "author": {
                                    Intent intentSubCat = new Intent(HomeSectionListActivity.this, AuthorDetailsActivity.class);
                                    intentSubCat.putExtra("AUTHOR_ID", homeContentsSingle.get(position).getPostId());
                                    startActivity(intentSubCat);
                                    break;
                                }
                                case "category": {
                                    if (homeContentsSingle.get(position).getSub_cat_status().equals("true")) {
                                        Intent intentSubCat = new Intent(HomeSectionListActivity.this, SubCategoryActivity.class);
                                        intentSubCat.putExtra("postCatId", homeContentsSingle.get(position).getPostId());
                                        intentSubCat.putExtra("postCatName", homeContentsSingle.get(position).getPostTitle());
                                        startActivity(intentSubCat);
                                    } else {
                                        Intent intentSubCat = new Intent(HomeSectionListActivity.this, BookListBySubCatActivity.class);
                                        intentSubCat.putExtra("postSubCatId", homeContentsSingle.get(position).getPostId());
                                        intentSubCat.putExtra("postSubCatName", homeContentsSingle.get(position).getPostTitle());
                                        intentSubCat.putExtra("type", "Cat");
                                        startActivity(intentSubCat);
                                    }
                                    break;
                                }
                                case "subcategory": {
                                    Intent intentSubCat = new Intent(HomeSectionListActivity.this, BookListBySubCatActivity.class);
                                    intentSubCat.putExtra("postSubCatId", homeContentsSingle.get(position).getPostId());
                                    intentSubCat.putExtra("postSubCatName", homeContentsSingle.get(position).getPostTitle());
                                    intentSubCat.putExtra("type", "");
                                    startActivity(intentSubCat);
                                    break;
                                }
                            }
                        });


                    } else {
                        viewBookListSubCat.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                        viewBookListSubCat.rvBookList.setVisibility(View.GONE);
                        viewBookListSubCat.progressHome.setVisibility(View.GONE);
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }
                viewBookListSubCat.progressHome.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NotNull Call<HomeRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                viewBookListSubCat.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                viewBookListSubCat.progressHome.setVisibility(View.GONE);
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }
}

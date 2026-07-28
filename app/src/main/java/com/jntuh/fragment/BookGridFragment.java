package com.jntuh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jntuh.adapter.BookGridAdapter;
import com.jntuh.adapter.LatestBookGridAdapter;
import com.jntuh.books.BookDetailsActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.FragmentBookBinding;
import com.jntuh.item.SubCatListBook;
import com.jntuh.response.SubCatListBookRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Constant;
import com.jntuh.util.EndlessRecyclerViewScrollListener;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class BookGridFragment extends Fragment {

    FragmentBookBinding viewBook;
    Method method;
    BookGridAdapter bookGridAdapter;
    private int pageIndex = 1;
    private boolean isFirst = true, isOver = false;
    List<SubCatListBook> listBooks;
    int j = 1;
    EndlessRecyclerViewScrollListener endlessRecyclerViewScrollListener;
    String strSubCatId, strSubCatName, strType;
    LatestBookGridAdapter latestBookGridAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        viewBook = FragmentBookBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());
        if (getArguments() != null) {
            strSubCatId = getArguments().getString("postSubCatId");
            strSubCatName = getArguments().getString("postSubCatName");
            strType = getArguments().getString("type");
        }
        listBooks = new ArrayList<>();

        viewBook.progressHome.setVisibility(View.GONE);
        viewBook.llNoData.clNoDataFound.setVisibility(View.GONE);


        if (strType.equals("TREND")) {
            viewBook.rvCat.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(requireActivity(), 2);
            viewBook.rvCat.setLayoutManager(layoutManager);
            viewBook.rvCat.setFocusable(false);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (latestBookGridAdapter.getItemViewType(position) == 1) {
                        return 2;
                    }
                    return 1;
                }
            });
            bookGridByTrend();
        } else if (strType.equals("LATEST")) {
            viewBook.rvCat.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(requireActivity(), 2);
            viewBook.rvCat.setLayoutManager(layoutManager);
            viewBook.rvCat.setFocusable(false);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (latestBookGridAdapter.getItemViewType(position) == 1) {
                        return 2;
                    }
                    return 1;
                }
            });
            bookGridByTrend();
        } else {
            viewBook.rvCat.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(requireActivity(), 2);
            viewBook.rvCat.setLayoutManager(layoutManager);
            viewBook.rvCat.setFocusable(false);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (bookGridAdapter.getItemViewType(position) != 1) {
                        return 2;
                    }
                    return 1;
                }
            });
            if (method.isNetworkAvailable()) {
                bookListGridBySubCategoryData();
            } else {
                method.alertBox(getString(R.string.internet_connection));
            }
            endlessRecyclerViewScrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    if (!isOver) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                pageIndex++;
                                bookListGridBySubCategoryData();
                            }
                        }, 1000);
                    } else {
                        bookGridAdapter.hideHeader();
                    }
                }
            };

            viewBook.rvCat.addOnScrollListener(endlessRecyclerViewScrollListener);
        }
        return viewBook.getRoot();
    }

    private void bookListGridBySubCategoryData() {
        if (getActivity() != null) {
            if (isFirst)
                viewBook.progressHome.setVisibility(View.VISIBLE);
            Call<SubCatListBookRP> call;
            JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
            if ("Cat".equals(strType)) {
                jsObj.addProperty("cat_id", strSubCatId);
                jsObj.addProperty("user_id", method.getUserId());
                ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
                call = apiService.getBookListByCatData(API.toBase64(jsObj.toString()), pageIndex);
            } else if ("SearchHome".equals(strType)) {
                jsObj.addProperty("search_text", strSubCatName);
                jsObj.addProperty("user_id", method.getUserId());
                ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
                call = apiService.getSearchBookData(API.toBase64(jsObj.toString()), pageIndex);
            } else if ("Search".equals(strType)) {
                jsObj.addProperty("search_text", strSubCatName);
                jsObj.addProperty("user_id", method.getUserId());
                ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
                call = apiService.getSearchBookData(API.toBase64(jsObj.toString()), pageIndex);
            } else if ("sortByFilter".equals(strType)) {
                jsObj.addProperty("filter_type", "sort_by");
                jsObj.addProperty("filter_val", strSubCatName);
                jsObj.addProperty("user_id", method.getUserId());
                ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
                call = apiService.getFilterSearchBookData(API.toBase64(jsObj.toString()), pageIndex);
            } else if ("AuthorFilter".equals(strType)) {
                jsObj.addProperty("filter_type", "author_by");
                jsObj.addProperty("filter_val", strSubCatName);
                jsObj.addProperty("user_id", method.getUserId());
                ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
                call = apiService.getFilterSearchBookData(API.toBase64(jsObj.toString()), pageIndex);
            } else if ("CatFilter".equals(strType)) {
                jsObj.addProperty("filter_type", "category_by");
                jsObj.addProperty("filter_val", strSubCatName);
                jsObj.addProperty("user_id", method.getUserId());
                ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
                call = apiService.getFilterSearchBookData(API.toBase64(jsObj.toString()), pageIndex);
            } else {
                jsObj.addProperty("sub_cat_id", strSubCatId);
                jsObj.addProperty("user_id", method.getUserId());
                ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
                call = apiService.getBookListBySubCatData(API.toBase64(jsObj.toString()), pageIndex);
            }
            call.enqueue(new Callback<SubCatListBookRP>() {
                @Override
                public void onResponse(@NotNull Call<SubCatListBookRP> call, @NotNull Response<SubCatListBookRP> response) {
                    if (getActivity() != null) {
                        try {

                            SubCatListBookRP subCatListBookRP = response.body();

                            if (subCatListBookRP != null && subCatListBookRP.getSuccess().equals("1")) {
                                if (subCatListBookRP.getSubCatListBooks().size() != 0) {
                                    for (int i = 0; i < subCatListBookRP.getSubCatListBooks().size(); i++) {
                                        if (Constant.isNative) {
                                            if (j % Constant.nativePosition == 0) {
                                                listBooks.add(null);
                                                j++;
                                            }
                                        }
                                        listBooks.add(subCatListBookRP.getSubCatListBooks().get(i));
                                        j++;
                                    }
                                    if (isFirst) {
                                        isFirst = false;
                                        viewBook.rvCat.setVisibility(View.VISIBLE);
                                        bookGridAdapter = new BookGridAdapter(requireActivity(), listBooks);
                                        viewBook.rvCat.setAdapter(bookGridAdapter);
                                    } else {
                                        bookGridAdapter.notifyDataSetChanged();
                                    }
                                    bookGridAdapter.setOnItemClickListener(position -> {
                                        Intent intentDetail = new Intent(requireActivity(), BookDetailsActivity.class);
                                        intentDetail.putExtra("BOOK_ID", listBooks.get(position).getPost_id());
                                        startActivity(intentDetail);
                                    });

                                } else {
                                    isOver = true;
                                    if (bookGridAdapter != null) { // when there is no data in first time
                                        bookGridAdapter.hideHeader();
                                    }
                                    if (isFirst) {
                                        viewBook.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                                        viewBook.rvCat.setVisibility(View.GONE);
                                        viewBook.progressHome.setVisibility(View.GONE);
                                    }
                                }
                            } else {
                                viewBook.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                                viewBook.rvCat.setVisibility(View.GONE);
                                viewBook.progressHome.setVisibility(View.GONE);
                                method.alertBox(getString(R.string.failed_try_again));
                            }

                        } catch (Exception e) {
                            Log.d("exception_error", e.toString());
                            method.alertBox(getString(R.string.failed_try_again));
                        }
                    }
                    viewBook.progressHome.setVisibility(View.GONE);
                }

                @Override
                public void onFailure(@NotNull Call<SubCatListBookRP> call, @NotNull Throwable t) {
                    // Log error here since request failed
                    Log.e("fail", t.toString());
                    viewBook.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                    viewBook.progressHome.setVisibility(View.GONE);
                    method.alertBox(getString(R.string.failed_try_again));
                }
            });
        }
    }

    private void bookGridByTrend() {
        if (getActivity() != null) {
            viewBook.progressHome.setVisibility(View.VISIBLE);
            Call<SubCatListBookRP> call;
            JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
            jsObj.addProperty("user_id", method.getUserId());
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
            if (strType.equals("LATEST")) {
                call = apiService.getBookLatestData(API.toBase64(jsObj.toString()));
            } else {
                call = apiService.getTrendingBookData(API.toBase64(jsObj.toString()));
            }

            call.enqueue(new Callback<SubCatListBookRP>() {
                @Override
                public void onResponse(@NotNull Call<SubCatListBookRP> call, @NotNull Response<SubCatListBookRP> response) {
                    if (getActivity() != null) {
                        try {

                            SubCatListBookRP subCatListBookRP = response.body();

                            if (subCatListBookRP != null && subCatListBookRP.getSuccess().equals("1")) {
                                if (subCatListBookRP.getSubCatListBooks().size() != 0) {
                                    viewBook.rvCat.setVisibility(View.VISIBLE);
                                    for (int i = 0; i < subCatListBookRP.getSubCatListBooks().size(); i++) {
                                        if (Constant.isNative) {
                                            if (j % Constant.nativePosition == 0) {
                                                listBooks.add(null);
                                                j++;
                                            }
                                        }
                                        listBooks.add(subCatListBookRP.getSubCatListBooks().get(i));
                                        j++;
                                    }
                                    latestBookGridAdapter = new LatestBookGridAdapter(requireActivity(), listBooks);
                                    viewBook.rvCat.setAdapter(latestBookGridAdapter);
                                    latestBookGridAdapter.setOnItemClickListener(position -> {
                                        Intent intentDetail = new Intent(requireActivity(), BookDetailsActivity.class);
                                        intentDetail.putExtra("BOOK_ID", listBooks.get(position).getPost_id());
                                        startActivity(intentDetail);
                                    });

                                } else {
                                    viewBook.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                                    viewBook.rvCat.setVisibility(View.GONE);
                                    viewBook.progressHome.setVisibility(View.GONE);
                                }
                            } else {
                                viewBook.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                                viewBook.rvCat.setVisibility(View.GONE);
                                viewBook.progressHome.setVisibility(View.GONE);
                                method.alertBox(getString(R.string.failed_try_again));
                            }

                        } catch (Exception e) {
                            Log.d("exception_error", e.toString());
                            method.alertBox(getString(R.string.failed_try_again));
                        }
                    }
                    viewBook.progressHome.setVisibility(View.GONE);
                }

                @Override
                public void onFailure(@NotNull Call<SubCatListBookRP> call, @NotNull Throwable t) {
                    // Log error here since request failed
                    Log.e("fail", t.toString());
                    viewBook.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                    viewBook.progressHome.setVisibility(View.GONE);
                    method.alertBox(getString(R.string.failed_try_again));
                }
            });
        }
    }
}

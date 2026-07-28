package com.jntuh.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jntuh.adapter.AuthorNameAdapter;
import com.jntuh.adapter.CatNameAdapter;
import com.jntuh.books.FilterSearchBookActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.LayoutBottomFilterBinding;
import com.jntuh.item.AuthorList;
import com.jntuh.item.CategoryList;
import com.jntuh.response.AuthorRP;
import com.jntuh.response.CatRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class FilterBookFragment extends BottomSheetDialogFragment {

    LayoutBottomFilterBinding viewFilterBookBinding;
    ProgressDialog progressDialog;
    Method method;
    TextView[] tvColor;
    int selectFilterPos = 0;
    String sortByValue;
    AuthorNameAdapter authorNameAdapter;
    List<AuthorList> authorList;
    int SORTBY = 0, AUTHOR = 1, CAT = 2;
    CatNameAdapter catNameAdapter;
    List<CategoryList> categoryLists;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        viewFilterBookBinding = LayoutBottomFilterBinding.inflate(inflater, container, false);
        Objects.requireNonNull(getDialog()).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        method = new Method(requireActivity());
        method.forceRTLIfSupported();
        authorList = new ArrayList<>();
        categoryLists = new ArrayList<>();

        progressDialog = new ProgressDialog(requireActivity(), R.style.MyAlertDialogStyle);
        viewFilterBookBinding.ivCloseReport.setOnClickListener(v -> dismiss());

        viewFilterBookBinding.btnSubmit.setOnClickListener(v -> {
            if (selectFilterPos == SORTBY) {
                if (viewFilterBookBinding.rbSortByNew.isChecked()) {
                    sortByValue = "NewArrival";
                    goSortBy();
                } else if (viewFilterBookBinding.rbSortByPop.isChecked()) {
                    sortByValue = "Popularity";
                    goSortBy();
                } else if (viewFilterBookBinding.rbSortByRating.isChecked()) {
                    sortByValue = "Ratings";
                    goSortBy();
                } else {
                    Toast.makeText(requireActivity(), getString(R.string.select_sort), Toast.LENGTH_SHORT).show();
                }

            } else if (selectFilterPos == AUTHOR) {//author
                if (getAuthorCommaSepIds().isEmpty()) {
                    Toast.makeText(requireActivity(), getString(R.string.select_sort), Toast.LENGTH_SHORT).show();
                } else {
                    goAuthor();
                }

            } else if (selectFilterPos == CAT) {
                if (getCatCommaSepIds().isEmpty()) {
                    Toast.makeText(requireActivity(), getString(R.string.select_sort), Toast.LENGTH_SHORT).show();
                } else {
                    goCat();
                }
            }
        });

        viewFilterBookBinding.btnReset.setOnClickListener(v -> {
            if (selectFilterPos == SORTBY) {
                viewFilterBookBinding.rgSortBy.clearCheck();
            } else if (selectFilterPos == AUTHOR) {
                for (AuthorList author : authorList) {
                    author.setSelected(false);
                }
                authorNameAdapter.notifyDataSetChanged();

            } else if (selectFilterPos == CAT) {
                for (CategoryList author : categoryLists) {
                    author.setSelectedCat(false);
                }
                catNameAdapter.notifyDataSetChanged();
            }
        });

        tvColor = new TextView[]{viewFilterBookBinding.tvSortBy, viewFilterBookBinding.tvAuthorBy, viewFilterBookBinding.tvCatBy};
        selectStarFilter(SORTBY);

        viewFilterBookBinding.tvSortBy.setOnClickListener(v -> {
            selectStarFilter(SORTBY);
            viewFilterBookBinding.rlSortBy.setVisibility(View.VISIBLE);
            viewFilterBookBinding.rlCategory.setVisibility(View.GONE);
            viewFilterBookBinding.rlAuthor.setVisibility(View.GONE);
        });
        viewFilterBookBinding.tvAuthorBy.setOnClickListener(v -> {
            selectStarFilter(AUTHOR);
            viewFilterBookBinding.rlSortBy.setVisibility(View.GONE);
            viewFilterBookBinding.rlCategory.setVisibility(View.GONE);
            viewFilterBookBinding.rlAuthor.setVisibility(View.VISIBLE);
            viewFilterBookBinding.llNoData.clNoDataFound.setVisibility(View.GONE);
            viewFilterBookBinding.progressHome.setVisibility(View.GONE);
            authorData();
            viewFilterBookBinding.rvList.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(requireActivity(), 1);
            viewFilterBookBinding.rvList.setLayoutManager(layoutManager);
            viewFilterBookBinding.rvList.setFocusable(false);
        });

        viewFilterBookBinding.tvCatBy.setOnClickListener(v -> {
            selectStarFilter(CAT);
            viewFilterBookBinding.rlSortBy.setVisibility(View.GONE);
            viewFilterBookBinding.rlCategory.setVisibility(View.VISIBLE);
            viewFilterBookBinding.rlAuthor.setVisibility(View.GONE);
            viewFilterBookBinding.llNoDataCat.clNoDataFound.setVisibility(View.GONE);
            viewFilterBookBinding.progressCategory.setVisibility(View.GONE);
            categoryData();
            viewFilterBookBinding.rvListCategory.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(requireActivity(), 1);
            viewFilterBookBinding.rvListCategory.setLayoutManager(layoutManager);
            viewFilterBookBinding.rvListCategory.setFocusable(false);
        });

        return viewFilterBookBinding.getRoot();
    }

    private void selectStarFilter(int pos) {
        selectFilterPos = pos;
        for (int i = 0; i < tvColor.length; i++) {
            if (i == pos) {
                tvColor[i].setBackgroundColor(getResources().getColor(R.color.filter_select_box));
                tvColor[i].setTextColor(getResources().getColor(R.color.filter_select_text));
            } else {
                tvColor[i].setBackgroundColor(getResources().getColor(R.color.filter_unselect_box));
                tvColor[i].setTextColor(getResources().getColor(R.color.filter_unselect_text));
            }
        }

    }

    private void goSortBy() {
        Intent intentN = new Intent(requireActivity(), FilterSearchBookActivity.class);
        intentN.putExtra("searchValue", sortByValue);
        intentN.putExtra("isFrom", "sortByFilter");
        startActivity(intentN);
    }

    private void goAuthor() {
        Intent intentN = new Intent(requireActivity(), FilterSearchBookActivity.class);
        intentN.putExtra("searchValue", getAuthorCommaSepIds());
        intentN.putExtra("isFrom", "AuthorFilter");
        startActivity(intentN);
    }

    private void goCat() {
        Intent intentN = new Intent(requireActivity(), FilterSearchBookActivity.class);
        intentN.putExtra("searchValue", getCatCommaSepIds());
        intentN.putExtra("isFrom", "CatFilter");
        startActivity(intentN);
    }


    private void authorData() {
        viewFilterBookBinding.progressHome.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<AuthorRP> call = apiService.getAllAuthorData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<AuthorRP>() {
            @Override
            public void onResponse(@NotNull Call<AuthorRP> call, @NotNull Response<AuthorRP> response) {

                try {

                    AuthorRP authorRP = response.body();

                    if (authorRP !=null && authorRP.getSuccess().equals("1")) {
                        if (authorRP.getAuthorLists().size() != 0) {
                            authorList = authorRP.getAuthorLists();
                            authorNameAdapter = new AuthorNameAdapter(requireActivity(), authorList);
                            viewFilterBookBinding.rvList.setAdapter(authorNameAdapter);

                            viewFilterBookBinding.edtAuthorSearch.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    if (authorNameAdapter != null) {
                                        authorNameAdapter.filter(s.toString());
                                    }
                                }

                                @Override
                                public void afterTextChanged(Editable s) {

                                }
                            });

                        } else {
                            viewFilterBookBinding.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                            viewFilterBookBinding.rvList.setVisibility(View.GONE);
                            viewFilterBookBinding.progressHome.setVisibility(View.GONE);
                        }
                    } else {
                        viewFilterBookBinding.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                        viewFilterBookBinding.rvList.setVisibility(View.GONE);
                        viewFilterBookBinding.progressHome.setVisibility(View.GONE);
                        method.alertBox(getResources().getString(R.string.failed_try_again));
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }

                viewFilterBookBinding.progressHome.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NotNull Call<AuthorRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                viewFilterBookBinding.llNoData.clNoDataFound.setVisibility(View.VISIBLE);
                viewFilterBookBinding.progressHome.setVisibility(View.GONE);
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });

    }

    private void categoryData() {
        viewFilterBookBinding.progressCategory.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<CatRP> call = apiService.getAllCatData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<CatRP>() {
            @Override
            public void onResponse(@NotNull Call<CatRP> call, @NotNull Response<CatRP> response) {

                try {

                    CatRP catRP = response.body();

                    if (catRP !=null && catRP.getSuccess().equals("1")) {
                        if (catRP.getCategoryLists().size() != 0) {
                            categoryLists = catRP.getCategoryLists();
                            catNameAdapter = new CatNameAdapter(requireActivity(), catRP.getCategoryLists());
                            viewFilterBookBinding.rvListCategory.setAdapter(catNameAdapter);
                            viewFilterBookBinding.edtCatSearch.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    if (catNameAdapter != null) {
                                        catNameAdapter.filter(s.toString());
                                    }
                                }

                                @Override
                                public void afterTextChanged(Editable s) {

                                }
                            });

                        } else {
                            viewFilterBookBinding.llNoDataCat.clNoDataFound.setVisibility(View.VISIBLE);
                            viewFilterBookBinding.rvListCategory.setVisibility(View.GONE);
                            viewFilterBookBinding.progressCategory.setVisibility(View.GONE);
                        }
                    } else {
                        viewFilterBookBinding.llNoDataCat.clNoDataFound.setVisibility(View.VISIBLE);
                        viewFilterBookBinding.rvListCategory.setVisibility(View.GONE);
                        viewFilterBookBinding.progressCategory.setVisibility(View.GONE);
                        method.alertBox(getResources().getString(R.string.failed_try_again));
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(getResources().getString(R.string.failed_try_again));
                }

                viewFilterBookBinding.progressCategory.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NotNull Call<CatRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                viewFilterBookBinding.llNoDataCat.clNoDataFound.setVisibility(View.VISIBLE);
                viewFilterBookBinding.progressCategory.setVisibility(View.GONE);
                method.alertBox(getResources().getString(R.string.failed_try_again));
            }
        });
    }

    @NonNull
    private String getCatCommaSepIds() {
        StringBuilder stringBuilder = new StringBuilder();
        String prefix = "";
        for (CategoryList itemFilter : categoryLists) {
            if (itemFilter.isSelectedCat()) {
                stringBuilder.append(prefix);
                prefix = ",";
                stringBuilder.append(itemFilter.getPost_id());
            }
        }

        return stringBuilder.toString();
    }

    @NonNull
    private String getAuthorCommaSepIds() {
        StringBuilder stringBuilder = new StringBuilder();
        String prefix = "";
        for (AuthorList itemFilter : authorList) {
            if (itemFilter.isSelected()) {
                stringBuilder.append(prefix);
                prefix = ",";
                stringBuilder.append(itemFilter.getPost_id());
            }
        }

        return stringBuilder.toString();
    }
}
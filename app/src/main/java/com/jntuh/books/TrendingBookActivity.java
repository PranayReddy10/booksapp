package com.jntuh.books;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.jntuh.books.databinding.ActivityBookListBinding;
import com.jntuh.fragment.BookGridFragment;
import com.jntuh.fragment.BookListFragment;
import com.jntuh.fragment.FilterBookFragment;
import com.jntuh.item.SubCatListBook;
import com.jntuh.util.BannerAds;
import com.jntuh.util.Method;

import java.util.ArrayList;
import java.util.List;


public class TrendingBookActivity extends AppCompatActivity {

    ActivityBookListBinding viewBookListSubCat;
    Method method;
    List<SubCatListBook> listBooks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewBookListSubCat = ActivityBookListBinding.inflate(getLayoutInflater());
        setContentView(viewBookListSubCat.getRoot());

        method = new Method(TrendingBookActivity.this);
        method.forceRTLIfSupported();
        listBooks = new ArrayList<>();

        viewBookListSubCat.toolbarMain.tvToolbarTitle.setText(getString(R.string.lbl_trending));

        viewBookListSubCat.toolbarMain.ivSearch.setVisibility(View.VISIBLE);
        viewBookListSubCat.toolbarMain.ivSearch.setOnClickListener(v -> {
            Intent intentSearch = new Intent(TrendingBookActivity.this, SearchBookActivity.class);
            startActivity(intentSearch);
        });

        viewBookListSubCat.toolbarMain.imageFilter.setVisibility(View.VISIBLE);
        viewBookListSubCat.toolbarMain.imageFilter.setOnClickListener(v -> {
            FilterBookFragment filterBookFragment = new FilterBookFragment();
            filterBookFragment.show(getSupportFragmentManager(), filterBookFragment.getTag());
        });

        viewBookListSubCat.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        viewBookListSubCat.ivViewGrid.setOnClickListener(v -> {
            viewBookListSubCat.ivViewList.setColorFilter(getResources().getColor(R.color.icon_view_normal), PorterDuff.Mode.SRC_IN);
            viewBookListSubCat.ivViewGrid.setColorFilter(getResources().getColor(R.color.icon_view_select), PorterDuff.Mode.SRC_IN);

            if (method.isNetworkAvailable()) {
                goGrid();
            } else {
                method.alertBox(getResources().getString(R.string.internet_connection));
            }
        });

        viewBookListSubCat.ivViewList.setOnClickListener(v -> {
            viewBookListSubCat.ivViewList.setColorFilter(getResources().getColor(R.color.icon_view_select), PorterDuff.Mode.SRC_IN);
            viewBookListSubCat.ivViewGrid.setColorFilter(getResources().getColor(R.color.icon_view_normal), PorterDuff.Mode.SRC_IN);
            if (method.isNetworkAvailable()) {
                goList();
            } else {
                method.alertBox(getResources().getString(R.string.internet_connection));
            }
        });

        if (method.isNetworkAvailable()) {
            goGrid();
        } else {
            method.alertBox(getResources().getString(R.string.internet_connection));
        }
        BannerAds.showBannerAds(TrendingBookActivity.this, viewBookListSubCat.layoutAds);
    }

    private void goGrid() {
        Bundle bundle = new Bundle();
        bundle.putString("postSubCatId", "");
        bundle.putString("postSubCatName", "");
        bundle.putString("type", "TREND");
        BookGridFragment bookGridFragment = new BookGridFragment();
        bookGridFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().add(R.id.frameMain, bookGridFragment, "")
                .commitAllowingStateLoss();
    }

    private void goList() {
        Bundle bundle = new Bundle();
        bundle.putString("postSubCatId", "");
        bundle.putString("postSubCatName", "");
        bundle.putString("type", "TREND");
        BookListFragment bookListFragment = new BookListFragment();
        bookListFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().add(R.id.frameMain, bookListFragment, "")
                .commitAllowingStateLoss();
    }
}

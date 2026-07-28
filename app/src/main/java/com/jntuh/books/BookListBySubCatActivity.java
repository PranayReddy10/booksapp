package com.jntuh.books;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import com.jntuh.books.databinding.ActivityBookListBinding;
import com.jntuh.fragment.BookGridFragment;
import com.jntuh.fragment.BookListFragment;
import com.jntuh.fragment.FilterBookFragment;
import com.jntuh.util.BannerAds;
import com.jntuh.util.Method;


public class BookListBySubCatActivity extends AppCompatActivity {

    ActivityBookListBinding viewBookListSubCat;
    String strSubCatId, strSubCatName, strType;
    Method method;
    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBookListSubCat = ActivityBookListBinding.inflate(getLayoutInflater());
        setContentView(viewBookListSubCat.getRoot());

        method = new Method(BookListBySubCatActivity.this);
        method.forceRTLIfSupported();
        fragmentManager = getSupportFragmentManager();

        Intent intent = getIntent();
        strSubCatId = intent.getStringExtra("postSubCatId");
        strSubCatName = intent.getStringExtra("postSubCatName");
        strType = intent.getStringExtra("type");

        if (strType.equals("SearchHome")) {
            viewBookListSubCat.toolbarMain.tvToolbarTitle.setText(getString(R.string.search_title));
            viewBookListSubCat.toolbarMain.ivSearch.setVisibility(View.GONE);
            viewBookListSubCat.toolbarMain.imageFilter.setVisibility(View.GONE);
        } else {
            viewBookListSubCat.toolbarMain.tvToolbarTitle.setText(strSubCatName);
            viewBookListSubCat.toolbarMain.ivSearch.setVisibility(View.VISIBLE);
            viewBookListSubCat.toolbarMain.imageFilter.setVisibility(View.VISIBLE);
        }

        viewBookListSubCat.toolbarMain.ivSearch.setOnClickListener(v -> {
            Intent intentSearch = new Intent(BookListBySubCatActivity.this, SearchBookActivity.class);
            startActivity(intentSearch);
        });

        viewBookListSubCat.toolbarMain.imageFilter.setOnClickListener(v -> {
            FilterBookFragment filterBookFragment = new FilterBookFragment();
            filterBookFragment.show(getSupportFragmentManager(), filterBookFragment.getTag());
        });

        viewBookListSubCat.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        viewBookListSubCat.ivViewList.setOnClickListener(v -> {
            viewBookListSubCat.ivViewList.setColorFilter(getResources().getColor(R.color.icon_view_select), PorterDuff.Mode.SRC_IN);
            viewBookListSubCat.ivViewGrid.setColorFilter(getResources().getColor(R.color.icon_view_normal), PorterDuff.Mode.SRC_IN);
            goList();
        });

        viewBookListSubCat.ivViewGrid.setOnClickListener(v -> {
            viewBookListSubCat.ivViewList.setColorFilter(getResources().getColor(R.color.icon_view_normal), PorterDuff.Mode.SRC_IN);
            viewBookListSubCat.ivViewGrid.setColorFilter(getResources().getColor(R.color.icon_view_select), PorterDuff.Mode.SRC_IN);
            goGrid();
        });
        goGrid();
        BannerAds.showBannerAds(BookListBySubCatActivity.this, viewBookListSubCat.layoutAds);
    }

    private void goGrid() {
        Bundle bundle = new Bundle();
        bundle.putString("postSubCatId", strSubCatId);
        bundle.putString("postSubCatName", strSubCatName);
        bundle.putString("type", strType);
        BookGridFragment bookGridFragment = new BookGridFragment();
        bookGridFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().add(R.id.frameMain, bookGridFragment, "")
                .commitAllowingStateLoss();
    }

    private void goList() {
        Bundle bundle = new Bundle();
        bundle.putString("postSubCatId", strSubCatId);
        bundle.putString("postSubCatName", strSubCatName);
        bundle.putString("type", strType);
        BookListFragment bookListFragment = new BookListFragment();
        bookListFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().add(R.id.frameMain, bookListFragment, "")
                .commitAllowingStateLoss();
    }

}

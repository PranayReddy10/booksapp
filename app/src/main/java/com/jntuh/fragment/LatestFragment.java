package com.jntuh.fragment;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jntuh.books.R;
import com.jntuh.books.SearchBookActivity;
import com.jntuh.books.databinding.FragmentLatestBinding;
import com.jntuh.util.Method;
import com.jntuh.util.StatusBarUtil;

public class LatestFragment extends Fragment {

    FragmentLatestBinding viewLatest;
    Method method;
    FragmentManager fragmentManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        StatusBarUtil.setStatusBar(requireActivity(), "");

        viewLatest = FragmentLatestBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());
        fragmentManager = getChildFragmentManager();

        viewLatest.toolbarMain.imageArrowBack.setVisibility(View.GONE);
        viewLatest.toolbarMain.tvToolbarTitle.setText(getString(R.string.latest_title));
        viewLatest.toolbarMain.ivSearch.setVisibility(View.VISIBLE);
        viewLatest.toolbarMain.imageFilter.setVisibility(View.VISIBLE);
        viewLatest.toolbarMain.imageFilter.setOnClickListener(v -> {
            FilterBookFragment filterBookFragment = new FilterBookFragment();
            filterBookFragment.show(requireActivity().getSupportFragmentManager(), filterBookFragment.getTag());
        });
        viewLatest.toolbarMain.ivSearch.setOnClickListener(v -> {
            Intent intentSearch = new Intent(requireActivity(), SearchBookActivity.class);
            startActivity(intentSearch);
        });

        viewLatest.ivViewGrid.setOnClickListener(v -> {
            viewLatest.ivViewList.setColorFilter(getResources().getColor(R.color.icon_view_normal), PorterDuff.Mode.SRC_IN);
            viewLatest.ivViewGrid.setColorFilter(getResources().getColor(R.color.icon_view_select), PorterDuff.Mode.SRC_IN);
            if (method.isNetworkAvailable()) {
                goGrid();
            } else {
                method.alertBox(getString(R.string.internet_connection));
            }
        });

        viewLatest.ivViewList.setOnClickListener(v -> {
            viewLatest.ivViewList.setColorFilter(getResources().getColor(R.color.icon_view_select), PorterDuff.Mode.SRC_IN);
            viewLatest.ivViewGrid.setColorFilter(getResources().getColor(R.color.icon_view_normal), PorterDuff.Mode.SRC_IN);
            if (method.isNetworkAvailable()) {
                goList();
            } else {
                method.alertBox(getString(R.string.internet_connection));
            }
        });

        if (method.isNetworkAvailable()) {
            goGrid();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }

        return viewLatest.getRoot();
    }

    private void goGrid() {
        Bundle bundle = new Bundle();
        bundle.putString("postSubCatId", "");
        bundle.putString("postSubCatName", "");
        bundle.putString("type", "LATEST");
        BookGridFragment bookGridFragment = new BookGridFragment();
        bookGridFragment.setArguments(bundle);
        fragmentManager.beginTransaction().add(R.id.frameMainLatest, bookGridFragment, "")
                .commitAllowingStateLoss();
    }

    private void goList() {
        Bundle bundle = new Bundle();
        bundle.putString("postSubCatId", "");
        bundle.putString("postSubCatName", "");
        bundle.putString("type", "LATEST");
        BookListFragment bookListFragment = new BookListFragment();
        bookListFragment.setArguments(bundle);
        fragmentManager.beginTransaction().add(R.id.frameMainLatest, bookListFragment, "")
                .commitAllowingStateLoss();
    }
}

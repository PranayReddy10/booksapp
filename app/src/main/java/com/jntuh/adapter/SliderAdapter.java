package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.jntuh.books.R;
import com.jntuh.books.databinding.RowSliderHomeBinding;
import com.jntuh.item.FeaturedBook;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.EnchantedViewPager;
import com.jntuh.util.OnClick;

import java.util.List;


public class SliderAdapter extends PagerAdapter {

    Activity activity;
    List<FeaturedBook> sliderLists;
    LayoutInflater inflater;
    RowSliderHomeBinding viewAdapterSlider;
    OnClick onClick;

    public SliderAdapter(Activity activity, List<FeaturedBook> sliderLists) {
        this.activity = activity;
        this.sliderLists = sliderLists;
        inflater = activity.getLayoutInflater();
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        viewAdapterSlider = RowSliderHomeBinding.inflate(inflater, container, false);
        View view = viewAdapterSlider.getRoot();

        com.jntuh.util.CoverHelper.bind(viewAdapterSlider.ivHomeFeature,
                sliderLists.get(position).getPostImage(),
                sliderLists.get(position).getPostTitle(), null);

        viewAdapterSlider.tvBookName.setText(sliderLists.get(position).getPostTitle());
        viewAdapterSlider.tvAuthor.setText(activity.getString(R.string.by_author, sliderLists.get(position).getAuthorName().isEmpty()?"":sliderLists.get(position).getAuthorName()));

        viewAdapterSlider.cvFeature.setOnClickListener(v -> AdInterstitialAds.ShowInterstitialAds(activity,position,onClick));


        view.setTag(EnchantedViewPager.ENCHANTED_VIEWPAGER_POSITION + position);
        container.addView(view, 0);
        return view;

    }

    @Override
    public int getCount() {
        return sliderLists.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view.equals(object);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        (container).removeView((View) object);
    }
}


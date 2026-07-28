package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowTrendHomeBinding;
import com.jntuh.item.HomeContent;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.Constant;
import com.jntuh.util.Method;
import com.jntuh.util.OnClick;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TrendingHomeAdapter extends RecyclerView.Adapter<TrendingHomeAdapter.ViewHolder> {

    Method method;
    Activity activity;
    List<HomeContent> homeContentsTrend;
    OnClick onClick;

    public TrendingHomeAdapter(Activity activity, List<HomeContent> homeContentsTrend) {
        this.activity = activity;
        this.homeContentsTrend = homeContentsTrend;
        method = new Method(activity, onClick);
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowTrendHomeBinding.inflate(activity.getLayoutInflater()));
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, final int position) {

        HomeContent homeContentTrendPos = homeContentsTrend.get(position);
        holder.rowTrendHomeBinding.tvHomeBookName.setText(homeContentTrendPos.getPostTitle());
        if (!homeContentTrendPos.getPostImage().equals("")) {
            Glide.with(activity.getApplicationContext()).load(homeContentTrendPos.getPostImage())
                    .placeholder(R.drawable.placeholder_portable)
                    .into(holder.rowTrendHomeBinding.ivHomePopular);
        }
        if (homeContentTrendPos.getBook_on_rent().equals("1")) {
            holder.rowTrendHomeBinding.llPremium.setVisibility(View.VISIBLE);
            holder.rowTrendHomeBinding.tvPremium.setText(activity.getString(R.string.rent));
            holder.rowTrendHomeBinding.imageLock.setImageResource(R.drawable.ic_rent);
            holder.rowTrendHomeBinding.tvHomeBookPrice.setText(activity.getString(R.string.currency_code, Constant.constantCurrency, homeContentTrendPos.getBook_rent_price()));
            holder.rowTrendHomeBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.paid_detail_box);
            holder.rowTrendHomeBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.white));
        } else if (homeContentTrendPos.getPost_access().equals("Paid")) {
            holder.rowTrendHomeBinding.llPremium.setVisibility(View.VISIBLE);
            holder.rowTrendHomeBinding.tvPremium.setText(activity.getString(R.string.lbl_premium));
            holder.rowTrendHomeBinding.imageLock.setImageResource(R.drawable.img_premium_lock);
            holder.rowTrendHomeBinding.tvHomeBookPrice.setText(activity.getString(R.string.lbl_paid));
            holder.rowTrendHomeBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.paid_detail_box);
            holder.rowTrendHomeBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.white));
        } else {
            holder.rowTrendHomeBinding.llPremium.setVisibility(View.GONE);
            holder.rowTrendHomeBinding.tvHomeBookPrice.setText(activity.getString(R.string.lbl_free));
            holder.rowTrendHomeBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.free_box);
            holder.rowTrendHomeBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.free_box_text));
        }
        holder.rowTrendHomeBinding.tvHomeBookName.setText(homeContentTrendPos.getPostTitle());
        holder.rowTrendHomeBinding.tvHomeBookStar.setText(homeContentTrendPos.getTotal_rate());
        holder.rowTrendHomeBinding.tvHomeByAuthor.setText(activity.getString(R.string.by_author,homeContentTrendPos.getAuthor_list().isEmpty()?"": homeContentTrendPos.getAuthor_list().get(0).getAuthor_name()));

        holder.rowTrendHomeBinding.llHomeBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdInterstitialAds.ShowInterstitialAds(activity,holder.getBindingAdapterPosition(),onClick);
            }
        });
    }

    @Override
    public int getItemCount() {
        return homeContentsTrend.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        RowTrendHomeBinding rowTrendHomeBinding;

        public ViewHolder(RowTrendHomeBinding rowTrendHomeBinding) {
            super(rowTrendHomeBinding.getRoot());
            this.rowTrendHomeBinding = rowTrendHomeBinding;
        }
    }

}

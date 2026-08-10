package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.books.databinding.RowContinueHomeBinding;
import com.jntuh.item.HomeContent;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.OnClick;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ContinueHomeAdapter extends RecyclerView.Adapter<ContinueHomeAdapter.ViewHolder> {

    Activity activity;
    List<HomeContent> homeContentsContinue;
    OnClick onClick;

    public ContinueHomeAdapter(Activity activity, List<HomeContent> homeContentsContinue) {
        this.activity = activity;
        this.homeContentsContinue = homeContentsContinue;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowContinueHomeBinding.inflate(activity.getLayoutInflater()));
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, final int position) {

         holder.rowContinueHomeBinding.tvHomeConTitle.setText(homeContentsContinue.get(position).getPostTitle());


        com.jntuh.util.CoverHelper.bind(holder.rowContinueHomeBinding.ivHomeCont,
                homeContentsContinue.get(position).getPostImage(),
                homeContentsContinue.get(position).getPostTitle(), null);

        holder.rowContinueHomeBinding.llContinueHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdInterstitialAds.ShowInterstitialAds(activity,holder.getBindingAdapterPosition(),onClick);
            }
        });

    }

    @Override
    public int getItemCount() {
        return homeContentsContinue.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

       RowContinueHomeBinding rowContinueHomeBinding;

        public ViewHolder(RowContinueHomeBinding rowContinueHomeBinding) {
            super(rowContinueHomeBinding.getRoot());
          this.rowContinueHomeBinding=rowContinueHomeBinding;
        }
    }

}

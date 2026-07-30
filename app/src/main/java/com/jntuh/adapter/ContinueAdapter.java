package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.util.CoverHelper;
import com.jntuh.books.databinding.RowFavoriteBinding;
import com.jntuh.item.SubCatListBook;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.Method;
import com.jntuh.util.OnClick;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ContinueAdapter extends RecyclerView.Adapter<ContinueAdapter.ViewHolder> {

    Activity activity;
    List<SubCatListBook> favListBookList;
    OnClick onClick;
    int columnWidth;
    Method method;

    public ContinueAdapter(Activity activity, List<SubCatListBook> favListBookList) {
        this.activity = activity;
        this.favListBookList = favListBookList;
        method = new Method(activity);
        Resources r = activity.getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, r.getDisplayMetrics());
        columnWidth = (int) ((method.getScreenWidth() - ((3 + 1) * padding)));
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowFavoriteBinding.inflate(activity.getLayoutInflater()));
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, final int position) {


        holder.rowFavoriteBinding.mcvFav.setVisibility(View.GONE);
        holder.rowFavoriteBinding.llHomeBook.setLayoutParams(new LinearLayout.LayoutParams(columnWidth / 3, columnWidth / 2));
        holder.rowFavoriteBinding.tvHomeConTitle.setText(favListBookList.get(position).getPost_title());

         holder.rowFavoriteBinding.ivHomeCont.post(() ->
                CoverHelper.bind(holder.rowFavoriteBinding.ivHomeCont,
                        favListBookList.get(position).getPost_image(),
                        favListBookList.get(position).getPost_title(),
                        favListBookList.get(position).getCover_color()));

        holder.rowFavoriteBinding.rlFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdInterstitialAds.ShowInterstitialAds(activity,holder.getBindingAdapterPosition(),onClick);
            }
        });


    }

    @Override
    public int getItemCount() {
        return favListBookList.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        RowFavoriteBinding rowFavoriteBinding;

        public ViewHolder(RowFavoriteBinding rowFavoriteBinding) {
            super(rowFavoriteBinding.getRoot());
            this.rowFavoriteBinding = rowFavoriteBinding;
        }
    }

}

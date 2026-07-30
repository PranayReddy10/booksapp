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
import com.jntuh.books.databinding.RowRentBookBinding;
import com.jntuh.item.SubCatListBook;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.Method;
import com.jntuh.util.OnClick;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RentAdapter extends RecyclerView.Adapter<RentAdapter.ViewHolder> {

    Activity activity;
    List<SubCatListBook> favListBookList;
    OnClick onClick;
    int columnWidth;
    Method method;

    public RentAdapter(Activity activity, List<SubCatListBook> favListBookList) {
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
        return new ViewHolder(RowRentBookBinding.inflate(activity.getLayoutInflater()));
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, final int position) {

        SubCatListBook favListBookListPis=favListBookList.get(position);
        holder.rowRentBookBinding.llHomeBook.setLayoutParams(new LinearLayout.LayoutParams(columnWidth / 3, columnWidth / 2));
        holder.rowRentBookBinding.tvHomeConTitle.setText(favListBookListPis.getPost_title());
        holder.rowRentBookBinding.tvExp.setText(favListBookListPis.getRent_exp_date());

         holder.rowRentBookBinding.ivHomeCont.post(() ->
                CoverHelper.bind(holder.rowRentBookBinding.ivHomeCont,
                        favListBookListPis.getPost_image(),
                        favListBookListPis.getPost_title(),
                        favListBookListPis.getCover_color()));

        holder.rowRentBookBinding.rlFav.setOnClickListener(new View.OnClickListener() {
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

        RowRentBookBinding rowRentBookBinding;

        public ViewHolder(RowRentBookBinding rowRentBookBinding) {
            super(rowRentBookBinding.getRoot());
            this.rowRentBookBinding = rowRentBookBinding;
        }
    }

}

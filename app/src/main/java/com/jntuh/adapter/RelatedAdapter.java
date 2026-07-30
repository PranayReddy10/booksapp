package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.util.CoverHelper;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowRelatedBinding;
import com.jntuh.item.BookRelatedList;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.Constant;
import com.jntuh.util.OnClick;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RelatedAdapter extends RecyclerView.Adapter<RelatedAdapter.ViewHolder> {

    Activity activity;
    List<BookRelatedList> bookRelatedLists;
    OnClick onClick;

    public RelatedAdapter(Activity activity, List<BookRelatedList> bookRelatedLists) {
        this.activity = activity;
        this.bookRelatedLists = bookRelatedLists;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowRelatedBinding.inflate(activity.getLayoutInflater()));
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, final int position) {

        BookRelatedList bookRelatedListPos=bookRelatedLists.get(position);
        holder.relatedBinding.tvHomeBookName.setText(bookRelatedListPos.getPost_title());
        holder.relatedBinding.ivHomePopular.post(() ->
                CoverHelper.bind(holder.relatedBinding.ivHomePopular,
                        bookRelatedListPos.getPost_image(),
                        bookRelatedListPos.getPost_title(),
                        null));
        if (bookRelatedListPos.getBook_on_rent().equals("1")) {
            holder.relatedBinding.llPremium.setVisibility(View.VISIBLE);
            holder.relatedBinding.tvPremium.setText(activity.getString(R.string.rent));
            holder.relatedBinding.imageLock.setImageResource(R.drawable.ic_rent);
            holder.relatedBinding.tvHomeBookPrice.setText(activity.getString(R.string.currency_code, Constant.constantCurrency, bookRelatedListPos.getBook_rent_price()));
            holder.relatedBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.paid_detail_box);
            holder.relatedBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.white));
        }else if(bookRelatedListPos.getPost_access().equals("Paid")){
            holder.relatedBinding.llPremium.setVisibility(View.VISIBLE);
            holder.relatedBinding.tvPremium.setText(activity.getString(R.string.lbl_premium));
            holder.relatedBinding.imageLock.setImageResource(R.drawable.img_premium_lock);
            holder.relatedBinding.tvHomeBookPrice.setText(activity.getString(R.string.lbl_paid));
            holder.relatedBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.paid_detail_box);
            holder.relatedBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.white));
        } else {
            holder.relatedBinding.llPremium.setVisibility(View.GONE);
            holder.relatedBinding.tvHomeBookPrice.setText(activity.getString(R.string.lbl_free));
            holder.relatedBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.free_box);
            holder.relatedBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.free_box_text));
        }
        holder.relatedBinding.tvHomeBookName.setText(bookRelatedListPos.getPost_title());
        holder.relatedBinding.tvHomeBookStar.setText(bookRelatedListPos.getTotal_rate());
        holder.relatedBinding.tvHomeByAuthor.setText(activity.getString(R.string.by_author,bookRelatedListPos.getListRelatedAuthor().isEmpty()?"":bookRelatedListPos.getListRelatedAuthor().get(0).getAuthor_name()));
        holder.relatedBinding.llHomeBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdInterstitialAds.ShowInterstitialAds(activity,holder.getBindingAdapterPosition(),onClick);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookRelatedLists.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        RowRelatedBinding relatedBinding;

        public ViewHolder(RowRelatedBinding relatedBinding) {
            super(relatedBinding.getRoot());
            this.relatedBinding = relatedBinding;
        }
    }

}

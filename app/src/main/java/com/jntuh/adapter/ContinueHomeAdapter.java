package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.books.R;
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

        HomeContent item = homeContentsContinue.get(position);

        holder.rowContinueHomeBinding.tvHomeConTitle.setText(item.getPostTitle());

        // Author line, when the book carries one.
        if (item.getAuthor_list() != null && !item.getAuthor_list().isEmpty()) {
            holder.rowContinueHomeBinding.tvHomeConAuthor.setVisibility(View.VISIBLE);
            holder.rowContinueHomeBinding.tvHomeConAuthor.setText(
                    activity.getString(R.string.by_author, item.getAuthor_list().get(0).getAuthor_name()));
        } else {
            holder.rowContinueHomeBinding.tvHomeConAuthor.setVisibility(View.GONE);
        }

        // Where the reader stopped. The feed carries the page reached but not the
        // book's length, so this is a page marker rather than a percentage.
        String page = item.getPage_num();
        if (page != null && !page.trim().isEmpty()) {
            holder.rowContinueHomeBinding.tvHomeConPage.setVisibility(View.VISIBLE);
            holder.rowContinueHomeBinding.tvHomeConPage.setText(
                    activity.getString(R.string.lbl_page_num, page.trim()));
        } else {
            holder.rowContinueHomeBinding.tvHomeConPage.setVisibility(View.INVISIBLE);
        }

        com.jntuh.util.CoverHelper.bind(holder.rowContinueHomeBinding.ivHomeCont,
                item.getPostImage(), item.getPostTitle(), null);

        View.OnClickListener open = v ->
                AdInterstitialAds.ShowInterstitialAds(activity, holder.getBindingAdapterPosition(), onClick);
        holder.rowContinueHomeBinding.llContinueHome.setOnClickListener(open);
        holder.rowContinueHomeBinding.btnHomeConResume.setOnClickListener(open);

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

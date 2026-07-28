package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowAuthorHomeBinding;
import com.jntuh.books.databinding.RowCatHomeBinding;
import com.jntuh.books.databinding.RowTrendHomeBinding;
import com.jntuh.item.HomeContent;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.Constant;
import com.jntuh.util.OnClick;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HomeContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Activity activity;
    String type;
    List<HomeContent> homeContents;
    private final int VIEW_TYPE_CAT = 1;
    private final int VIEW_TYPE_SUB_CAT = 2;
    private final int VIEW_TYPE_BOOK = 3;
    private final int VIEW_TYPE_AUTHOR = 4;
    OnClick onClick;

    public HomeContentAdapter(Activity activity, List<HomeContent> homeContents, String type) {
        this.activity = activity;
        this.homeContents = homeContents;
        this.type = type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CAT) {
            return new CategoryHolder(RowCatHomeBinding.inflate(activity.getLayoutInflater()));
        } else if (viewType == VIEW_TYPE_SUB_CAT) {
            return new SubCategoryHolder(RowCatHomeBinding.inflate(activity.getLayoutInflater()));
        } else if (viewType == VIEW_TYPE_BOOK) {
            return new BookHolder(RowTrendHomeBinding.inflate(activity.getLayoutInflater()));
        } else if (viewType == VIEW_TYPE_AUTHOR) {
            return new AuthorHolder(RowAuthorHomeBinding.inflate(activity.getLayoutInflater()));
        }
        return null;
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onBindViewHolder(@NotNull RecyclerView.ViewHolder holder, final int position) {

        if (holder.getItemViewType() == VIEW_TYPE_CAT) {
            CategoryHolder categoryHolder = (CategoryHolder) holder;
            categoryHolder.rowCatHomeBinding.tvHomeCat.setText(homeContents.get(position).getPostTitle());
            if (!homeContents.get(position).getPostImage().equals("")) {
                Glide.with(activity.getApplicationContext()).load(homeContents.get(position).getPostImage())
                        .placeholder(R.drawable.placeholder_landscape)
                        .into(categoryHolder.rowCatHomeBinding.ivHomeCat);
            }
            categoryHolder.rowCatHomeBinding.flHomeCat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdInterstitialAds.ShowInterstitialAds(activity,categoryHolder.getBindingAdapterPosition(),onClick);
                }
            });
        } else if (holder.getItemViewType() == VIEW_TYPE_SUB_CAT) {
            SubCategoryHolder subCategoryHolder = (SubCategoryHolder) holder;
            subCategoryHolder.rowSubCatHomeBinding.tvHomeCat.setText(homeContents.get(position).getPostTitle());
            if (!homeContents.get(position).getPostImage().equals("")) {
                Glide.with(activity.getApplicationContext()).load(homeContents.get(position).getPostImage())
                        .placeholder(R.drawable.placeholder_landscape)
                        .into(subCategoryHolder.rowSubCatHomeBinding.ivHomeCat);
            }

            subCategoryHolder.rowSubCatHomeBinding.flHomeCat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdInterstitialAds.ShowInterstitialAds(activity,subCategoryHolder.getBindingAdapterPosition(),onClick);
                }
            });

        } else if (holder.getItemViewType() == VIEW_TYPE_BOOK) {
            BookHolder bookHolder = (BookHolder) holder;
            HomeContent homeContentPos=homeContents.get(position);
            bookHolder.rowTrendHomeBinding.tvHomeBookName.setText(homeContentPos.getPostTitle());
            if (!homeContentPos.getPostImage().equals("")) {
                Glide.with(activity.getApplicationContext()).load(homeContentPos.getPostImage())
                        .placeholder(R.drawable.placeholder_portable)
                        .into(bookHolder.rowTrendHomeBinding.ivHomePopular);
            }
            if (homeContentPos.getBook_on_rent().equals("1")) {
                bookHolder.rowTrendHomeBinding.llPremium.setVisibility(View.VISIBLE);
                bookHolder.rowTrendHomeBinding.tvPremium.setText(activity.getString(R.string.rent));
                bookHolder.rowTrendHomeBinding.imageLock.setImageResource(R.drawable.ic_rent);
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setText(activity.getString(R.string.currency_code, Constant.constantCurrency,homeContentPos.getBook_rent_price()));
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.paid_detail_box);
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.white));
            }else if(homeContentPos.getPost_access().equals("Paid")){
                bookHolder.rowTrendHomeBinding.llPremium.setVisibility(View.VISIBLE);
                bookHolder.rowTrendHomeBinding.tvPremium.setText(activity.getString(R.string.lbl_premium));
                bookHolder.rowTrendHomeBinding.imageLock.setImageResource(R.drawable.img_premium_lock);
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setText(activity.getString(R.string.lbl_paid));
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.paid_detail_box);
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.white));
            } else {
                bookHolder.rowTrendHomeBinding.llPremium.setVisibility(View.GONE);
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setText(activity.getString(R.string.lbl_free));
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setBackgroundResource(R.drawable.free_box);
                bookHolder.rowTrendHomeBinding.tvHomeBookPrice.setTextColor(ContextCompat.getColor(activity, R.color.free_box_text));
            }
            bookHolder.rowTrendHomeBinding.tvHomeBookName.setText(homeContentPos.getPostTitle());
            bookHolder.rowTrendHomeBinding.tvHomeBookStar.setText(homeContentPos.getTotal_rate());
            bookHolder.rowTrendHomeBinding.tvHomeByAuthor.setText(activity.getString(R.string.by_author, homeContentPos.getAuthor_list().isEmpty()?"":homeContentPos.getAuthor_list().get(0).getAuthor_name()));

            bookHolder.rowTrendHomeBinding.llHomeBook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdInterstitialAds.ShowInterstitialAds(activity,bookHolder.getBindingAdapterPosition(),onClick);
                }
            });

        } else if (holder.getItemViewType() == VIEW_TYPE_AUTHOR) {
            AuthorHolder authorHolder = (AuthorHolder) holder;
            authorHolder.rowAuthorHomeBinding.tvHomeAuthorName.setText(homeContents.get(position).getPostTitle());
            if (!homeContents.get(position).getPostImage().equals("")) {
                Glide.with(activity.getApplicationContext()).load(homeContents.get(position).getPostImage())
                        .placeholder(R.drawable.placeholder_author)
                        .into(authorHolder.rowAuthorHomeBinding.ivHomeAuthor);
            }

            authorHolder.rowAuthorHomeBinding.llHomeAuthor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdInterstitialAds.ShowInterstitialAds(activity,authorHolder.getBindingAdapterPosition(),onClick);
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return homeContents.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    @Override
    public int getItemViewType(int position) {
        switch (homeContents.get(position).getPostType()) {
            case "book":
            default:
                return VIEW_TYPE_BOOK;
            case "author":
                return VIEW_TYPE_AUTHOR;
            case "category":
                return VIEW_TYPE_CAT;
            case "subcategory":
                return VIEW_TYPE_SUB_CAT;
        }
    }

    public class BookHolder extends RecyclerView.ViewHolder {

        RowTrendHomeBinding rowTrendHomeBinding;

        public BookHolder(RowTrendHomeBinding rowTrendHomeBinding) {
            super(rowTrendHomeBinding.getRoot());
            this.rowTrendHomeBinding = rowTrendHomeBinding;
        }
    }

    public class CategoryHolder extends RecyclerView.ViewHolder {

        RowCatHomeBinding rowCatHomeBinding;

        public CategoryHolder(RowCatHomeBinding rowCatHomeBinding) {
            super(rowCatHomeBinding.getRoot());
            this.rowCatHomeBinding = rowCatHomeBinding;
        }
    }

    public class SubCategoryHolder extends RecyclerView.ViewHolder {

        RowCatHomeBinding rowSubCatHomeBinding;

        public SubCategoryHolder(RowCatHomeBinding rowSubCatHomeBinding) {
            super(rowSubCatHomeBinding.getRoot());
            this.rowSubCatHomeBinding = rowSubCatHomeBinding;
        }
    }

    public class AuthorHolder extends RecyclerView.ViewHolder {

        RowAuthorHomeBinding rowAuthorHomeBinding;

        public AuthorHolder(RowAuthorHomeBinding rowAuthorHomeBinding) {
            super(rowAuthorHomeBinding.getRoot());
            this.rowAuthorHomeBinding = rowAuthorHomeBinding;
        }
    }

}

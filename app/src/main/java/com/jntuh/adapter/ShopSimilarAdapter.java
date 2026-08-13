package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowShopSimilarBinding;
import com.jntuh.item.ShopProduct;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Horizontal "Similar products" cards on the product detail screen. */
public class ShopSimilarAdapter extends RecyclerView.Adapter<ShopSimilarAdapter.VH> {

    public interface OnSimilarClick {
        void onSimilarClick(ShopProduct product);
    }

    private final Activity activity;
    private final List<ShopProduct> items;
    private final OnSimilarClick listener;

    public ShopSimilarAdapter(Activity activity, List<ShopProduct> items, OnSimilarClick listener) {
        this.activity = activity;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new VH(RowShopSimilarBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull VH holder, int position) {
        ShopProduct p = items.get(position);
        holder.binding.tvSimilarName.setText(p.getName());
        holder.binding.tvSimilarPrice.setText(p.getCurrency() + (p.getPrice() == null ? "" : p.getPrice()));
        Glide.with(activity.getApplicationContext())
                .load(p.getThumb())
                .placeholder(R.color.card_view_bg)
                .into(holder.binding.ivSimilar);
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onSimilarClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final RowShopSimilarBinding binding;
        VH(RowShopSimilarBinding b) { super(b.getRoot()); this.binding = b; }
    }
}

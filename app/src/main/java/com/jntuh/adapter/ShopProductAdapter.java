package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowShopProductBinding;
import com.jntuh.item.ShopProduct;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Product grid tile for the Shop tab (2-column staggered grid). Shows image,
 * name, sale price + struck-through regular price, and a discount badge.
 * Tapping opens the product detail screen.
 */
public class ShopProductAdapter extends RecyclerView.Adapter<ShopProductAdapter.VH> {

    public interface OnProductClick {
        void onProductClick(ShopProduct product);
    }

    private final Activity activity;
    private final List<ShopProduct> items;
    private final OnProductClick listener;

    public ShopProductAdapter(Activity activity, List<ShopProduct> items, OnProductClick listener) {
        this.activity = activity;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new VH(RowShopProductBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull VH holder, int position) {
        ShopProduct p = items.get(position);

        holder.binding.tvProductName.setText(p.getName());

        String cur = p.getCurrency();
        holder.binding.tvPrice.setText(cur + (p.getPrice() == null ? "" : p.getPrice()));

        // Struck-through regular price only when on sale and it differs.
        if (p.isOnSale() && p.getRegular_price() != null && !p.getRegular_price().isEmpty()
                && !p.getRegular_price().equals(p.getPrice())) {
            holder.binding.tvRegularPrice.setVisibility(View.VISIBLE);
            holder.binding.tvRegularPrice.setText(cur + p.getRegular_price());
            holder.binding.tvRegularPrice.setPaintFlags(
                    holder.binding.tvRegularPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.binding.tvRegularPrice.setVisibility(View.GONE);
        }

        // Discount badge.
        if (p.getDiscount() != null && !p.getDiscount().isEmpty()) {
            holder.binding.tvDiscount.setVisibility(View.VISIBLE);
            holder.binding.tvDiscount.setText(p.getDiscount());
        } else {
            holder.binding.tvDiscount.setVisibility(View.GONE);
        }

        Glide.with(activity.getApplicationContext())
                .load(p.getThumb())
                .placeholder(R.color.card_view_bg)
                .into(holder.binding.ivProduct);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final RowShopProductBinding binding;
        VH(RowShopProductBinding b) { super(b.getRoot()); this.binding = b; }
    }
}

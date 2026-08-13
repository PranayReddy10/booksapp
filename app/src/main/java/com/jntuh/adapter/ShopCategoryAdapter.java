package com.jntuh.adapter;

import android.app.Activity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.books.R;
import com.jntuh.books.databinding.RowShopCategoryBinding;
import com.jntuh.item.ShopCategory;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Horizontal category chips for the Shop tab. The first chip ("All") is a
 * synthetic entry with a null id. Selecting a chip filters the product grid.
 */
public class ShopCategoryAdapter extends RecyclerView.Adapter<ShopCategoryAdapter.VH> {

    public interface OnCategoryClick {
        void onCategoryClick(ShopCategory category, int position);
    }

    private final Activity activity;
    private final List<ShopCategory> items;
    private final OnCategoryClick listener;
    private int selectedPos = 0;

    public ShopCategoryAdapter(Activity activity, List<ShopCategory> items, OnCategoryClick listener) {
        this.activity = activity;
        this.items = items;
        this.listener = listener;
    }

    public void setSelected(int pos) {
        int old = selectedPos;
        selectedPos = pos;
        notifyItemChanged(old);
        notifyItemChanged(selectedPos);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new VH(RowShopCategoryBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NotNull VH holder, int position) {
        ShopCategory c = items.get(position);
        holder.binding.tvChip.setText(c.getName());

        boolean selected = position == selectedPos;
        holder.binding.tvChip.setBackgroundResource(
                selected ? R.drawable.bg_shop_chip_selected : R.drawable.bg_shop_chip_normal);
        holder.binding.tvChip.setTextColor(ContextCompat.getColor(activity,
                selected ? R.color.white : R.color.gray));

        holder.binding.tvChip.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(c, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final RowShopCategoryBinding binding;
        VH(RowShopCategoryBinding b) { super(b.getRoot()); this.binding = b; }
    }
}

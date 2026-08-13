package com.jntuh.adapter;

import android.app.Activity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowShopImageBinding;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Simple full-width image pager for the product detail carousel. */
public class ShopImageAdapter extends RecyclerView.Adapter<ShopImageAdapter.VH> {

    private final Activity activity;
    private final List<String> images;

    public ShopImageAdapter(Activity activity, List<String> images) {
        this.activity = activity;
        this.images = images;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new VH(RowShopImageBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NotNull VH holder, int position) {
        Glide.with(activity.getApplicationContext())
                .load(images.get(position))
                .placeholder(R.color.card_view_bg)
                .into(holder.binding.ivPage);
    }

    @Override
    public int getItemCount() {
        return images == null ? 0 : images.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final RowShopImageBinding binding;
        VH(RowShopImageBinding b) { super(b.getRoot()); this.binding = b; }
    }
}

package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowMediaTileBinding;
import com.jntuh.item.MediaItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Explore grid tile. Photos/videos shown as cropped thumbnails with a type badge
 * and (when enabled) a view count, matching the reference Search/Explore screen.
 * Tapping a tile opens the full-screen vertical feed focused on that position.
 */
public class MediaTileAdapter extends RecyclerView.Adapter<MediaTileAdapter.ViewHolder> {

    public interface OnTileClickListener {
        void onTileClick(int position);
    }

    private final Activity activity;
    private final List<MediaItem> items;
    private final OnTileClickListener listener;

    // A few heights (in dp) to give the grid a staggered, Pinterest-like rhythm.
    private static final int[] TILE_HEIGHTS = {180, 220, 260, 200, 240};

    public MediaTileAdapter(Activity activity, List<MediaItem> items, OnTileClickListener listener) {
        this.activity = activity;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowMediaTileBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        MediaItem item = items.get(position);

        // Staggered height, stable per position.
        int hDp = TILE_HEIGHTS[Math.abs(position) % TILE_HEIGHTS.length];
        int hPx = Math.round(activity.getResources().getDisplayMetrics().density * hDp);
        ViewGroup.LayoutParams lp = holder.binding.ivTile.getLayoutParams();
        lp.height = hPx;
        holder.binding.ivTile.setLayoutParams(lp);

        boolean isVideo = "video".equalsIgnoreCase(item.getMedia_type());
        // Prefer thumb_url (esp. for videos); fall back to the file itself.
        String thumb = item.getThumb_url();
        String load = (thumb != null && !thumb.isEmpty()) ? thumb : item.getFile_url();
        Glide.with(activity.getApplicationContext())
                .load(load)
                .placeholder(R.color.card_view_bg)
                .into(holder.binding.ivTile);

        // Type badge glyph.
        holder.binding.ivTileBadge.setImageResource(
                isVideo ? android.R.drawable.ic_media_play : android.R.drawable.ic_menu_gallery);

        // View count only when the post allows it.
        if ("1".equals(item.getShow_views())) {
            holder.binding.llTileViews.setVisibility(View.VISIBLE);
            holder.binding.tvTileViews.setText(safeNum(item.getView_count()));
        } else {
            holder.binding.llTileViews.setVisibility(View.GONE);
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onTileClick(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safeNum(String s) {
        return (s == null || s.isEmpty()) ? "0" : s;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowMediaTileBinding binding;

        ViewHolder(RowMediaTileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

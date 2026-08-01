package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowMyMediaBinding;
import com.jntuh.item.MyMediaItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MyMediaAdapter extends RecyclerView.Adapter<MyMediaAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(MyMediaItem item, int position);
        void onOpen(MyMediaItem item, int position);
    }

    private final Activity activity;
    private final List<MyMediaItem> mediaList;
    private final OnDeleteListener deleteListener;

    public MyMediaAdapter(Activity activity, List<MyMediaItem> mediaList, OnDeleteListener deleteListener) {
        this.activity = activity;
        this.mediaList = mediaList;
        this.deleteListener = deleteListener;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowMyMediaBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        MyMediaItem item = mediaList.get(position);

        holder.binding.tvMediaTitle.setText(item.getTitle());

        boolean isVideo = "video".equalsIgnoreCase(item.getMedia_type());
        holder.binding.ivPlayBadge.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        // Thumbnail: prefer thumb_url (esp. for videos), fall back to the file itself.
        String thumb = item.getThumb_url();
        String load = (thumb != null && !thumb.isEmpty()) ? thumb : item.getFile_url();
        Glide.with(activity.getApplicationContext())
                .load(load)
                .placeholder(R.color.card_view_bg)
                .into(holder.binding.ivMediaThumb);

        // Status chip: label can be "pending"/"approved"/"rejected" or numeric.
        String raw = item.getUpload_status() == null ? "" : item.getUpload_status().trim().toLowerCase();
        String label;
        int colorRes;
        switch (raw) {
            case "approved":
            case "1":
                label = activity.getString(R.string.status_approved);
                colorRes = R.color.status_approved;
                break;
            case "rejected":
            case "2":
                label = activity.getString(R.string.status_rejected);
                colorRes = R.color.status_rejected;
                break;
            default:
                label = activity.getString(R.string.status_pending);
                colorRes = R.color.status_pending;
                break;
        }
        holder.binding.tvMediaStatus.setText(label);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(activity.getResources().getDisplayMetrics().density * 12);
        pill.setColor(ContextCompat.getColor(activity, colorRes));
        holder.binding.tvMediaStatus.setBackground(pill);

        boolean isRejected = colorRes == R.color.status_rejected;
        if (isRejected && item.getReject_reason() != null && !item.getReject_reason().isEmpty()) {
            holder.binding.tvMediaReason.setVisibility(View.VISIBLE);
            holder.binding.tvMediaReason.setText(
                    activity.getString(R.string.reason_prefix) + item.getReject_reason());
        } else {
            holder.binding.tvMediaReason.setVisibility(View.GONE);
        }

        holder.binding.ivMediaDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onDelete(mediaList.get(pos), pos);
            }
        });

        // Tapping the row opens that post in the full-screen feed.
        holder.binding.getRoot().setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onOpen(mediaList.get(pos), pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowMyMediaBinding binding;

        ViewHolder(RowMyMediaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

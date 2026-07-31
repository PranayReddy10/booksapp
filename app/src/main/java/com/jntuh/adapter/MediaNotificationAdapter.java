package com.jntuh.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowMediaNotificationBinding;
import com.jntuh.item.MediaNotificationItem;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MediaNotificationAdapter extends RecyclerView.Adapter<MediaNotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onClick(MediaNotificationItem item, int position);
    }

    private final Activity activity;
    private final List<MediaNotificationItem> notifications;
    private final OnNotificationClickListener listener;

    public MediaNotificationAdapter(Activity activity, List<MediaNotificationItem> notifications,
                                    OnNotificationClickListener listener) {
        this.activity = activity;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowMediaNotificationBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        MediaNotificationItem n = notifications.get(position);

        holder.binding.tvNotifTitle.setText(n.getTitle());
        holder.binding.tvNotifMessage.setText(n.getMessage());
        holder.binding.tvNotifTime.setText(relativeTime(n.getCreated_at()));

        boolean unread = "0".equals(n.getIs_read());
        holder.binding.viewUnread.setVisibility(unread ? View.VISIBLE : View.GONE);
        // Subtle tint for unread rows.
        holder.binding.rowRoot.setBackgroundColor(
                unread ? 0x11FF7A00 : Color.TRANSPARENT);

        String img = n.getImage();
        if (img != null && !img.isEmpty()) {
            Glide.with(activity.getApplicationContext())
                    .load(img)
                    .placeholder(R.color.card_view_bg)
                    .into(holder.binding.ivNotifImage);
        } else {
            holder.binding.ivNotifImage.setImageResource(R.color.card_view_bg);
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onClick(notifications.get(pos), pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String relativeTime(String created) {
        if (created == null || created.isEmpty()) return "";
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date d = fmt.parse(created);
            if (d == null) return "";
            long diff = System.currentTimeMillis() - d.getTime();
            if (diff < 0) diff = 0;
            long min = diff / 60000;
            if (min < 1) return "just now";
            if (min < 60) return min + "m ago";
            long hr = min / 60;
            if (hr < 24) return hr + "h ago";
            long day = hr / 24;
            if (day < 30) return day + "d ago";
            return created.substring(0, 10);
        } catch (Exception e) {
            return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowMediaNotificationBinding binding;

        ViewHolder(RowMediaNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

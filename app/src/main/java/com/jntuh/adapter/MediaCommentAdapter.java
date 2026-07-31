package com.jntuh.adapter;

import android.app.Activity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowMediaCommentBinding;
import com.jntuh.item.MediaCommentItem;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MediaCommentAdapter extends RecyclerView.Adapter<MediaCommentAdapter.ViewHolder> {

    public interface OnCommentLongClickListener {
        void onLongClick(MediaCommentItem item, int position);
    }

    private final Activity activity;
    private final List<MediaCommentItem> comments;
    private final String currentUserId;
    private final OnCommentLongClickListener longClickListener;

    public MediaCommentAdapter(Activity activity, List<MediaCommentItem> comments,
                               String currentUserId, OnCommentLongClickListener longClickListener) {
        this.activity = activity;
        this.comments = comments;
        this.currentUserId = currentUserId == null ? "" : currentUserId;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowMediaCommentBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        MediaCommentItem c = comments.get(position);

        holder.binding.tvCommentUser.setText(c.getUser_name());
        holder.binding.tvCommentText.setText(c.getComment());
        holder.binding.tvCommentTime.setText(shortTime(c.getCreated_at()));

        String img = c.getUser_image();
        if (img != null && !img.isEmpty()) {
            Glide.with(activity.getApplicationContext())
                    .load(img)
                    .placeholder(R.drawable.img_user)
                    .circleCrop()
                    .into(holder.binding.ivCommentAvatar);
        } else {
            holder.binding.ivCommentAvatar.setImageResource(R.drawable.img_user);
        }

        // Long-press to delete, only on the user's own comments.
        boolean isOwn = currentUserId.equals(c.getUser_id());
        holder.binding.getRoot().setOnLongClickListener(v -> {
            if (isOwn && longClickListener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    longClickListener.onLongClick(comments.get(pos), pos);
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private String shortTime(String created) {
        if (created == null || created.isEmpty()) return "";
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date d = fmt.parse(created);
            if (d == null) return "";
            long diff = System.currentTimeMillis() - d.getTime();
            if (diff < 0) diff = 0;
            long min = diff / 60000;
            if (min < 1) return "now";
            if (min < 60) return min + "m";
            long hr = min / 60;
            if (hr < 24) return hr + "h";
            long day = hr / 24;
            if (day < 30) return day + "d";
            return created.substring(0, 10);
        } catch (Exception e) {
            return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowMediaCommentBinding binding;

        ViewHolder(RowMediaCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

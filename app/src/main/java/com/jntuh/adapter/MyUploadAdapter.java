package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.util.CoverHelper;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowMyUploadBinding;
import com.jntuh.item.MyUploadList;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MyUploadAdapter extends RecyclerView.Adapter<MyUploadAdapter.ViewHolder> {

    private final Activity activity;
    private final List<MyUploadList> uploadList;

    public MyUploadAdapter(Activity activity, List<MyUploadList> uploadList) {
        this.activity = activity;
        this.uploadList = uploadList;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowMyUploadBinding.inflate(activity.getLayoutInflater()));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        MyUploadList item = uploadList.get(position);

        holder.binding.tvUploadTitle.setText(item.getPost_title());

        holder.binding.ivUploadCover.post(() ->
                CoverHelper.bind(holder.binding.ivUploadCover,
                        item.getPost_image(),
                        item.getPost_title(),
                        null));

        // Status can arrive as label ("pending"/"approved"/"rejected") or numeric.
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
        holder.binding.tvUploadStatus.setText(label);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(activity.getResources().getDisplayMetrics().density * 12);
        pill.setColor(ContextCompat.getColor(activity, colorRes));
        holder.binding.tvUploadStatus.setBackground(pill);

        boolean isRejected = colorRes == R.color.status_rejected;
        if (isRejected && item.getReject_reason() != null && !item.getReject_reason().isEmpty()) {
            holder.binding.tvUploadReason.setVisibility(View.VISIBLE);
            holder.binding.tvUploadReason.setText(
                    activity.getString(R.string.reason_prefix) + item.getReject_reason());
        } else {
            holder.binding.tvUploadReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return uploadList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowMyUploadBinding binding;

        ViewHolder(RowMyUploadBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

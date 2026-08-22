package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.widget.Toast;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.util.CoverHelper;
import com.jntuh.books.R;
import com.jntuh.books.BookDetailsActivity;
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

        // What this book has done for its uploader. Hidden entirely when coins
        // are off, and while a book is still pending, where it can only be zeros.
        boolean showEarnings = item.getCoins_enabled() == 1
                && "approved".equalsIgnoreCase(item.getUpload_status() == null ? "" : item.getUpload_status());
        holder.binding.tvUploadEarnings.setVisibility(showEarnings ? View.VISIBLE : View.GONE);
        if (showEarnings) {
            holder.binding.tvUploadEarnings.setText(activity.getString(R.string.msg_upload_earnings,
                    java.text.NumberFormat.getInstance().format(item.getTotal_views()),
                    item.getReader_count(),
                    item.getCoins_earned()));
        }

        // Tapping an upload opens the book's detail page — but only approved books are
        // live (books_details returns status=1 only), so guard pending/rejected.
        holder.binding.getRoot().setOnClickListener(v -> {
            String st = item.getUpload_status() == null ? "" : item.getUpload_status().trim().toLowerCase();
            boolean approved = st.equals("approved") || st.equals("1");
            if (approved && item.getPost_id() != null && !item.getPost_id().isEmpty()) {
                Intent intent = new Intent(activity, BookDetailsActivity.class);
                intent.putExtra("BOOK_ID", item.getPost_id());
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, activity.getString(R.string.upload_not_live),
                        Toast.LENGTH_SHORT).show();
            }
        });

        holder.binding.ivUploadCover.post(() ->
                CoverHelper.bind(holder.binding.ivUploadCover,
                        item.getPost_image(),
                        item.getPost_title(),
                        item.getCover_color()));

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

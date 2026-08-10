package com.jntuh.adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.MediaFeedActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowHomeFeedBinding;
import com.jntuh.item.FeedPost;

import java.util.List;

public class HomeFeedAdapter extends RecyclerView.Adapter<HomeFeedAdapter.ViewHolder> {

    private final Activity activity;
    private final List<FeedPost> feedPosts;

    public HomeFeedAdapter(Activity activity, List<FeedPost> feedPosts) {
        this.activity = activity;
        this.feedPosts = feedPosts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowHomeFeedBinding b = RowHomeFeedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FeedPost item = feedPosts.get(position);

        String thumb = item.getThumb_url();
        if (thumb == null || thumb.isEmpty()) thumb = item.getFile_url();
        Glide.with(activity.getApplicationContext())
                .load(thumb)
                .placeholder(R.drawable.placeholder_portable)
                .into(holder.binding.ivHomeFeed);

        // Play badge for video posts.
        boolean isVideo = "reel".equalsIgnoreCase(item.getMedia_type())
                || "video".equalsIgnoreCase(item.getMedia_type());
        holder.binding.ivHomeFeedPlay.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        holder.binding.getRoot().setOnClickListener(v -> {
            // Open the full-screen feed viewer at this post.
            Intent i = new Intent(activity, MediaFeedActivity.class);
            i.putExtra(MediaFeedActivity.EXTRA_POST_ID, item.getPost_id());
            activity.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return feedPosts == null ? 0 : feedPosts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowHomeFeedBinding binding;

        ViewHolder(RowHomeFeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

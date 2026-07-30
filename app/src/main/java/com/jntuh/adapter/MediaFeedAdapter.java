package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowMediaFeedBinding;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Full-bleed feed adapter. Photos render straight into an ImageView; videos use a
 * single shared ExoPlayer that is bound to whichever row is currently centered.
 * We never hold more than one player — {@link #playPosition(int)} moves it, and
 * {@link #releasePlayer()} tears it down when the screen goes away.
 */
public class MediaFeedAdapter extends RecyclerView.Adapter<MediaFeedAdapter.ViewHolder> {

    private static final int TYPE_PHOTO = 0;
    private static final int TYPE_VIDEO = 1;

    private final Activity activity;
    private final List<com.jntuh.item.MediaItem> items;

    private ExoPlayer player;
    private VideoHolder boundVideoHolder;   // the holder the player is currently attached to
    private int boundPosition = -1;
    private RecyclerView recyclerView;

    public MediaFeedAdapter(Activity activity, List<com.jntuh.item.MediaItem> items) {
        this.activity = activity;
        this.items = items;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        this.recyclerView = rv;
    }

    @Override
    public int getItemViewType(int position) {
        String t = items.get(position).getMedia_type();
        return "video".equalsIgnoreCase(t) ? TYPE_VIDEO : TYPE_PHOTO;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        RowMediaFeedBinding binding = RowMediaFeedBinding.inflate(activity.getLayoutInflater(), parent, false);
        // Force each row to fill the parent (one post = one screen).
        binding.getRoot().setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return (viewType == TYPE_VIDEO)
                ? new VideoHolder(binding)
                : new PhotoHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        com.jntuh.item.MediaItem item = items.get(position);

        // Caption + uploader overlay (shared by both types).
        holder.binding.tvCaption.setText(item.getTitle());
        boolean isAdmin = "1".equals(item.getIs_admin());
        holder.binding.tvUploader.setText(
                isAdmin ? activity.getString(R.string.lbl_admin)
                        : "@" + safe(item.getUploaded_by()));

        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---- Playback control, driven by the snap listener in the activity ----

    /**
     * Make {@code position} the active page. If it's a video, attach the shared
     * player to that row and start it; if it's a photo, just release any playing
     * video. Called once per snap. Never holds more than one player.
     */
    @OptIn(markerClass = UnstableApi.class)
    public void playPosition(int position) {
        if (position < 0 || position >= items.size()) return;
        if (position == boundPosition && boundVideoHolder != null) return;

        detachPlayer();
        boundPosition = position;

        if (!isVideo(position) || recyclerView == null) return;

        RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(position);
        if (!(vh instanceof VideoHolder)) return;   // not laid out yet; onViewAttached will retry
        final VideoHolder holder = (VideoHolder) vh;

        if (player == null) {
            player = new ExoPlayer.Builder(activity).build();
        }
        boundVideoHolder = holder;

        holder.binding.progressVideo.setVisibility(View.VISIBLE);
        holder.binding.ivVideoPoster.setVisibility(View.VISIBLE);

        holder.binding.playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(items.get(position).getFile_url()));
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (boundVideoHolder != holder) return;
                if (state == Player.STATE_READY) {
                    holder.binding.progressVideo.setVisibility(View.GONE);
                    holder.binding.ivVideoPoster.setVisibility(View.GONE);
                } else if (state == Player.STATE_BUFFERING) {
                    holder.binding.progressVideo.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /** Detach the player from its current holder (keeps the player instance alive). */
    public void detachPlayer() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.stop();
            player.clearMediaItems();
        }
        if (boundVideoHolder != null) {
            boundVideoHolder.binding.playerView.setPlayer(null);
            boundVideoHolder.binding.ivVideoPoster.setVisibility(View.VISIBLE);
            boundVideoHolder = null;
        }
    }

    /** Fully release the player. Call from the activity's onDestroy / onPause. */
    public void releasePlayer() {
        detachPlayer();
        if (player != null) {
            player.release();
            player = null;
        }
        boundPosition = -1;
    }

    public boolean isVideo(int position) {
        return position >= 0 && position < items.size()
                && "video".equalsIgnoreCase(items.get(position).getMedia_type());
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // If the snap targeted this position before its holder existed, start now.
        if (holder instanceof VideoHolder
                && holder.getBindingAdapterPosition() == boundPosition
                && boundVideoHolder == null) {
            playPosition(boundPosition);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof VideoHolder && holder == boundVideoHolder) {
            detachPlayer();
        }
    }

    // ---- Holders ----

    abstract static class ViewHolder extends RecyclerView.ViewHolder {
        final RowMediaFeedBinding binding;

        ViewHolder(RowMediaFeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        abstract void bind(com.jntuh.item.MediaItem item);
    }

    class PhotoHolder extends ViewHolder {
        PhotoHolder(RowMediaFeedBinding binding) {
            super(binding);
        }

        @Override
        void bind(com.jntuh.item.MediaItem item) {
            binding.playerView.setVisibility(View.GONE);
            binding.ivVideoPoster.setVisibility(View.GONE);
            binding.progressVideo.setVisibility(View.GONE);
            binding.ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(activity.getApplicationContext())
                    .load(item.getFile_url())
                    .into(binding.ivPhoto);
        }
    }

    class VideoHolder extends ViewHolder {
        VideoHolder(RowMediaFeedBinding binding) {
            super(binding);
        }

        @Override
        void bind(com.jntuh.item.MediaItem item) {
            binding.ivPhoto.setVisibility(View.GONE);
            binding.playerView.setVisibility(View.VISIBLE);
            binding.ivVideoPoster.setVisibility(View.VISIBLE);
            // Poster from thumb_url (falls back to nothing if empty).
            String thumb = item.getThumb_url();
            if (thumb != null && !thumb.isEmpty()) {
                Glide.with(activity.getApplicationContext())
                        .load(thumb)
                        .into(binding.ivVideoPoster);
            }
        }
    }
}

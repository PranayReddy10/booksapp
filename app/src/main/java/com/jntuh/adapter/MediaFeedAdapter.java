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
 * Full-screen card feed (one post per screen). A single shared ExoPlayer plays only
 * the centered video and is released on scroll-away. The action bar under each post
 * shows like / comment / share / link / views, each gated on that post's toggle.
 */
public class MediaFeedAdapter extends RecyclerView.Adapter<MediaFeedAdapter.ViewHolder> {

    public interface InteractionListener {
        void onLike(com.jntuh.item.MediaItem item, int position);
        void onComment(com.jntuh.item.MediaItem item, int position);
        void onShare(com.jntuh.item.MediaItem item);
        void onOpenLink(com.jntuh.item.MediaItem item);
        void onOpenBook(com.jntuh.item.MediaItem item);
    }

    private final Activity activity;
    private final List<com.jntuh.item.MediaItem> items;
    private final InteractionListener listener;

    private ExoPlayer player;
    private ViewHolder boundVideoHolder;
    private int boundPosition = -1;
    private RecyclerView recyclerView;

    public MediaFeedAdapter(Activity activity, List<com.jntuh.item.MediaItem> items,
                            InteractionListener listener) {
        this.activity = activity;
        this.items = items;
        this.listener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        this.recyclerView = rv;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        RowMediaFeedBinding binding = RowMediaFeedBinding.inflate(activity.getLayoutInflater(), parent, false);
        binding.getRoot().setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        com.jntuh.item.MediaItem item = items.get(position);
        RowMediaFeedBinding b = holder.binding;

        // Header
        boolean isAdmin = "1".equals(item.getIs_admin());
        b.tvUploader.setText(isAdmin ? activity.getString(R.string.lbl_admin)
                : safe(item.getUploaded_by()));
        b.tvTime.setText(relativeTime(item.getCreated_at()));

        // Caption
        b.tvCaption.setText(item.getTitle());

        // Media
        boolean isVideo = "video".equalsIgnoreCase(item.getMedia_type());

        // Blurred fill behind the media so posters/portrait media don't sit in
        // flat black bars.
        String bgSrc = isVideo ? item.getThumb_url() : item.getFile_url();
        if (bgSrc != null && !bgSrc.isEmpty()) {
            b.ivBlurBg.setVisibility(View.VISIBLE);
            b.vBlurScrim.setVisibility(View.VISIBLE);
            Glide.with(activity.getApplicationContext()).load(bgSrc).into(b.ivBlurBg);
            applyBlur(b.ivBlurBg);
        } else {
            b.ivBlurBg.setVisibility(View.GONE);
            b.vBlurScrim.setVisibility(View.GONE);
        }

        boolean hasMedia = (isVideo ? item.getFile_url() : bgSrc) != null
                && !String.valueOf(isVideo ? item.getFile_url() : bgSrc).trim().isEmpty();

        if (!hasMedia) {
            // Text post: nothing to play or show, so the words fill the screen.
            b.ivPhoto.setVisibility(View.GONE);
            b.playerView.setVisibility(View.GONE);
            b.ivVideoPoster.setVisibility(View.GONE);
            b.progressVideo.setVisibility(View.GONE);
            b.tvCaption.setMaxLines(14);
            b.tvCaption.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22f);
        } else if (isVideo) {
            b.tvCaption.setMaxLines(3);
            b.tvCaption.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f);
            b.ivPhoto.setVisibility(View.GONE);
            b.playerView.setVisibility(View.VISIBLE);
            b.ivVideoPoster.setVisibility(View.VISIBLE);
            String thumb = item.getThumb_url();
            if (thumb != null && !thumb.isEmpty()) {
                Glide.with(activity.getApplicationContext()).load(thumb).into(b.ivVideoPoster);
            }
        } else {
            b.tvCaption.setMaxLines(3);
            b.tvCaption.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f);
            b.playerView.setVisibility(View.GONE);
            b.ivVideoPoster.setVisibility(View.GONE);
            b.progressVideo.setVisibility(View.GONE);
            b.ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(activity.getApplicationContext()).load(item.getFile_url()).into(b.ivPhoto);
        }

        // Views — shown in the right action rail (below comment), when enabled.
        if ("1".equals(item.getShow_views())) {
            b.llViewsRail.setVisibility(View.VISIBLE);
            b.tvViewRail.setText(num(item.getView_count()));
        } else {
            b.llViewsRail.setVisibility(View.GONE);
        }

        // Like — only when allow_likes is on
        if ("1".equals(item.getAllow_likes())) {
            b.llLike.setVisibility(View.VISIBLE);
            bindLikeState(holder, item);
            b.llLike.setOnClickListener(v -> {
                if (listener != null) listener.onLike(item, holder.getBindingAdapterPosition());
            });
        } else {
            b.llLike.setVisibility(View.GONE);
        }

        // Comment — only when allow_comments is on
        if ("1".equals(item.getAllow_comments())) {
            b.llComment.setVisibility(View.VISIBLE);
            b.tvCommentCount.setText(num(item.getComment_count()));
            b.llComment.setOnClickListener(v -> {
                if (listener != null) listener.onComment(item, holder.getBindingAdapterPosition());
            });
        } else {
            b.llComment.setVisibility(View.GONE);
        }

        // Share — always available
        b.ivShare.setOnClickListener(v -> {
            if (listener != null) listener.onShare(item);
        });

        // Link chip — only when link_url present
        String link = item.getLink_url();
        if (link != null && !link.trim().isEmpty()) {
            b.tvLinkChip.setVisibility(View.VISIBLE);
            b.tvLinkChip.setOnClickListener(v -> {
                if (listener != null) listener.onOpenLink(item);
            });
        } else {
            b.tvLinkChip.setVisibility(View.GONE);
        }

        // "View book" bar — only when the post is linked to a book.
        String bookId = item.getBook_id();
        if (bookId != null && !bookId.trim().isEmpty()) {
            String title = item.getBook_title();
            b.tvViewBookLabel.setText(
                    (title != null && !title.trim().isEmpty())
                            ? activity.getString(R.string.lbl_view_book_fmt, title)
                            : activity.getString(R.string.lbl_view_book));
            b.btnViewBook.setVisibility(View.VISIBLE);
            b.btnViewBook.setOnClickListener(v -> {
                if (listener != null) listener.onOpenBook(item);
            });
        } else {
            b.btnViewBook.setVisibility(View.GONE);
        }
    }

    /** Update just the like icon + count from the current item state. */
    public void bindLikeState(@NonNull ViewHolder holder, com.jntuh.item.MediaItem item) {
        boolean liked = "1".equals(item.getIs_liked());
        holder.binding.ivLike.setImageResource(
                liked ? R.drawable.ic_feed_heart : R.drawable.ic_feed_heart_outline);
        // Filled heart is red when liked; outline is white on the media overlay.
        holder.binding.ivLike.setColorFilter(
                androidx.core.content.ContextCompat.getColor(activity,
                        liked ? R.color.status_rejected : R.color.white));
        holder.binding.tvLikeCount.setText(num(item.getLike_count()));
    }

    /** Refresh the like row for a given position without rebinding the whole card. */
    public void refreshLike(int position) {
        if (recyclerView == null) return;
        RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(position);
        if (vh instanceof ViewHolder && position >= 0 && position < items.size()) {
            bindLikeState((ViewHolder) vh, items.get(position));
        }
    }

    // ---- Playback control ----

    @OptIn(markerClass = UnstableApi.class)
    public void playPosition(int position) {
        if (position < 0 || position >= items.size()) return;
        if (position == boundPosition && boundVideoHolder != null) return;

        detachPlayer();
        boundPosition = position;

        if (!isVideo(position) || recyclerView == null) return;

        RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(position);
        if (!(vh instanceof ViewHolder)) return;
        final ViewHolder holder = (ViewHolder) vh;

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

    public void releasePlayer() {
        detachPlayer();
        if (player != null) {
            player.release();
            player = null;
        }
        boundPosition = -1;
    }

    public boolean isVideo(int position) {
        if (position < 0 || position >= items.size()) return false;
        com.jntuh.item.MediaItem it = items.get(position);
        String url = it.getFile_url();
        return "video".equalsIgnoreCase(it.getMedia_type())
                && url != null && !url.trim().isEmpty();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.getBindingAdapterPosition() == boundPosition
                && boundVideoHolder == null && isVideo(boundPosition)) {
            playPosition(boundPosition);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder == boundVideoHolder) {
            detachPlayer();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String num(String s) {
        return (s == null || s.isEmpty()) ? "0" : s;
    }

    // Very small relative-time formatter for "created_at" (yyyy-MM-dd HH:mm:ss, UTC-ish).
    private String relativeTime(String created) {
        if (created == null || created.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat fmt =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            java.util.Date d = fmt.parse(created);
            if (d == null) return created;
            long diff = System.currentTimeMillis() - d.getTime();
            if (diff < 0) diff = 0;
            long min = diff / 60000;
            if (min < 1) return "just now";
            if (min < 60) return min + (min == 1 ? " minute ago" : " minutes ago");
            long hr = min / 60;
            if (hr < 24) return hr + (hr == 1 ? " hour ago" : " hours ago");
            long day = hr / 24;
            if (day < 30) return day + (day == 1 ? " day ago" : " days ago");
            return created.substring(0, 10);
        } catch (Exception e) {
            return created;
        }
    }

    private void applyBlur(android.widget.ImageView view) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                view.setRenderEffect(
                        android.graphics.RenderEffect.createBlurEffect(
                                45f, 45f, android.graphics.Shader.TileMode.CLAMP));
            } catch (Throwable ignored) {
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowMediaFeedBinding binding;

        ViewHolder(RowMediaFeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

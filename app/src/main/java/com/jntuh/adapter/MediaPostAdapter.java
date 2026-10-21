package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowMediaPostBinding;
import com.jntuh.item.MediaItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Campus feed rendered as a timeline: avatar on the left, "Name @handle · 2h" on top,
 * the post's words, optional rounded media, link / book chips, then a spread
 * reply–like–views–share–save row. Videos show a poster with a play badge; tapping the
 * media opens the full-screen player at that post, tapping a text post opens its replies.
 */
public class MediaPostAdapter extends RecyclerView.Adapter<MediaPostAdapter.ViewHolder> {

    public interface Listener {
        void onOpenPost(int position);
        void onLike(MediaItem item, int position);
        void onComment(MediaItem item, int position);
        void onShare(MediaItem item);
        void onOpenLink(MediaItem item);
        void onOpenBook(MediaItem item);
        void onMore(MediaItem item, View anchor);
    }

    private final Activity activity;
    private final List<MediaItem> items;
    private final Listener listener;

    private RecyclerView recyclerView;

    public MediaPostAdapter(Activity activity, List<MediaItem> items, Listener listener) {
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
        return new ViewHolder(RowMediaPostBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        MediaItem item = items.get(position);
        RowMediaPostBinding b = holder.binding;

        // Header: avatar, name, "@handle · time", university underneath.
        boolean isAdmin = "1".equals(item.getIs_admin());
        b.tvUploader.setText(isAdmin ? activity.getString(R.string.lbl_admin) : safe(item.getUploaded_by()));
        String when = relativeTime(item.getCreated_at());
        String handle = isAdmin ? "" : safe(item.getUploader_username()).trim();
        b.tvHandle.setText(handle.isEmpty() ? when : "@" + handle + (when.isEmpty() ? "" : " · " + when));
        b.tvTime.setText(when);

        String avatar = isAdmin ? "" : safe(item.getUploader_image());
        if (isAdmin) {
            b.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        } else if (!avatar.trim().isEmpty()) {
            Glide.with(activity.getApplicationContext())
                    .load(avatar)
                    .placeholder(R.drawable.img_user)
                    .error(R.drawable.img_user)
                    .into(b.ivAvatar);
        } else {
            Glide.with(activity.getApplicationContext()).clear(b.ivAvatar);
            b.ivAvatar.setImageResource(R.drawable.img_user);
        }

        String university = isAdmin ? "" : safe(item.getUploader_university()).trim();
        b.tvUniversity.setText(university);
        b.tvUniversity.setVisibility(university.isEmpty() ? View.GONE : View.VISIBLE);

        b.ivMore.setOnClickListener(v -> {
            if (listener != null) listener.onMore(item, v);
        });

        // Media — videos show their poster with a play badge, photos the file itself.
        // A post with neither is a text post: the words carry the card on their own.
        boolean isVideo = "video".equalsIgnoreCase(item.getMedia_type());
        String thumb = item.getThumb_url();
        String load = (thumb != null && !thumb.isEmpty()) ? thumb : item.getFile_url();
        boolean hasMedia = load != null && !load.trim().isEmpty();

        if (hasMedia) {
            b.cvMedia.setVisibility(View.VISIBLE);
            Glide.with(activity.getApplicationContext())
                    .load(load)
                    .placeholder(R.color.feed_media_bg)
                    .into(b.ivMedia);
            b.ivPlay.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        } else {
            b.cvMedia.setVisibility(View.GONE);
            b.ivPlay.setVisibility(View.GONE);
        }

        View.OnClickListener openPost = v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) listener.onOpenPost(pos);
        };
        b.cvMedia.setOnClickListener(openPost);

        // Tapping the words: media posts open the player, text posts open their replies.
        View.OnClickListener openWords = hasMedia ? openPost : v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null
                    && "1".equals(item.getAllow_comments())) listener.onComment(item, pos);
        };
        b.llPostRow.setOnClickListener(openWords);
        b.tvCaption.setOnClickListener(openWords);
        b.tvPostBody.setOnClickListener(openWords);

        // Like — only when the post allows it.
        if ("1".equals(item.getAllow_likes())) {
            b.llLike.setVisibility(View.VISIBLE);
            bindLikeState(holder, item);
            b.llLike.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) listener.onLike(item, pos);
            });
        } else {
            b.llLike.setVisibility(View.GONE);
        }

        // Comment — only when the post allows it.
        if ("1".equals(item.getAllow_comments())) {
            b.llComment.setVisibility(View.VISIBLE);
            b.tvCommentCount.setText(compact(item.getComment_count()));
            b.llComment.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) listener.onComment(item, pos);
            });
        } else {
            b.llComment.setVisibility(View.GONE);
        }

        b.llShare.setOnClickListener(v -> {
            if (listener != null) listener.onShare(item);
        });

        // Views — count only, when the post exposes it.
        if ("1".equals(item.getShow_views())) {
            b.llViews.setVisibility(View.VISIBLE);
            b.tvViewCount.setText(compact(item.getView_count()));
        } else {
            b.llViews.setVisibility(View.GONE);
        }

        // Words: the title as a bold first line, then the body (text posts keep their
        // words in the description).
        String caption = safe(item.getTitle()).trim();
        b.tvCaption.setText(caption);
        b.tvCaption.setVisibility(caption.isEmpty() ? View.GONE : View.VISIBLE);

        String body = safe(item.getDescription()).trim();
        b.tvPostBody.setText(body);
        b.tvPostBody.setVisibility(body.isEmpty() || body.equals(caption) ? View.GONE : View.VISIBLE);

        // Extras: open link / view book, mirroring the full-screen feed's actions.
        String link = item.getLink_url();
        boolean hasLink = link != null && !link.trim().isEmpty();
        if (hasLink) {
            b.tvLinkChip.setVisibility(View.VISIBLE);
            b.tvLinkChip.setOnClickListener(v -> {
                if (listener != null) listener.onOpenLink(item);
            });
        } else {
            b.tvLinkChip.setVisibility(View.GONE);
        }

        String bookId = item.getBook_id();
        boolean hasBook = bookId != null && !bookId.trim().isEmpty();
        if (hasBook) {
            String title = item.getBook_title();
            b.tvBookChip.setText((title != null && !title.trim().isEmpty())
                    ? activity.getString(R.string.lbl_view_book_fmt, title)
                    : activity.getString(R.string.lbl_view_book));
            b.tvBookChip.setVisibility(View.VISIBLE);
            b.tvBookChip.setOnClickListener(v -> {
                if (listener != null) listener.onOpenBook(item);
            });
        } else {
            b.tvBookChip.setVisibility(View.GONE);
        }
        b.llPostExtras.setVisibility((hasLink || hasBook) ? View.VISIBLE : View.GONE);
    }

    /** Paint the heart + count from the item's current like state. */
    public void bindLikeState(@NonNull ViewHolder holder, MediaItem item) {
        boolean liked = "1".equals(item.getIs_liked());
        holder.binding.ivLike.setImageResource(
                liked ? R.drawable.ic_feed_heart : R.drawable.ic_feed_heart_outline);
        holder.binding.ivLike.setColorFilter(
                androidx.core.content.ContextCompat.getColor(activity,
                        liked ? R.color.status_rejected : R.color.authx_text_muted));
        holder.binding.tvLikeCount.setText(compact(item.getLike_count()));
        holder.binding.tvLikeCount.setTextColor(
                androidx.core.content.ContextCompat.getColor(activity,
                        liked ? R.color.status_rejected : R.color.authx_text_muted));
    }

    /** Refresh just the like row for a position, without rebinding the whole card. */
    public void refreshLike(int position) {
        if (recyclerView == null || position < 0 || position >= items.size()) return;
        RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(position);
        if (vh instanceof ViewHolder) {
            bindLikeState((ViewHolder) vh, items.get(position));
        } else {
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    /** 18600 -> "18.6k", so long counts don't push the action row around. */
    private String compact(String s) {
        if (s == null || s.isEmpty()) return "0";
        long n;
        try {
            n = Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return s;
        }
        if (n < 1000) return String.valueOf(n);
        if (n < 1000000) return trimZero(n / 1000d) + "k";
        return trimZero(n / 1000000d) + "M";
    }

    private String trimZero(double v) {
        String out = String.format(Locale.US, "%.1f", v);
        return out.endsWith(".0") ? out.substring(0, out.length() - 2) : out;
    }

    // Small relative-time formatter for "created_at" (yyyy-MM-dd HH:mm:ss).
    private String relativeTime(String created) {
        if (created == null || created.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat fmt =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            java.util.Date d = fmt.parse(created);
            if (d == null) return created;
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
            return created;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowMediaPostBinding binding;

        ViewHolder(RowMediaPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

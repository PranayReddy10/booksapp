package com.jntuh.adapter;

import android.app.Activity;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.books.R;
import com.jntuh.books.databinding.RowContinueBinding;
import com.jntuh.item.SubCatListBook;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.CoverHelper;
import com.jntuh.util.OnClick;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Continue Reading list: one card per book in progress, with its cover, title,
 * the page the reader stopped at, and a Continue pill. The whole card opens it.
 */
public class ContinueAdapter extends RecyclerView.Adapter<ContinueAdapter.ViewHolder> {

    private final Activity activity;
    private final List<SubCatListBook> books;
    private OnClick onClick;

    public ContinueAdapter(Activity activity, List<SubCatListBook> books) {
        this.activity = activity;
        this.books = books;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowContinueBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        SubCatListBook book = books.get(position);
        RowContinueBinding b = holder.binding;

        b.tvContTitle.setText(book.getPost_title());

        String page = book.getPage_num() == null ? "" : book.getPage_num().trim();
        boolean started = !page.isEmpty() && !"0".equals(page);
        b.tvContPage.setText(started
                ? activity.getString(R.string.lbl_cont_page, page)
                : activity.getString(R.string.lbl_cont_not_started));

        // Generated covers need the view's real size, so bind after layout.
        b.ivContCover.post(() -> CoverHelper.bind(b.ivContCover,
                book.getPost_image(), book.getPost_title(), book.getCover_color()));

        b.llContRow.setOnClickListener(v -> open(holder));
        b.tvContResume.setOnClickListener(v -> open(holder));
    }

    private void open(ViewHolder holder) {
        int p = holder.getBindingAdapterPosition();
        if (p == RecyclerView.NO_POSITION) return;
        AdInterstitialAds.ShowInterstitialAds(activity, p, onClick);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final RowContinueBinding binding;

        ViewHolder(RowContinueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

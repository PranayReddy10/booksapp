package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.books.R;
import com.jntuh.books.databinding.RowResultSemesterBinding;
import com.jntuh.item.SemesterItem;
import com.jntuh.item.SubjectItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Read-only semester list on the My Results tab. Each card shows the semester's
 * SGPA and an expandable subject table (Credits / Grade / Points), with a red
 * Backlog tag and optional internal/external/total line per subject.
 */
public class SemesterAdapter extends RecyclerView.Adapter<SemesterAdapter.ViewHolder> {

    private final Activity activity;
    private final List<SemesterItem> semesters;
    // Track which positions are expanded (first one open by default).
    private final boolean[] expanded;

    public SemesterAdapter(Activity activity, List<SemesterItem> semesters) {
        this.activity = activity;
        this.semesters = semesters;
        this.expanded = new boolean[semesters.size()];
        if (expanded.length > 0) expanded[0] = true;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowResultSemesterBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        SemesterItem sem = semesters.get(position);
        RowResultSemesterBinding b = holder.binding;

        String code = sem.getSem_code() == null ? "" : sem.getSem_code().trim();
        b.tvSemTitle.setText(activity.getString(R.string.result_semesters).equals("Semesters")
                ? "Semester " + code : code);

        StringBuilder meta = new StringBuilder();
        if (sem.getExam_month_year() != null && !sem.getExam_month_year().trim().isEmpty()) {
            meta.append(sem.getExam_month_year().trim());
        }
        if (sem.getCredits_earned() != null) {
            if (meta.length() > 0) meta.append("  ·  ");
            meta.append(fmt(sem.getCredits_earned())).append(" credits");
        }
        if (meta.length() == 0) {
            b.tvSemMeta.setVisibility(View.GONE);
        } else {
            b.tvSemMeta.setVisibility(View.VISIBLE);
            b.tvSemMeta.setText(meta.toString());
        }

        if (sem.getSgpa() != null) {
            b.llSgpaChip.setVisibility(View.VISIBLE);
            b.tvSemSgpa.setText(fmt(sem.getSgpa()));
        } else {
            b.llSgpaChip.setVisibility(View.GONE);
        }

        // Per-semester verified/locked badge.
        b.pillSemLocked.setVisibility(sem.getLocked() == 1 ? View.VISIBLE : View.GONE);

        // Build the subject rows once per bind.
        b.llSubjectRows.removeAllViews();
        List<SubjectItem> subjects = sem.getSubjects();
        LayoutInflater inflater = activity.getLayoutInflater();
        for (SubjectItem s : subjects) {
            View row = inflater.inflate(R.layout.row_result_subject, b.llSubjectRows, false);

            TextView tvName = row.findViewById(R.id.tvSubName);
            TextView tvCode = row.findViewById(R.id.tvSubCode);
            TextView tvCredits = row.findViewById(R.id.tvSubCredits);
            TextView tvGrade = row.findViewById(R.id.tvSubGrade);
            TextView tvPoints = row.findViewById(R.id.tvSubPoints);
            TextView tagBacklog = row.findViewById(R.id.tagBacklog);
            TextView tvMarks = row.findViewById(R.id.tvSubMarks);

            tvName.setText(nn(s.getSubject_name(), "—"));
            tvCode.setText(nn(s.getSubject_code(), ""));
            tvCredits.setText(s.getCredits() == null ? "—" : fmt(s.getCredits()));
            tvGrade.setText(nn(s.getGrade(), "—"));
            tvPoints.setText(s.getGrade_points() == null ? "—" : fmt(s.getGrade_points()));

            tagBacklog.setVisibility(s.getIs_backlog() == 1 ? View.VISIBLE : View.GONE);

            if (s.getInternal() != null || s.getExternal() != null || s.getTotal() != null) {
                StringBuilder m = new StringBuilder();
                if (s.getInternal() != null) m.append("Int ").append(s.getInternal());
                if (s.getExternal() != null) {
                    if (m.length() > 0) m.append("  ");
                    m.append("Ext ").append(s.getExternal());
                }
                if (s.getTotal() != null) {
                    if (m.length() > 0) m.append("  ");
                    m.append("Total ").append(s.getTotal());
                }
                tvMarks.setVisibility(View.VISIBLE);
                tvMarks.setText(m.toString());
            } else {
                tvMarks.setVisibility(View.GONE);
            }

            b.llSubjectRows.addView(row);
        }

        applyExpanded(holder, expanded[position], false);

        b.llSemHeader.setOnClickListener(v -> {
            int p = holder.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) return;
            expanded[p] = !expanded[p];
            applyExpanded(holder, expanded[p], true);
        });
    }

    private void applyExpanded(ViewHolder holder, boolean open, boolean animate) {
        holder.binding.llSubjectContainer.setVisibility(open ? View.VISIBLE : View.GONE);
        float target = open ? 180f : 0f;
        if (animate) {
            holder.binding.ivSemChevron.animate().rotation(target).setDuration(220).start();
        } else {
            holder.binding.ivSemChevron.setRotation(target);
        }
    }

    @Override
    public int getItemCount() {
        return semesters.size();
    }

    private static String nn(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s.trim();
    }

    /** Trim trailing .0 for whole numbers, else 2 decimals. */
    private static String fmt(Double d) {
        if (d == null) return "—";
        if (d == Math.rint(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) (double) d);
        }
        return String.format(java.util.Locale.US, "%.2f", d);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RowResultSemesterBinding binding;

        ViewHolder(RowResultSemesterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

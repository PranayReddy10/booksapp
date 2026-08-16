package com.jntuh.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.books.R;
import com.jntuh.books.databinding.RowTaskDateBinding;
import com.jntuh.util.TaskDates;

import java.util.Date;
import java.util.List;
import java.util.Set;

/** The horizontal day strip above the Today list. */
public class TaskDateAdapter extends RecyclerView.Adapter<TaskDateAdapter.Holder> {

    public interface Listener {
        void onDateSelected(long dayStart);
    }

    private final Activity activity;
    private final List<Long> days;
    private final Listener listener;
    /** Day-starts that have at least one task, so the strip can show a dot. */
    private Set<Long> daysWithTasks;
    private long selectedDay;

    public TaskDateAdapter(Activity activity, List<Long> days, long selectedDay, Listener listener) {
        this.activity = activity;
        this.days = days;
        this.selectedDay = selectedDay;
        this.listener = listener;
    }

    public void setSelectedDay(long dayStart) {
        long previous = selectedDay;
        selectedDay = dayStart;
        // indexOf returns -1 for a day outside the strip's range; repainting only the two
        // affected chips keeps the strip from flickering on every tap.
        notifyIfPresent(days.indexOf(previous));
        notifyIfPresent(days.indexOf(dayStart));
    }

    private void notifyIfPresent(int position) {
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    public void setDaysWithTasks(Set<Long> daysWithTasks) {
        this.daysWithTasks = daysWithTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(RowTaskDateBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        long day = days.get(position);
        RowTaskDateBinding v = holder.v;
        boolean selected = day == selectedDay;

        v.tvDateDay.setText(TaskDates.dayFormat().format(new Date(day)));
        v.tvDateWeekday.setText(TaskDates.weekdayFormat().format(new Date(day)));

        v.llDateRoot.setBackgroundResource(selected
                ? R.drawable.bg_task_date_selected : R.drawable.bg_task_date_normal);
        v.tvDateDay.setTextColor(ContextCompat.getColor(activity,
                selected ? R.color.white : R.color.task_title));
        v.tvDateWeekday.setTextColor(ContextCompat.getColor(activity,
                selected ? R.color.white : R.color.task_muted));

        boolean hasTasks = daysWithTasks != null && daysWithTasks.contains(day);
        v.viewDateDot.setVisibility(hasTasks ? View.VISIBLE : View.INVISIBLE);
        if (hasTasks) {
            v.viewDateDot.getBackground().mutate().setTint(ContextCompat.getColor(activity,
                    selected ? R.color.white : R.color.app_bg_orange));
        }

        v.llDateRoot.setOnClickListener(view -> listener.onDateSelected(day));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final RowTaskDateBinding v;

        Holder(RowTaskDateBinding binding) {
            super(binding.getRoot());
            this.v = binding;
        }
    }
}

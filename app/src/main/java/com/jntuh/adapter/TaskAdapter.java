package com.jntuh.adapter;

import android.app.Activity;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.books.R;
import com.jntuh.books.databinding.RowTaskBinding;
import com.jntuh.item.TaskCategory;
import com.jntuh.item.TaskItem;
import com.jntuh.util.TaskDates;

import java.util.List;

/** Renders the task cards. Tapping a card edits it; tapping the tick completes it. */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.Holder> {

    public interface Listener {
        void onTaskClick(TaskItem task);

        void onTaskToggle(TaskItem task);
    }

    private final Activity activity;
    private final List<TaskItem> tasks;
    private final Listener listener;

    public TaskAdapter(Activity activity, List<TaskItem> tasks, Listener listener) {
        this.activity = activity;
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(RowTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TaskItem task = tasks.get(position);
        RowTaskBinding v = holder.v;

        int accent = ContextCompat.getColor(activity, TaskCategory.colorOf(task.getCategory()));

        v.tvTaskTitle.setText(task.getTitle());
        v.ivTaskIcon.setImageResource(TaskCategory.iconOf(task.getCategory()));
        v.ivTaskIcon.setColorFilter(accent);
        v.viewTaskRail.getBackground().mutate().setTint(accent);

        v.tvTaskCategory.setText(activity.getString(TaskCategory.labelOf(task.getCategory())).toUpperCase());
        v.tvTaskCategory.setTextColor(accent);

        v.tvTaskTime.setText(TaskDates.relativeLabel(activity, task.getDueAt()));

        // Overdue reads red; everything else stays muted.
        if (task.isOverdue()) {
            v.tvTaskTime.setTextColor(ContextCompat.getColor(activity, R.color.task_overdue));
            v.ivTaskClock.setColorFilter(ContextCompat.getColor(activity, R.color.task_overdue));
        } else {
            v.tvTaskTime.setTextColor(ContextCompat.getColor(activity, R.color.task_muted));
            v.ivTaskClock.setColorFilter(ContextCompat.getColor(activity, R.color.task_muted));
        }

        v.ivTaskAlarm.setVisibility(task.isAlarm() ? View.VISIBLE : View.GONE);
        v.ivTaskRepeat.setVisibility(task.isRepeating() ? View.VISIBLE : View.GONE);

        // Completed tasks are struck through and dimmed rather than hidden.
        if (task.isDone()) {
            v.tvTaskTitle.setPaintFlags(v.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            v.ivTaskDone.setColorFilter(ContextCompat.getColor(activity, R.color.task_done));
            v.llTaskRoot.setAlpha(0.6f);
        } else {
            v.tvTaskTitle.setPaintFlags(v.tvTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            v.ivTaskDone.setColorFilter(ContextCompat.getColor(activity, R.color.task_hairline));
            v.llTaskRoot.setAlpha(1f);
        }

        v.llTaskRoot.setOnClickListener(view -> listener.onTaskClick(task));
        v.flTaskDone.setOnClickListener(view -> listener.onTaskToggle(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final RowTaskBinding v;

        Holder(RowTaskBinding binding) {
            super(binding.getRoot());
            this.v = binding;
        }
    }
}

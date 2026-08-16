package com.jntuh.books;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.jntuh.adapter.TaskAdapter;
import com.jntuh.adapter.TaskDateAdapter;
import com.jntuh.books.databinding.ActivityTasksBinding;
import com.jntuh.item.TaskItem;
import com.jntuh.service.TaskActionReceiver;
import com.jntuh.util.AlarmPermissions;
import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.Method;
import com.jntuh.util.NotificationTiramisu;
import com.jntuh.util.ReminderScheduler;
import com.jntuh.util.TaskDates;
import com.jntuh.util.TaskNotifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The task hub: a daily-goal card, a day strip, and Today / Upcoming / Completed lists.
 * All data is local (see {@link DatabaseHandler}), so this screen works offline.
 */
public class TasksActivity extends AppCompatActivity implements TaskAdapter.Listener {

    public static final String EXTRA_FOCUS_TASK_ID = "focus_task_id";

    private static final int TAB_TODAY = 0;
    private static final int TAB_UPCOMING = 1;
    private static final int TAB_DONE = 2;

    /** How far the day strip runs: a week back for overdue work, a month ahead. */
    private static final int DAYS_BEFORE = 7;
    private static final int DAYS_AFTER = 30;

    private ActivityTasksBinding v;
    private DatabaseHandler db;
    private Method method;

    private final List<TaskItem> shown = new ArrayList<>();
    private TaskAdapter taskAdapter;
    private TaskDateAdapter dateAdapter;

    private long selectedDay;
    private int currentTab = TAB_TODAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        v = ActivityTasksBinding.inflate(getLayoutInflater());
        setContentView(v.getRoot());

        db = new DatabaseHandler(this);
        method = new Method(this);
        method.forceRTLIfSupported();

        selectedDay = TaskDates.startOfDay(System.currentTimeMillis());

        TaskNotifier.ensureChannels(this);
        NotificationTiramisu.takePermission(this);

        setUpTabs();
        setUpLists();

        v.ivBack.setOnClickListener(view -> finish());
        v.fabAddTask.setOnClickListener(view -> openEditor(null));
        v.ivTaskSettings.setOnClickListener(view -> showPermissionSheet());
        v.llPermBanner.setOnClickListener(view -> showPermissionSheet());

        // Opened from a notification: jump straight into that task.
        long focusId = getIntent() == null ? -1 : getIntent().getLongExtra(EXTRA_FOCUS_TASK_ID, -1);
        if (focusId >= 0) {
            TaskItem task = db.getTask(focusId);
            if (task != null) {
                openEditor(task);
            }
        }
    }

    private void setUpTabs() {
        v.tabTasks.addTab(v.tabTasks.newTab().setText(getString(R.string.task_tab_today)));
        v.tabTasks.addTab(v.tabTasks.newTab().setText(getString(R.string.task_tab_upcoming)));
        v.tabTasks.addTab(v.tabTasks.newTab().setText(getString(R.string.task_tab_done)));

        v.tabTasks.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                // The day strip only makes sense when looking at a single day.
                v.rvDates.setVisibility(currentTab == TAB_TODAY ? View.VISIBLE : View.GONE);
                refresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setUpLists() {
        v.rvTasks.setLayoutManager(new LinearLayoutManager(this));
        v.rvTasks.setNestedScrollingEnabled(false);
        taskAdapter = new TaskAdapter(this, shown, this);
        v.rvTasks.setAdapter(taskAdapter);

        List<Long> days = new ArrayList<>();
        long today = TaskDates.startOfDay(System.currentTimeMillis());
        for (int i = -DAYS_BEFORE; i <= DAYS_AFTER; i++) {
            days.add(TaskDates.startOfDay(TaskDates.plusDays(today, i)));
        }

        v.rvDates.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dateAdapter = new TaskDateAdapter(this, days, selectedDay, dayStart -> {
            selectedDay = dayStart;
            dateAdapter.setSelectedDay(dayStart);
            refresh();
        });
        v.rvDates.setAdapter(dateAdapter);
        // Open on today, with a couple of past days visible for context.
        v.rvDates.scrollToPosition(Math.max(0, DAYS_BEFORE - 2));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        shown.clear();

        switch (currentTab) {
            case TAB_UPCOMING:
                // Everything still open from tomorrow onwards.
                for (TaskItem task : db.getTasks()) {
                    if (!task.isDone() && task.getDueAt() >= TaskDates.endOfDay(System.currentTimeMillis())) {
                        shown.add(task);
                    }
                }
                v.tvEmptyTitle.setText(R.string.task_empty_upcoming);
                break;

            case TAB_DONE:
                for (TaskItem task : db.getTasks()) {
                    if (task.isDone()) {
                        shown.add(task);
                    }
                }
                v.tvEmptyTitle.setText(R.string.task_empty_done);
                break;

            case TAB_TODAY:
            default:
                shown.addAll(db.getTasksBetween(selectedDay, TaskDates.endOfDay(selectedDay)));
                v.tvEmptyTitle.setText(R.string.task_empty_today);
                break;
        }

        taskAdapter.notifyDataSetChanged();
        v.llEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        v.rvTasks.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);

        updateGoalCard();
        updateDateDots();
        updatePermissionBanner();
    }

    /** Progress card counts the selected day when on Today, otherwise actual today. */
    private void updateGoalCard() {
        long day = currentTab == TAB_TODAY ? selectedDay : TaskDates.startOfDay(System.currentTimeMillis());
        List<TaskItem> ofDay = db.getTasksBetween(day, TaskDates.endOfDay(day));

        int total = ofDay.size();
        int done = 0;
        for (TaskItem task : ofDay) {
            if (task.isDone()) done++;
        }

        v.tvGoalCount.setText(getString(R.string.task_goal_progress, done, total));
        v.pbGoal.setProgress(total == 0 ? 0 : (int) ((done * 100f) / total));

        if (total == 0) {
            v.tvGoalMsg.setText(R.string.task_goal_none);
        } else if (done >= total) {
            v.tvGoalMsg.setText(R.string.task_goal_all_done);
        } else {
            v.tvGoalMsg.setText(getString(R.string.task_goal_remaining, total - done));
        }

        int streak = calculateStreak();
        v.tvStreak.setVisibility(streak >= 2 ? View.VISIBLE : View.GONE);
        v.tvStreak.setText(getString(R.string.task_streak, streak));
    }

    /**
     * Consecutive days, counting back from today, on which at least one task was
     * completed. Today not yet having a completion does not break the streak — that only
     * happens once the day is over.
     */
    private int calculateStreak() {
        List<TaskItem> all = db.getTasks();
        Set<Long> completedDays = new HashSet<>();
        for (TaskItem task : all) {
            if (task.getCompletedAt() > 0) {
                completedDays.add(TaskDates.startOfDay(task.getCompletedAt()));
            }
        }
        if (completedDays.isEmpty()) {
            return 0;
        }

        long today = TaskDates.startOfDay(System.currentTimeMillis());
        int streak = 0;
        long cursor = completedDays.contains(today) ? today : TaskDates.startOfDay(TaskDates.plusDays(today, -1));
        while (completedDays.contains(cursor)) {
            streak++;
            cursor = TaskDates.startOfDay(TaskDates.plusDays(cursor, -1));
        }
        return streak;
    }

    private void updateDateDots() {
        Set<Long> days = new HashSet<>();
        for (TaskItem task : db.getTasks()) {
            if (!task.isDone()) {
                days.add(TaskDates.startOfDay(task.getDueAt()));
            }
        }
        dateAdapter.setDaysWithTasks(days);
    }

    /** Only nags when there is actually an alarm-type task that the OS could swallow. */
    private void updatePermissionBanner() {
        boolean hasAlarmTask = false;
        for (TaskItem task : db.getTasksToSchedule()) {
            if (task.isAlarm()) {
                hasAlarmTask = true;
                break;
            }
        }
        boolean blocked = hasAlarmTask && !AlarmPermissions.isFullyReady(this);
        v.llPermBanner.setVisibility(blocked ? View.VISIBLE : View.GONE);
    }

    private void openEditor(TaskItem task) {
        Intent intent = new Intent(this, AddEditTaskActivity.class);
        if (task != null) {
            intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.getId());
        }
        startActivity(intent);
    }

    @Override
    public void onTaskClick(TaskItem task) {
        openEditor(task);
    }

    @Override
    public void onTaskToggle(TaskItem task) {
        if (task.isDone()) {
            // Re-open it and put the reminder back if the time is still ahead.
            task.setDone(false);
            task.setCompletedAt(0);
            db.updateTask(task);
            ReminderScheduler.schedule(this, task);
        } else {
            TaskActionReceiver.markDone(this, db, task);
            TaskNotifier.cancel(this, task.getId());
            Toast.makeText(this, R.string.task_marked_done, Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    /**
     * One dialog for all three OS gates. Each row is skipped when already granted, so a
     * fully-permitted user sees nothing to do.
     */
    private void showPermissionSheet() {
        List<String> labels = new ArrayList<>();
        final List<Intent> intents = new ArrayList<>();

        if (!AlarmPermissions.canScheduleExact(this)) {
            labels.add(getString(R.string.task_perm_exact) + "\n" + getString(R.string.task_perm_exact_desc));
            intents.add(AlarmPermissions.exactAlarmSettingsIntent(this));
        }
        if (!AlarmPermissions.canUseFullScreen(this)) {
            labels.add(getString(R.string.task_perm_fullscreen) + "\n" + getString(R.string.task_perm_fullscreen_desc));
            intents.add(AlarmPermissions.fullScreenSettingsIntent(this));
        }
        labels.add(getString(R.string.task_perm_battery) + "\n" + getString(R.string.task_perm_battery_desc));
        intents.add(AlarmPermissions.batterySettingsIntent());

        new AlertDialog.Builder(this)
                .setTitle(R.string.task_perm_title)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    Intent intent = intents.get(which);
                    if (intent == null) return;
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        // Some OEM builds ship without these settings screens.
                        Toast.makeText(this, R.string.task_perm_battery_desc, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.task_perm_later, null)
                .show();
    }
}

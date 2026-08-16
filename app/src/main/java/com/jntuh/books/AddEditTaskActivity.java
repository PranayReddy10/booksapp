package com.jntuh.books;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.jntuh.books.databinding.ActivityAddEditTaskBinding;
import com.jntuh.item.TaskCategory;
import com.jntuh.item.TaskItem;
import com.jntuh.util.AlarmPermissions;
import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.MyApplication;
import com.jntuh.util.ReminderScheduler;
import com.jntuh.util.TaskDates;

import java.util.Calendar;

/** Create or edit a single task, then hand it to {@link ReminderScheduler}. */
public class AddEditTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    /** Spinner positions map to these minute values. */
    private static final int[] LEAD_MINUTES = {0, 5, 15, 30, 60, 1440};
    private static final int[] SNOOZE_MINUTES = {5, 10, 15, 30};

    private static final int[] REPEAT_MODES = {
            TaskItem.REPEAT_ONCE, TaskItem.REPEAT_DAILY, TaskItem.REPEAT_WEEKDAYS,
            TaskItem.REPEAT_WEEKLY, TaskItem.REPEAT_MONTHLY
    };

    private ActivityAddEditTaskBinding v;
    private DatabaseHandler db;

    private TaskItem task;
    private boolean isEdit;
    private final Calendar due = Calendar.getInstance();

    private ActivityResultLauncher<Intent> tonePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        v = ActivityAddEditTaskBinding.inflate(getLayoutInflater());
        setContentView(v.getRoot());

        db = new DatabaseHandler(this);
        registerTonePicker();

        long id = getIntent() == null ? -1 : getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        if (id >= 0) {
            task = db.getTask(id);
        }
        isEdit = task != null;
        if (!isEdit) {
            task = new TaskItem();
            // Default to the next round half-hour, which is nearly always what is wanted.
            due.setTimeInMillis(System.currentTimeMillis());
            due.add(Calendar.MINUTE, 30);
            due.set(Calendar.MINUTE, due.get(Calendar.MINUTE) < 30 ? 0 : 30);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
        } else {
            due.setTimeInMillis(task.getDueAt());
        }

        v.tvFormTitle.setText(isEdit ? R.string.task_edit : R.string.task_new);
        v.ivDelete.setVisibility(isEdit ? View.VISIBLE : View.GONE);

        buildCategoryChips();
        buildRepeatChips();
        buildSpinners();
        bindTask();

        v.ivBack.setOnClickListener(view -> finish());
        v.llPickDate.setOnClickListener(view -> pickDate());
        v.llPickTime.setOnClickListener(view -> pickTime());
        v.llPickTone.setOnClickListener(view -> pickTone());
        v.btnSave.setOnClickListener(view -> save());
        v.ivDelete.setOnClickListener(view -> confirmDelete());

        v.rgRemind.setOnCheckedChangeListener((group, checkedId) -> onReminderTypeChanged(true));
    }

    private void registerTonePicker() {
        tonePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getData() == null) return;
            Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            MyApplication.getInstance().saveAlarmToneUri(uri == null ? "" : uri.toString());
            showToneName();
        });
    }

    /** Chips are built in code so the category list stays driven by {@link TaskCategory}. */
    private Chip makeChip(CharSequence label, Object tag) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setTag(tag);
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.task_chip_bg_state));
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.task_chip_text_state));
        return chip;
    }

    private void buildCategoryChips() {
        for (String category : TaskCategory.ALL) {
            v.chipCategory.addView(makeChip(getString(TaskCategory.labelOf(category)), category));
        }
    }

    private void buildRepeatChips() {
        int[] labels = {R.string.task_repeat_once, R.string.task_repeat_daily,
                R.string.task_repeat_weekdays, R.string.task_repeat_weekly,
                R.string.task_repeat_monthly};
        for (int i = 0; i < REPEAT_MODES.length; i++) {
            v.chipRepeat.addView(makeChip(getString(labels[i]), REPEAT_MODES[i]));
        }
    }

    private void buildSpinners() {
        String[] leadLabels = {
                getString(R.string.task_lead_none), getString(R.string.task_lead_5),
                getString(R.string.task_lead_15), getString(R.string.task_lead_30),
                getString(R.string.task_lead_60), getString(R.string.task_lead_1440)};
        v.spLead.setAdapter(spinnerAdapter(leadLabels));

        String[] snoozeLabels = {
                getString(R.string.task_snooze_5), getString(R.string.task_snooze_10),
                getString(R.string.task_snooze_15), getString(R.string.task_snooze_30)};
        v.spSnooze.setAdapter(spinnerAdapter(snoozeLabels));
    }

    /**
     * simple_spinner_item for the closed state and the dropdown variant for the popup —
     * using the dropdown layout for both leaves a stray radio mark on the collapsed row.
     */
    private ArrayAdapter<String> spinnerAdapter(String[] labels) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void bindTask() {
        v.edtTitle.setText(task.getTitle());
        v.edtNotes.setText(task.getNotes());

        selectCategoryChip(task.getCategory());
        selectRepeatChip(task.getRepeatMode());

        switch (task.getReminderType()) {
            case TaskItem.REMIND_ALARM:
                v.rbRemindAlarm.setChecked(true);
                break;
            case TaskItem.REMIND_NONE:
                v.rbRemindNone.setChecked(true);
                break;
            default:
                v.rbRemindNotification.setChecked(true);
                break;
        }

        v.spLead.setSelection(indexOf(LEAD_MINUTES, task.getLeadMinutes()));
        v.spSnooze.setSelection(indexOf(SNOOZE_MINUTES, task.getSnoozeMinutes()));

        showDateTime();
        showToneName();
        onReminderTypeChanged(false);
    }

    private void selectCategoryChip(String category) {
        if (v.chipCategory.getChildCount() == 0) return;
        for (int i = 0; i < v.chipCategory.getChildCount(); i++) {
            Chip chip = (Chip) v.chipCategory.getChildAt(i);
            if (chip.getTag().equals(category)) {
                chip.setChecked(true);
                return;
            }
        }
        ((Chip) v.chipCategory.getChildAt(0)).setChecked(true);
    }

    private void selectRepeatChip(int mode) {
        for (int i = 0; i < v.chipRepeat.getChildCount(); i++) {
            Chip chip = (Chip) v.chipRepeat.getChildAt(i);
            if (((Integer) chip.getTag()) == mode) {
                chip.setChecked(true);
                return;
            }
        }
    }

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) return i;
        }
        return 0;
    }

    private void showDateTime() {
        v.tvPickDate.setText(TaskDates.dateFormat().format(due.getTime()));
        v.tvPickTime.setText(TaskDates.timeFormat().format(due.getTime()));
    }

    private void showToneName() {
        String saved = MyApplication.getInstance().getAlarmToneUri();
        if (TextUtils.isEmpty(saved)) {
            v.tvToneName.setText(R.string.task_tone_default);
            return;
        }
        try {
            String title = RingtoneManager.getRingtone(this, Uri.parse(saved)).getTitle(this);
            v.tvToneName.setText(TextUtils.isEmpty(title) ? getString(R.string.task_tone_default) : title);
        } catch (Exception e) {
            v.tvToneName.setText(R.string.task_tone_default);
        }
    }

    /**
     * Lead time and snooze are meaningless without a reminder; the tone is alarm-only.
     *
     * @param userInitiated true when the user just tapped a radio button. Opening an
     *                      existing alarm task must not re-nag about permissions every
     *                      single time, so the prompt is limited to a real choice.
     */
    private void onReminderTypeChanged(boolean userInitiated) {
        boolean wantsReminder = !v.rbRemindNone.isChecked();
        boolean isAlarm = v.rbRemindAlarm.isChecked();

        v.llReminderOptions.setVisibility(wantsReminder ? View.VISIBLE : View.GONE);
        v.llTone.setVisibility(isAlarm ? View.VISIBLE : View.GONE);

        // Warn as soon as alarm is picked, rather than letting it fail silently later.
        if (userInitiated && isAlarm && !AlarmPermissions.isFullyReady(this)) {
            promptForAlarmPermissions();
        }
    }

    private void promptForAlarmPermissions() {
        final Intent intent = !AlarmPermissions.canScheduleExact(this)
                ? AlarmPermissions.exactAlarmSettingsIntent(this)
                : AlarmPermissions.fullScreenSettingsIntent(this);
        if (intent == null) return;

        boolean exactMissing = !AlarmPermissions.canScheduleExact(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.task_perm_title)
                .setMessage(exactMissing
                        ? R.string.task_perm_exact_desc
                        : R.string.task_perm_fullscreen_desc)
                .setPositiveButton(R.string.task_perm_open, (dialog, which) -> {
                    try {
                        startActivity(intent);
                    } catch (Exception ignored) {
                        // Settings screen missing on this build; the alarm still fires,
                        // just without the exactness or lockscreen takeover.
                    }
                })
                .setNegativeButton(R.string.task_perm_later, null)
                .show();
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            due.set(Calendar.YEAR, year);
            due.set(Calendar.MONTH, month);
            due.set(Calendar.DAY_OF_MONTH, day);
            showDateTime();
        }, due.get(Calendar.YEAR), due.get(Calendar.MONTH), due.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            due.set(Calendar.HOUR_OF_DAY, hour);
            due.set(Calendar.MINUTE, minute);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            showDateTime();
        }, due.get(Calendar.HOUR_OF_DAY), due.get(Calendar.MINUTE), false).show();
    }

    private void pickTone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.task_label_tone));

        String saved = MyApplication.getInstance().getAlarmToneUri();
        if (!TextUtils.isEmpty(saved)) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(saved));
        }
        try {
            tonePicker.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.task_tone_default, Toast.LENGTH_SHORT).show();
        }
    }

    private String selectedCategory() {
        for (int i = 0; i < v.chipCategory.getChildCount(); i++) {
            Chip chip = (Chip) v.chipCategory.getChildAt(i);
            if (chip.isChecked()) {
                return (String) chip.getTag();
            }
        }
        return TaskCategory.OTHER;
    }

    private int selectedRepeat() {
        for (int i = 0; i < v.chipRepeat.getChildCount(); i++) {
            Chip chip = (Chip) v.chipRepeat.getChildAt(i);
            if (chip.isChecked()) {
                return (Integer) chip.getTag();
            }
        }
        return TaskItem.REPEAT_ONCE;
    }

    private int selectedReminderType() {
        if (v.rbRemindAlarm.isChecked()) return TaskItem.REMIND_ALARM;
        if (v.rbRemindNone.isChecked()) return TaskItem.REMIND_NONE;
        return TaskItem.REMIND_NOTIFICATION;
    }

    private void save() {
        String title = v.edtTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            v.edtTitle.setError(getString(R.string.task_need_title));
            Toast.makeText(this, R.string.task_need_title, Toast.LENGTH_SHORT).show();
            return;
        }

        int reminderType = selectedReminderType();
        int repeat = selectedRepeat();

        task.setTitle(title);
        task.setNotes(v.edtNotes.getText().toString().trim());
        task.setCategory(selectedCategory());
        task.setDueAt(due.getTimeInMillis());
        task.setRepeatMode(repeat);
        task.setReminderType(reminderType);
        task.setLeadMinutes(LEAD_MINUTES[v.spLead.getSelectedItemPosition()]);
        task.setSnoozeMinutes(SNOOZE_MINUTES[v.spSnooze.getSelectedItemPosition()]);

        // A one-shot reminder in the past would never fire; a repeating one just rolls on.
        if (reminderType != TaskItem.REMIND_NONE
                && repeat == TaskItem.REPEAT_ONCE
                && task.getTriggerAt() <= System.currentTimeMillis()) {
            Toast.makeText(this, R.string.task_need_future, Toast.LENGTH_LONG).show();
            return;
        }

        // Editing a task that was already ticked off re-opens it.
        if (task.isDone() && task.getDueAt() > System.currentTimeMillis()) {
            task.setDone(false);
            task.setCompletedAt(0);
        }

        if (isEdit) {
            db.updateTask(task);
        } else {
            db.addTask(task);
        }

        ReminderScheduler.schedule(this, task);

        Toast.makeText(this, reminderType == TaskItem.REMIND_NONE
                ? R.string.task_saved_no_remind : R.string.task_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.task_delete_confirm)
                .setMessage(R.string.task_delete_confirm_desc)
                .setPositiveButton(R.string.task_delete, (dialog, which) -> {
                    ReminderScheduler.cancel(this, task);
                    db.deleteTask(task.getId());
                    Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.task_cancel, null)
                .show();
    }
}

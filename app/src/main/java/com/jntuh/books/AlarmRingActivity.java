package com.jntuh.books;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.jntuh.books.databinding.ActivityAlarmRingBinding;
import com.jntuh.item.TaskCategory;
import com.jntuh.item.TaskItem;
import com.jntuh.service.AlarmSoundService;
import com.jntuh.service.TaskActionReceiver;
import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.ReminderScheduler;
import com.jntuh.util.TaskDates;
import com.jntuh.util.TaskNotifier;

import java.util.Date;

/**
 * The alarm takeover. Launched by the notification's full-screen intent, so it has to be
 * able to appear over the lock screen and turn the display on by itself.
 *
 * <p>It does not own the audio — {@link AlarmSoundService} does — so being denied on
 * Android 14+ costs the user the big screen, not the ringing.
 */
public class AlarmRingActivity extends AppCompatActivity {

    private ActivityAlarmRingBinding v;
    private DatabaseHandler db;
    private TaskItem task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showOverLockScreen();

        v = ActivityAlarmRingBinding.inflate(getLayoutInflater());
        setContentView(v.getRoot());

        db = new DatabaseHandler(this);
        long taskId = getIntent() == null ? -1 : getIntent().getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1);
        task = taskId >= 0 ? db.getTask(taskId) : null;

        if (task == null) {
            // Deleted while ringing — nothing to show.
            stopRinging();
            finish();
            return;
        }

        bind();

        v.btnAlarmDismiss.setOnClickListener(view -> {
            stopRinging();
            finish();
        });

        v.btnAlarmSnooze.setOnClickListener(view -> {
            stopRinging();
            ReminderScheduler.snooze(this, task);
            finish();
        });

        v.btnAlarmDone.setOnClickListener(view -> {
            stopRinging();
            TaskActionReceiver.markDone(this, db, task);
            finish();
        });
    }

    /**
     * Turn the screen on and show above the keyguard. The pre-27 window flags are still
     * needed because setShowWhenLocked/setTurnScreenOn only exist from Oreo MR1.
     */
    private void showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguard = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguard != null) {
                keyguard.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void bind() {
        long now = System.currentTimeMillis();
        v.tvAlarmClock.setText(TaskDates.timeFormat().format(new Date(now)));
        v.tvAlarmDate.setText(TaskDates.longDateFormat().format(new Date(now)));

        v.tvAlarmTitle.setText(task.getTitle());
        v.tvAlarmCategory.setText(getString(TaskCategory.labelOf(task.getCategory())).toUpperCase());
        v.ivAlarmIcon.setImageResource(TaskCategory.iconOf(task.getCategory()));
        v.ivAlarmIcon.setColorFilter(ContextCompat.getColor(this, TaskCategory.colorOf(task.getCategory())));

        String notes = !TextUtils.isEmpty(task.getNotes()) ? task.getNotes() : task.getBookTitle();
        v.tvAlarmNotes.setVisibility(TextUtils.isEmpty(notes) ? View.GONE : View.VISIBLE);
        v.tvAlarmNotes.setText(notes);

        v.btnAlarmSnooze.setText(getString(R.string.alarm_snooze));
    }

    private void stopRinging() {
        AlarmSoundService.stop(this);
        if (task != null) {
            TaskNotifier.cancel(this, task.getId());
        }
    }

    /**
     * The back gesture must not silently leave the alarm ringing behind an empty screen —
     * treat it as a dismiss.
     */
    @Override
    public void onBackPressed() {
        stopRinging();
        super.onBackPressed();
    }
}

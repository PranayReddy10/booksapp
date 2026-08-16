package com.jntuh.item;

import java.io.Serializable;

/**
 * A single student task / reminder. Rows live only on the device (see
 * {@link com.jntuh.util.DatabaseHandler}); the server knows nothing about them yet.
 * The {@code serverId / updatedAt / syncState / deleted} quartet is unused today and
 * exists so a future sync can be added without migrating a table full of live data.
 */
public class TaskItem implements Serializable {

    /** Reminder styles, stored as ints so the column never has to change. */
    public static final int REMIND_NONE = 0;
    public static final int REMIND_NOTIFICATION = 1;
    public static final int REMIND_ALARM = 2;

    /** Repeat rules. */
    public static final int REPEAT_ONCE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_WEEKDAYS = 2;
    public static final int REPEAT_WEEKLY = 3;
    public static final int REPEAT_MONTHLY = 4;

    /** Sync bookkeeping (reserved). */
    public static final int SYNC_LOCAL = 0;
    public static final int SYNC_SYNCED = 1;
    public static final int SYNC_DIRTY = 2;

    private long id;
    private String title = "";
    private String notes = "";
    private String category = TaskCategory.STUDY;

    /** When the task is due, epoch millis. The reminder fires leadMinutes before this. */
    private long dueAt;
    private int repeatMode = REPEAT_ONCE;
    private int reminderType = REMIND_NOTIFICATION;
    private int leadMinutes;
    private int snoozeMinutes = 10;

    private boolean done;
    private long completedAt;

    /** Optional tie-in with the library: the book this task is about. */
    private String bookId = "";
    private String bookTitle = "";

    private long createdAt;

    // ---- reserved for a future server sync ----
    private String serverId = "";
    private long updatedAt;
    private int syncState = SYNC_LOCAL;
    private boolean deleted;

    public TaskItem() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? TaskCategory.OTHER : category;
    }

    public long getDueAt() {
        return dueAt;
    }

    public void setDueAt(long dueAt) {
        this.dueAt = dueAt;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(int repeatMode) {
        this.repeatMode = repeatMode;
    }

    public int getReminderType() {
        return reminderType;
    }

    public void setReminderType(int reminderType) {
        this.reminderType = reminderType;
    }

    public int getLeadMinutes() {
        return leadMinutes;
    }

    public void setLeadMinutes(int leadMinutes) {
        this.leadMinutes = leadMinutes;
    }

    public int getSnoozeMinutes() {
        return snoozeMinutes;
    }

    public void setSnoozeMinutes(int snoozeMinutes) {
        this.snoozeMinutes = snoozeMinutes;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId == null ? "" : bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle == null ? "" : bookTitle;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId == null ? "" : serverId;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getSyncState() {
        return syncState;
    }

    public void setSyncState(int syncState) {
        this.syncState = syncState;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /** The instant the user should actually be alerted. */
    public long getTriggerAt() {
        return dueAt - (leadMinutes * 60_000L);
    }

    public boolean isRepeating() {
        return repeatMode != REPEAT_ONCE;
    }

    public boolean wantsReminder() {
        return reminderType != REMIND_NONE;
    }

    public boolean isAlarm() {
        return reminderType == REMIND_ALARM;
    }

    /** Overdue means: due in the past and still not ticked off. */
    public boolean isOverdue() {
        return !done && dueAt < System.currentTimeMillis();
    }
}

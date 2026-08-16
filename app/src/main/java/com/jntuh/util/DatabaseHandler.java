package com.jntuh.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.jntuh.item.DownloadList;
import com.jntuh.item.TaskItem;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    // v2 added the reminder tasks table.
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "MyBook";

    // book download table name
    private static final String TABLE_NAME_DOWNLOAD = "book_download";

    // book Table Columns names
    private static final String ID = "auto_id";
    private static final String KEY_BOOK_ID = "book_id";
    private static final String KEY_BOOK_TITLE = "book_title";
    private static final String KEY_BOOK_IMAGE = "image";
    private static final String KEY_BOOK_FILE_URL = "book_file_url";
    private static final String KEY_BOOK_AUTHOR_NAME = "book_author_name";

    // reminder task table
    private static final String TABLE_NAME_TASK = "tasks";

    private static final String T_ID = "id";
    private static final String T_TITLE = "title";
    private static final String T_NOTES = "notes";
    private static final String T_CATEGORY = "category";
    private static final String T_DUE_AT = "due_at";
    private static final String T_REPEAT = "repeat_mode";
    private static final String T_REMINDER = "reminder_type";
    private static final String T_LEAD = "lead_minutes";
    private static final String T_SNOOZE = "snooze_minutes";
    private static final String T_DONE = "is_done";
    private static final String T_COMPLETED_AT = "completed_at";
    private static final String T_BOOK_ID = "book_id";
    private static final String T_BOOK_TITLE = "book_title";
    private static final String T_CREATED_AT = "created_at";
    // reserved for a future server sync — written but never read today
    private static final String T_SERVER_ID = "server_id";
    private static final String T_UPDATED_AT = "updated_at";
    private static final String T_SYNC_STATE = "sync_state";
    private static final String T_DELETED = "is_deleted";

    private static final String CREATE_TABLE_TASK = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_TASK + "("
            + T_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + T_TITLE + " TEXT,"
            + T_NOTES + " TEXT,"
            + T_CATEGORY + " TEXT,"
            + T_DUE_AT + " INTEGER,"
            + T_REPEAT + " INTEGER DEFAULT 0,"
            + T_REMINDER + " INTEGER DEFAULT 1,"
            + T_LEAD + " INTEGER DEFAULT 0,"
            + T_SNOOZE + " INTEGER DEFAULT 10,"
            + T_DONE + " INTEGER DEFAULT 0,"
            + T_COMPLETED_AT + " INTEGER DEFAULT 0,"
            + T_BOOK_ID + " TEXT,"
            + T_BOOK_TITLE + " TEXT,"
            + T_CREATED_AT + " INTEGER DEFAULT 0,"
            + T_SERVER_ID + " TEXT,"
            + T_UPDATED_AT + " INTEGER DEFAULT 0,"
            + T_SYNC_STATE + " INTEGER DEFAULT 0,"
            + T_DELETED + " INTEGER DEFAULT 0" + ")";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE_DOWNLOAD = "CREATE TABLE " + TABLE_NAME_DOWNLOAD + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT ," + KEY_BOOK_ID + " TEXT,"
                + KEY_BOOK_TITLE + " TEXT," + KEY_BOOK_IMAGE + " TEXT,"
                + KEY_BOOK_AUTHOR_NAME + "" + " TEXT," + KEY_BOOK_FILE_URL + " TEXT" + ")";
        db.execSQL(CREATE_TABLE_DOWNLOAD);

        db.execSQL(CREATE_TABLE_TASK);

    }

    /**
     * Migrations are additive on purpose. This used to DROP the download table on every
     * version bump, which would have wiped the user's downloaded books the moment the
     * schema changed — so each step now only creates what that version introduced.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_TASK);
        }
    }

    //-------------Download Table-------------------//

    // Adding new book detail
    public void addDownload(DownloadList downloadList) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_BOOK_ID, downloadList.getId());
        values.put(KEY_BOOK_TITLE, downloadList.getTitle());
        values.put(KEY_BOOK_IMAGE, downloadList.getImage());
        values.put(KEY_BOOK_AUTHOR_NAME, downloadList.getAuthor());
        values.put(KEY_BOOK_FILE_URL, downloadList.getUrl());

        // Inserting Row
        db.insert(TABLE_NAME_DOWNLOAD, null, values);
        db.close(); // Closing database connection
    }

    // Getting All book
    public List<DownloadList> getDownload() {
        List<DownloadList> downloadLists = new ArrayList<DownloadList>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_NAME_DOWNLOAD;

        SQLiteDatabase db = this.getWritableDatabase();
        @SuppressLint("Recycle") Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                DownloadList list = new DownloadList();
                list.setId(cursor.getString(1));
                list.setTitle(cursor.getString(2));
                list.setImage(cursor.getString(3));
                list.setAuthor(cursor.getString(4));
                list.setUrl(cursor.getString(5));

                // Adding book to list
                downloadLists.add(list);
            } while (cursor.moveToNext());
        }

        // return book list
        return downloadLists;
    }

    // Deleting single book
    public void deleteDownloadBook(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME_DOWNLOAD, KEY_BOOK_ID + "=" + id, null);
    }

    //check book id in database or not
    public boolean checkIdDownloadBook(String id) {
        String selectQuery = "SELECT  * FROM " + TABLE_NAME_DOWNLOAD + " WHERE " + KEY_BOOK_ID + "=" + id;
        SQLiteDatabase db = this.getWritableDatabase();
        @SuppressLint("Recycle") Cursor cursor = db.rawQuery(selectQuery, null);
        return cursor.getCount() == 0;
    }

    //-------------Download Table-------------------//


    //-------------Task Table-------------------//

    private ContentValues taskValues(TaskItem task) {
        ContentValues values = new ContentValues();
        values.put(T_TITLE, task.getTitle());
        values.put(T_NOTES, task.getNotes());
        values.put(T_CATEGORY, task.getCategory());
        values.put(T_DUE_AT, task.getDueAt());
        values.put(T_REPEAT, task.getRepeatMode());
        values.put(T_REMINDER, task.getReminderType());
        values.put(T_LEAD, task.getLeadMinutes());
        values.put(T_SNOOZE, task.getSnoozeMinutes());
        values.put(T_DONE, task.isDone() ? 1 : 0);
        values.put(T_COMPLETED_AT, task.getCompletedAt());
        values.put(T_BOOK_ID, task.getBookId());
        values.put(T_BOOK_TITLE, task.getBookTitle());
        values.put(T_CREATED_AT, task.getCreatedAt());
        values.put(T_SERVER_ID, task.getServerId());
        values.put(T_UPDATED_AT, System.currentTimeMillis());
        values.put(T_SYNC_STATE, task.getSyncState());
        values.put(T_DELETED, task.isDeleted() ? 1 : 0);
        return values;
    }

    /** Inserts and returns the new row id, which doubles as the alarm request code. */
    public long addTask(TaskItem task) {
        if (task.getCreatedAt() == 0) {
            task.setCreatedAt(System.currentTimeMillis());
        }
        SQLiteDatabase db = this.getWritableDatabase();
        long id = db.insert(TABLE_NAME_TASK, null, taskValues(task));
        task.setId(id);
        return id;
    }

    public void updateTask(TaskItem task) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.update(TABLE_NAME_TASK, taskValues(task), T_ID + "=?",
                new String[]{String.valueOf(task.getId())});
    }

    /**
     * Hard delete. Soft-delete columns exist for a future sync, but with no server to
     * tell about the removal there is nothing to be gained from keeping the tombstone.
     */
    public void deleteTask(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME_TASK, T_ID + "=?", new String[]{String.valueOf(id)});
    }

    public TaskItem getTask(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME_TASK, null, T_ID + "=? AND " + T_DELETED + "=0",
                new String[]{String.valueOf(id)}, null, null, null);
        TaskItem task = null;
        try {
            if (cursor.moveToFirst()) {
                task = readTask(cursor);
            }
        } finally {
            cursor.close();
        }
        return task;
    }

    /** Every live task, soonest first. */
    public List<TaskItem> getTasks() {
        return queryTasks(T_DELETED + "=0", null);
    }

    /** Tasks whose due time falls inside [from, to). Used by the day view. */
    public List<TaskItem> getTasksBetween(long from, long to) {
        return queryTasks(T_DELETED + "=0 AND " + T_DUE_AT + ">=? AND " + T_DUE_AT + "<?",
                new String[]{String.valueOf(from), String.valueOf(to)});
    }

    /**
     * Unfinished tasks that still have a reminder in the future — the set that needs a
     * live alarm. Used after a reboot and by the daily sweep.
     */
    public List<TaskItem> getTasksToSchedule() {
        return queryTasks(T_DELETED + "=0 AND " + T_DONE + "=0 AND " + T_REMINDER + "!=0", null);
    }

    public int countPending() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME_TASK
                + " WHERE " + T_DELETED + "=0 AND " + T_DONE + "=0", null);
        int count = 0;
        try {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }
        return count;
    }

    private List<TaskItem> queryTasks(String selection, String[] args) {
        List<TaskItem> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME_TASK, null, selection, args,
                null, null, T_DUE_AT + " ASC");
        try {
            if (cursor.moveToFirst()) {
                do {
                    tasks.add(readTask(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return tasks;
    }

    /** Reads by column name so a future added column cannot shift the mapping. */
    private TaskItem readTask(Cursor c) {
        TaskItem task = new TaskItem();
        task.setId(c.getLong(c.getColumnIndexOrThrow(T_ID)));
        task.setTitle(c.getString(c.getColumnIndexOrThrow(T_TITLE)));
        task.setNotes(c.getString(c.getColumnIndexOrThrow(T_NOTES)));
        task.setCategory(c.getString(c.getColumnIndexOrThrow(T_CATEGORY)));
        task.setDueAt(c.getLong(c.getColumnIndexOrThrow(T_DUE_AT)));
        task.setRepeatMode(c.getInt(c.getColumnIndexOrThrow(T_REPEAT)));
        task.setReminderType(c.getInt(c.getColumnIndexOrThrow(T_REMINDER)));
        task.setLeadMinutes(c.getInt(c.getColumnIndexOrThrow(T_LEAD)));
        task.setSnoozeMinutes(c.getInt(c.getColumnIndexOrThrow(T_SNOOZE)));
        task.setDone(c.getInt(c.getColumnIndexOrThrow(T_DONE)) == 1);
        task.setCompletedAt(c.getLong(c.getColumnIndexOrThrow(T_COMPLETED_AT)));
        task.setBookId(c.getString(c.getColumnIndexOrThrow(T_BOOK_ID)));
        task.setBookTitle(c.getString(c.getColumnIndexOrThrow(T_BOOK_TITLE)));
        task.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(T_CREATED_AT)));
        task.setServerId(c.getString(c.getColumnIndexOrThrow(T_SERVER_ID)));
        task.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(T_UPDATED_AT)));
        task.setSyncState(c.getInt(c.getColumnIndexOrThrow(T_SYNC_STATE)));
        task.setDeleted(c.getInt(c.getColumnIndexOrThrow(T_DELETED)) == 1);
        return task;
    }

    //-------------Task Table-------------------//


}

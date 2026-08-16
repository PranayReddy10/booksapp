package com.jntuh.item;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.jntuh.books.R;

/**
 * The fixed set of task categories a student can pick from. Stored as the string
 * constant so the value survives reordering; unknown values fall back to OTHER.
 */
public final class TaskCategory {

    public static final String STUDY = "study";
    public static final String EXAM = "exam";
    public static final String ASSIGNMENT = "assignment";
    public static final String READING = "reading";
    public static final String RESULT = "result";
    public static final String OTHER = "other";

    public static final String[] ALL = {STUDY, EXAM, ASSIGNMENT, READING, RESULT, OTHER};

    private TaskCategory() {
    }

    @StringRes
    public static int labelOf(String category) {
        if (category == null) return R.string.task_cat_other;
        switch (category) {
            case STUDY:
                return R.string.task_cat_study;
            case EXAM:
                return R.string.task_cat_exam;
            case ASSIGNMENT:
                return R.string.task_cat_assignment;
            case READING:
                return R.string.task_cat_reading;
            case RESULT:
                return R.string.task_cat_result;
            default:
                return R.string.task_cat_other;
        }
    }

    /** Accent used for the category pill and the card's left rail. */
    @ColorRes
    public static int colorOf(String category) {
        if (category == null) return R.color.task_cat_other;
        switch (category) {
            case STUDY:
                return R.color.task_cat_study;
            case EXAM:
                return R.color.task_cat_exam;
            case ASSIGNMENT:
                return R.color.task_cat_assignment;
            case READING:
                return R.color.task_cat_reading;
            case RESULT:
                return R.color.task_cat_result;
            default:
                return R.color.task_cat_other;
        }
    }

    @DrawableRes
    public static int iconOf(String category) {
        if (category == null) return R.drawable.ic_task_other;
        switch (category) {
            case STUDY:
                return R.drawable.ic_task_study;
            case EXAM:
                return R.drawable.ic_task_exam;
            case ASSIGNMENT:
                return R.drawable.ic_task_assignment;
            case READING:
                return R.drawable.ic_task_reading;
            case RESULT:
                return R.drawable.ic_result_school;
            default:
                return R.drawable.ic_task_other;
        }
    }
}

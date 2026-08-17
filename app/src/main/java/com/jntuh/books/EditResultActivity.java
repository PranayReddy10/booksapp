package com.jntuh.books;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.item.ResultItem;
import com.jntuh.item.SemesterItem;
import com.jntuh.item.SubjectItem;
import com.jntuh.response.ReportCardRP;
import com.jntuh.response.ResultRP;
import com.jntuh.response.ResultSaveRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.GradeUtil;
import com.jntuh.util.Method;
import com.jntuh.books.databinding.ActivityEditResultBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Add / edit the current user's own result. Builds semesters and subjects
 * dynamically, computes grade points client-side for instant feedback, then
 * ships everything to result_save as a semesters_json string and asks the
 * server to (re)generate the report card. Reachable only when the record is
 * NOT locked; a locked record is re-checked on load and closes back to the
 * read-only view.
 */
public class EditResultActivity extends AppCompatActivity {

    public static final String EXTRA_HAS_RESULT = "has_result";

    private ActivityEditResultBinding binding;
    private Method method;

    // Profile-sourced identity (read-only on this screen).
    private String pRoll = "", pBranch = "", pRegulation = "", pDegree = "", pName = "";

    // Runtime view models: one editor view per semester, each holding subject views.
    private final List<SemesterEditor> semesterEditors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        method.forceRTLIfSupported();

        binding.ivBack.setOnClickListener(v -> finish());
        binding.btnAddSemester.setOnClickListener(v -> addSemester(null));
        binding.btnSaveResult.setOnClickListener(v -> save());
        binding.btnGoCompleteProfile.setOnClickListener(v -> {
            // Straight to Edit Profile — that's where roll number, branch and
            // regulation are filled in; this screen reloads when the user returns.
            startActivity(new Intent(EditResultActivity.this, EditProfileActivity.class));
            finish();
        });

        // The results form pulls identity from the profile. Fetch it first, gate
        // on completeness, then load any existing result.
        if (method.isNetworkAvailable()) {
            fetchProfileThenLoad();
        } else {
            method.alertBox(getString(R.string.internet_connection));
            finish();
        }
    }

    /** Fetch profile; populate read-only identity; block if incomplete. */
    private void fetchProfileThenLoad() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.getProfileData(API.toBase64(jsObj.toString())).enqueue(new Callback<com.jntuh.response.LoginRP>() {
            @Override
            public void onResponse(@NotNull Call<com.jntuh.response.LoginRP> call,
                                   @NotNull Response<com.jntuh.response.LoginRP> resp) {
                if (resp.body() != null && resp.body().getItemUserList() != null
                        && !resp.body().getItemUserList().isEmpty()) {
                    com.jntuh.response.LoginRP.ItemUser u = resp.body().getItemUserList().get(0);
                    pRoll = nn(u.getRollnumber());
                    pBranch = nn(u.getBranch());
                    pRegulation = nn(u.getRegulation());
                    pDegree = nn(u.getDegree());
                    pName = nn(u.getName());
                }
                boolean complete = !pRoll.isEmpty() && !pBranch.isEmpty();
                if (!complete) {
                    showBlocker(true);
                    return;
                }
                showBlocker(false);
                bindIdentity();
                afterProfileReady();
            }

            @Override
            public void onFailure(@NotNull Call<com.jntuh.response.LoginRP> call, @NotNull Throwable t) {
                method.alertBox(getString(R.string.failed_try_again));
                finish();
            }
        });
    }

    private void showBlocker(boolean show) {
        binding.llProfileBlocker.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.scrollResultForm.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.llSaveBar.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void bindIdentity() {
        binding.tvProfHallTicket.setText(pRoll);
        binding.tvProfName.setText(pName);
        StringBuilder br = new StringBuilder();
        if (!pBranch.isEmpty()) br.append(pBranch);
        if (!pRegulation.isEmpty()) { if (br.length() > 0) br.append("  ·  "); br.append(pRegulation); }
        if (!pDegree.isEmpty()) { if (br.length() > 0) br.append("  ·  "); br.append(pDegree); }
        binding.tvProfBranchReg.setText(br.toString());
    }

    private void afterProfileReady() {
        boolean hasResult = getIntent().getBooleanExtra(EXTRA_HAS_RESULT, false);
        if (hasResult) {
            binding.tvEditTitle.setText(getString(R.string.result_edit_title));
            loadExisting();
        } else {
            addSemester(null);
        }
    }

    // ---------------------------------------------------------------- load

    private void loadExisting() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.getResult(API.toBase64(jsObj.toString())).enqueue(new Callback<ResultRP>() {
            @Override
            public void onResponse(@NotNull Call<ResultRP> call, @NotNull Response<ResultRP> resp) {
                ResultItem item = null;
                if (resp.body() != null && resp.body().getResultItems() != null
                        && !resp.body().getResultItems().isEmpty()) {
                    item = resp.body().getResultItems().get(0);
                }
                if (item != null && item.getHas_result() == 1) {
                    prefill(item);
                    // If literally every semester is locked, there's nothing to
                    // edit in place — but the user can still ADD a new semester,
                    // so we keep the editor open and just show a hint.
                    if (item.getLocked() == 1) {
                        Toast.makeText(EditResultActivity.this,
                                getString(R.string.result_sem_locked_note), Toast.LENGTH_LONG).show();
                    }
                } else {
                    addSemester(null);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResultRP> call, @NotNull Throwable t) {
                addSemester(null);
            }
        });
    }

    private void prefill(ResultItem item) {
        // Identity is shown read-only from the profile (bindIdentity()); here we
        // only rebuild the semester editors from the saved result.
        binding.llSemesterEditors.removeAllViews();
        semesterEditors.clear();
        if (item.getSemesters() != null && !item.getSemesters().isEmpty()) {
            for (SemesterItem s : item.getSemesters()) addSemester(s);
        } else {
            addSemester(null);
        }
    }

    // ------------------------------------------------------------ builders

    private void addSemester(SemesterItem model) {
        LayoutInflater inflater = getLayoutInflater();
        View block = inflater.inflate(R.layout.item_edit_semester, binding.llSemesterEditors, false);

        SemesterEditor ed = new SemesterEditor();
        ed.root = block;
        ed.tvTitle = block.findViewById(R.id.tvEditSemTitle);
        ed.etSemCode = block.findViewById(R.id.etSemCode);
        ed.etExamMonth = block.findViewById(R.id.etExamMonth);
        ed.llSubjects = block.findViewById(R.id.llEditSubjects);
        View btnAddSubject = block.findViewById(R.id.btnAddSubject);
        View ivRemoveSem = block.findViewById(R.id.ivRemoveSem);

        btnAddSubject.setOnClickListener(v -> addSubject(ed, null));
        ivRemoveSem.setOnClickListener(v -> {
            binding.llSemesterEditors.removeView(block);
            semesterEditors.remove(ed);
            renumber();
        });

        // A verified/locked semester is shown read-only: no edits, no removal,
        // no add-subject. The student can still add other semesters.
        ed.locked = (model != null && model.getLocked() == 1);
        if (ed.locked) {
            ed.etSemCode.setEnabled(false);
            ed.etExamMonth.setEnabled(false);
            btnAddSubject.setVisibility(View.GONE);
            ivRemoveSem.setVisibility(View.GONE);
            View lockTag = block.findViewById(R.id.tvSemLockedTag);
            if (lockTag != null) lockTag.setVisibility(View.VISIBLE);
        }

        if (model != null) {
            ed.etSemCode.setText(nn(model.getSem_code()));
            ed.etExamMonth.setText(nn(model.getExam_month_year()));
            if (model.getSubjects() != null && !model.getSubjects().isEmpty()) {
                for (SubjectItem s : model.getSubjects()) addSubject(ed, s);
            } else {
                addSubject(ed, null);
            }
        } else {
            addSubject(ed, null);
        }

        semesterEditors.add(ed);
        binding.llSemesterEditors.addView(block);
        block.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                this, R.anim.result_row_slide_in));
        renumber();
    }

    @SuppressLint("SetTextI18n")
    private void addSubject(SemesterEditor sem, SubjectItem model) {
        LayoutInflater inflater = getLayoutInflater();
        View row = inflater.inflate(R.layout.item_edit_subject, sem.llSubjects, false);

        SubjectEditor se = new SubjectEditor();
        se.root = row;
        se.etCode = row.findViewById(R.id.etSubCode);
        se.etName = row.findViewById(R.id.etSubName);
        se.etCredits = row.findViewById(R.id.etSubCredits);
        se.spGrade = row.findViewById(R.id.spSubGrade);
        se.tvPoints = row.findViewById(R.id.tvSubPointsLive);
        se.cbBacklog = row.findViewById(R.id.cbBacklog);
        se.etInternal = row.findViewById(R.id.etInternal);
        se.etExternal = row.findViewById(R.id.etExternal);
        se.etTotal = row.findViewById(R.id.etTotal);
        View ivRemove = row.findViewById(R.id.ivRemoveSubject);
        View btnToggleMarks = row.findViewById(R.id.btnToggleMarks);
        LinearLayout llMarks = row.findViewById(R.id.llMarksRow);

        ArrayAdapter<String> gradeAdapter =
                new ArrayAdapter<>(this, R.layout.row_spinner_item, GradeUtil.GRADES);
        se.spGrade.setAdapter(gradeAdapter);

        ivRemove.setOnClickListener(v -> {
            sem.llSubjects.removeView(row);
            sem.subjects.remove(se);
        });

        btnToggleMarks.setOnClickListener(v ->
                llMarks.setVisibility(llMarks.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        // Live grade points on credits/grade change.
        Runnable recompute = () -> {
            Double credits = parseD(se.etCredits.getText().toString());
            String grade = (String) se.spGrade.getSelectedItem();
            Double pts = GradeUtil.gradePoints(grade, credits);
            se.tvPoints.setText(pts == null ? "—" : fmt(pts));
            // F/Ab auto-ticks backlog (user may untick afterwards).
            if (GradeUtil.isFailGrade(grade) && !se.userTouchedBacklog) {
                se.cbBacklog.setChecked(true);
            }
        };
        se.etCredits.addTextChangedListener(new SimpleWatcher(recompute));
        se.spGrade.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View vv, int pos, long id) { recompute.run(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        se.cbBacklog.setOnClickListener(v -> se.userTouchedBacklog = true);

        if (model != null) {
            se.etCode.setText(nn(model.getSubject_code()));
            se.etName.setText(nn(model.getSubject_name()));
            if (model.getCredits() != null) se.etCredits.setText(fmt(model.getCredits()));
            selectGrade(se.spGrade, model.getGrade());
            se.cbBacklog.setChecked(model.getIs_backlog() == 1);
            se.userTouchedBacklog = true;
            boolean anyMark = model.getInternal() != null || model.getExternal() != null || model.getTotal() != null;
            if (anyMark) {
                llMarks.setVisibility(View.VISIBLE);
                if (model.getInternal() != null) se.etInternal.setText(String.valueOf(model.getInternal()));
                if (model.getExternal() != null) se.etExternal.setText(String.valueOf(model.getExternal()));
                if (model.getTotal() != null) se.etTotal.setText(String.valueOf(model.getTotal()));
            }
            recompute.run();
        }

        sem.subjects.add(se);
        sem.llSubjects.addView(row);
        if (sem.locked) {
            se.etCode.setEnabled(false);
            se.etName.setEnabled(false);
            se.etCredits.setEnabled(false);
            se.spGrade.setEnabled(false);
            se.cbBacklog.setEnabled(false);
            se.etInternal.setEnabled(false);
            se.etExternal.setEnabled(false);
            se.etTotal.setEnabled(false);
            ivRemove.setVisibility(View.GONE);
        }
        row.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                this, R.anim.result_row_slide_in));
    }

    @SuppressLint("SetTextI18n")
    private void renumber() {
        for (int i = 0; i < semesterEditors.size(); i++) {
            semesterEditors.get(i).tvTitle.setText("Semester " + (i + 1));
        }
    }

    // -------------------------------------------------------------- save

    private void save() {
        List<SemesterItem> semesters = new ArrayList<>();
        int backlogCount = 0;
        for (SemesterEditor sem : semesterEditors) {
            // Never submit a locked (verified) semester — server ignores it too,
            // but we skip it client-side so nothing is needlessly re-sent.
            if (sem.locked) continue;

            List<SubjectItem> subjects = new ArrayList<>();
            for (SubjectEditor se : sem.subjects) {
                String code = se.etCode.getText().toString().trim();
                String name = se.etName.getText().toString().trim();
                Double credits = parseD(se.etCredits.getText().toString());
                String grade = (String) se.spGrade.getSelectedItem();
                if (code.isEmpty() && name.isEmpty() && credits == null) continue; // skip blank

                SubjectItem s = new SubjectItem();
                s.setSubject_code(code);
                s.setSubject_name(name);
                s.setCredits(credits);
                s.setGrade(grade);
                s.setGrade_points(GradeUtil.gradePoints(grade, credits)); // optional; server validates
                boolean backlog = se.cbBacklog.isChecked();
                s.setIs_backlog(backlog ? 1 : 0);
                if (backlog) backlogCount++;
                s.setInternal(parseI(se.etInternal.getText().toString()));
                s.setExternal(parseI(se.etExternal.getText().toString()));
                s.setTotal(parseI(se.etTotal.getText().toString()));
                subjects.add(s);
            }
            if (subjects.isEmpty()) continue;

            SemesterItem si = new SemesterItem();
            si.setSem_code(sem.etSemCode.getText().toString().trim());
            si.setExam_month_year(sem.etExamMonth.getText().toString().trim());
            si.setSubjects(subjects);
            semesters.add(si);
        }

        if (semesters.isEmpty()) {
            Toast.makeText(this, getString(R.string.result_need_one_semester), Toast.LENGTH_SHORT).show();
            return;
        }

        setSaving(true);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        // Identity comes from the profile server-side; we still send hall_ticket
        // (= profile roll) so an admin-added row links on first save.
        jsObj.addProperty("hall_ticket_no", pRoll);
        jsObj.addProperty("student_name", pName);
        jsObj.addProperty("backlogs_count", String.valueOf(backlogCount));
        jsObj.addProperty("semesters_json", new Gson().toJson(semesters)); // list -> JSON string

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.saveResult(API.toBase64(jsObj.toString())).enqueue(new Callback<ResultSaveRP>() {
            @Override
            public void onResponse(@NotNull Call<ResultSaveRP> call, @NotNull Response<ResultSaveRP> resp) {
                ResultSaveRP body = resp.body();
                boolean ok = body != null && body.getEbookApp() != null && !body.getEbookApp().isEmpty()
                        && "1".equals(body.getEbookApp().get(0).getSuccess());
                if (ok) {
                    // Kick off report generation, then close back to the tab.
                    generateThenFinish();
                } else {
                    setSaving(false);
                    String msg = getString(R.string.failed_try_again);
                    if (body != null && body.getEbookApp() != null && !body.getEbookApp().isEmpty()
                            && body.getEbookApp().get(0).getMsg() != null) {
                        msg = body.getEbookApp().get(0).getMsg();
                    }
                    method.alertBox(msg);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResultSaveRP> call, @NotNull Throwable t) {
                setSaving(false);
                method.alertBox(getString(R.string.failed_try_again));
            }
        });
    }

    private void generateThenFinish() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.generateReportCard(API.toBase64(jsObj.toString())).enqueue(new Callback<ReportCardRP>() {
            @Override
            public void onResponse(@NotNull Call<ReportCardRP> call, @NotNull Response<ReportCardRP> resp) {
                setSaving(false);
                Toast.makeText(EditResultActivity.this,
                        getString(R.string.result_saved), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(@NotNull Call<ReportCardRP> call, @NotNull Throwable t) {
                // Save already succeeded; the card can be generated later from the tab.
                setSaving(false);
                Toast.makeText(EditResultActivity.this,
                        getString(R.string.result_saved), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setSaving(boolean saving) {
        binding.btnSaveResult.setEnabled(!saving);
        binding.progressSave.setVisibility(saving ? View.VISIBLE : View.GONE);
        binding.tvSaveLabel.setText(saving ? "…" : getString(R.string.result_save));
    }

    // ------------------------------------------------------------ helpers

    private void selectGrade(Spinner sp, String grade) {
        if (grade == null) return;
        for (int i = 0; i < GradeUtil.GRADES.length; i++) {
            if (GradeUtil.GRADES[i].equalsIgnoreCase(grade.trim())) {
                sp.setSelection(i);
                return;
            }
        }
    }

    private static String nn(String s) {
        return s == null ? "" : s;
    }

    private static Double parseD(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseI(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static String fmt(Double d) {
        if (d == null) return "—";
        if (d == Math.rint(d) && !Double.isInfinite(d)) return String.valueOf((long) (double) d);
        return String.format(Locale.US, "%.2f", d);
    }

    // Simple text watcher wrapper.
    private static class SimpleWatcher implements TextWatcher {
        private final Runnable r;
        SimpleWatcher(Runnable r) { this.r = r; }
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void afterTextChanged(Editable s) { r.run(); }
    }

    // View-model holders.
    private static class SemesterEditor {
        View root;
        TextView tvTitle;
        EditText etSemCode, etExamMonth;
        LinearLayout llSubjects;
        boolean locked = false;
        final List<SubjectEditor> subjects = new ArrayList<>();
    }

    private static class SubjectEditor {
        View root;
        EditText etCode, etName, etCredits, etInternal, etExternal, etTotal;
        Spinner spGrade;
        TextView tvPoints;
        CheckBox cbBacklog;
        boolean userTouchedBacklog = false;
    }
}

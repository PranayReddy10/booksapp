package com.jntuh.util;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.jntuh.books.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a normal {@link Spinner} into a searchable one WITHOUT changing any of the
 * screen's existing logic. Tapping the spinner opens a dialog with a search box and a
 * filtered list; choosing an item calls {@code spinner.setSelection(originalIndex)},
 * so every existing {@code getSelectedItemPosition()} / OnItemSelectedListener keeps
 * working exactly as before.
 *
 * Usage (after the spinner's adapter is set/populated):
 *   SearchableSpinner.attach(spinner, "Select college");
 * Call attach() again (or it re-reads live) so it always searches the current items.
 */
public final class SearchableSpinner {

    private SearchableSpinner() {
    }

    public static void attach(final Spinner spinner, final String title) {
        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                showDialog(spinner, title);
                return true; // consume — don't open the default dropdown
            }
            return true;
        });
    }

    private static void showDialog(final Spinner spinner, final String title) {
        final Context ctx = spinner.getContext();
        if (spinner.getAdapter() == null || spinner.getAdapter().getCount() == 0) return;

        // Snapshot the current spinner items (as shown) with their real indexes.
        final int count = spinner.getAdapter().getCount();
        final List<String> allLabels = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allLabels.add(String.valueOf(spinner.getAdapter().getItem(i)));
        }

        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_searchable_spinner);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.9),
                    (int) (ctx.getResources().getDisplayMetrics().heightPixels * 0.7));
        }

        TextView tvTitle = dialog.findViewById(R.id.tvSearchTitle);
        final EditText etSearch = dialog.findViewById(R.id.etSearch);
        final ListView listView = dialog.findViewById(R.id.lvItems);
        tvTitle.setText(title);

        // Filtered view keeps a parallel list of the ORIGINAL indexes so selecting an
        // item maps back to the real spinner position.
        final List<String> shownLabels = new ArrayList<>(allLabels);
        final List<Integer> shownIndexes = new ArrayList<>();
        for (int i = 0; i < allLabels.size(); i++) shownIndexes.add(i);

        // Rows paint their own selected state (outline + check), so the picker reads
        // as a choice list rather than a plain dropdown.
        final ArrayAdapter<String> listAdapter =
                new ArrayAdapter<String>(ctx, R.layout.row_searchable_item, R.id.tvItem, shownLabels) {
                    @NonNull
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        View row = super.getView(position, convertView, parent);
                        boolean selected = position < shownIndexes.size()
                                && shownIndexes.get(position) == spinner.getSelectedItemPosition();

                        View rowBg = row.findViewById(R.id.llItemRow);
                        if (rowBg != null) {
                            rowBg.setBackgroundResource(selected
                                    ? R.drawable.bg_picker_row_selected : R.drawable.bg_picker_row);
                        }
                        View check = row.findViewById(R.id.ivItemCheck);
                        if (check != null) {
                            check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
                        }
                        TextView label = row.findViewById(R.id.tvItem);
                        if (label != null) {
                            label.setTextColor(ContextCompat.getColor(ctx,
                                    selected ? R.color.app_bg_orange : R.color.gray_2));
                        }
                        return row;
                    }
                };
        listView.setAdapter(listAdapter);
        listView.setDivider(null);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim().toLowerCase();
                shownLabels.clear();
                shownIndexes.clear();
                for (int i = 0; i < allLabels.size(); i++) {
                    String label = allLabels.get(i);
                    if (q.isEmpty() || label.toLowerCase().contains(q)) {
                        shownLabels.add(label);
                        shownIndexes.add(i);
                    }
                }
                listAdapter.notifyDataSetChanged();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int realIndex = shownIndexes.get(position);
            spinner.setSelection(realIndex); // fires the existing OnItemSelectedListener
            dialog.dismiss();
        });

        dialog.show();
    }
}

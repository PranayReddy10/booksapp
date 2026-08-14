package com.jntuh.books;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.jntuh.books.databinding.ActivityProfileSectionBinding;
import com.jntuh.fragment.ContinueFragment;
import com.jntuh.fragment.DashBoardFragment;
import com.jntuh.fragment.ManageBooksFragment;
import com.jntuh.fragment.MyResultsFragment;
import com.jntuh.fragment.RentBookFragment;
import com.jntuh.util.Method;

/**
 * Hosts one section of the profile as its own page. The profile screen lists its
 * sections as rows rather than tabs, and each row opens this activity with the
 * section it should show.
 */
public class ProfileSectionActivity extends AppCompatActivity {

    public static final String EXTRA_SECTION = "section";

    public static final String SECTION_RESULTS = "results";
    public static final String SECTION_CONTINUE = "continue";
    public static final String SECTION_UPLOADS = "uploads";
    public static final String SECTION_SUBSCRIPTION = "subscription";
    public static final String SECTION_RENT = "rent";

    private ActivityProfileSectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Method(this).forceRTLIfSupported();

        String section = getIntent().getStringExtra(EXTRA_SECTION);
        if (section == null) section = SECTION_RESULTS;

        binding.tvSectionTitle.setText(getString(titleFor(section)));
        binding.ivSectionBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.flSectionContainer, fragmentFor(section))
                    .commit();
        }
    }

    private Fragment fragmentFor(String section) {
        switch (section) {
            case SECTION_CONTINUE:
                return new ContinueFragment();
            case SECTION_UPLOADS:
                return new ManageBooksFragment();
            case SECTION_SUBSCRIPTION:
                return new DashBoardFragment();
            case SECTION_RENT:
                return new RentBookFragment();
            default:
                return new MyResultsFragment();
        }
    }

    private int titleFor(String section) {
        switch (section) {
            case SECTION_CONTINUE:
                return R.string.tab_continue;
            case SECTION_UPLOADS:
                return R.string.tab_manage_books;
            case SECTION_SUBSCRIPTION:
                return R.string.tab_subs;
            case SECTION_RENT:
                return R.string.tab_rent;
            default:
                return R.string.tab_my_results;
        }
    }
}

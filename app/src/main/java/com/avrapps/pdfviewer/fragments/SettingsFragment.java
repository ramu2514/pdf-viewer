package com.avrapps.pdfviewer.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.constants.AppConstants;
import com.avrapps.pdfviewer.utils.BillinUtils;
import com.avrapps.pdfviewer.utils.MessagingUtility;
import com.avrapps.pdfviewer.utils.PermissionUtils;
import com.avrapps.pdfviewer.utils.PreferenceUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.avrapps.pdfviewer.constants.AppConstants.APP_THEME_IDS;
import static com.avrapps.pdfviewer.constants.AppConstants.AUTO_IMPORT_FILES;
import static com.avrapps.pdfviewer.constants.AppConstants.DARK_THEME;
import static com.avrapps.pdfviewer.constants.AppConstants.KEEP_SCREEN_ON;
import static com.avrapps.pdfviewer.constants.AppConstants.PAGE_THEME_IDS;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsActivity";
    SharedPreferences sp;
    MainActivity activity;
    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.settings_activity, container, false);
        activity = (MainActivity) getActivity();
        this.view = view;
        applySettings();
        return view;
    }

    private void setTitle() {
        try {
            Toolbar toolbar = activity.findViewById(R.id.my_toolbar);
            activity.setSupportActionBar(toolbar);
            toolbar.setTitle(R.string.tab_head_settings);
        } catch (Exception ignored) {
        }
    }

    private void applySettings() {
        setTitle();
        sp = PreferenceManager.getDefaultSharedPreferences(activity);
        //Page scroll direction settings
        boolean isScrollViewEnabled = sp.getBoolean(AppConstants.READER_HORIZONTAL_SWIPE, false);
        Log.d(TAG, "isScrollViewEnabled : " + isScrollViewEnabled);
        RadioButton radio0 = view.findViewById(R.id.radio0);
        RadioButton radio1 = view.findViewById(R.id.radio1);
        if (isScrollViewEnabled) {
            radio0.setChecked(true);
        } else {
            radio1.setChecked(true);
        }
        radio0.setOnClickListener(this::setSliderPreference);
        radio1.setOnClickListener(this::setSliderPreference);

        //App Theme settings
        int appThemePreference = sp.getInt(AppConstants.APP_THEME, 0);
        for (int theme : APP_THEME_IDS) {
            ((ImageView) view.findViewById(theme)).setImageDrawable(null);
            view.findViewById(theme).setOnClickListener(this::setAppTheme);
        }
        ((ImageView) view.findViewById(APP_THEME_IDS.get(appThemePreference))).setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_done));

        //Page Theme settings
        int notebookBackgroundPreference = sp.getInt(AppConstants.PAGE_THEME, 0);
        for (int pageTheme : PAGE_THEME_IDS) {
            view.findViewById(pageTheme).setBackground(null);
            view.findViewById(pageTheme).setOnClickListener(this::setPageTheme);
        }
        view.findViewById(PAGE_THEME_IDS.get(notebookBackgroundPreference)).setBackgroundColor(ContextCompat.getColor(activity, R.color.grey));
        EditText author = view.findViewById(R.id.author);
        author.setOnClickListener(v -> {
            if (BillinUtils.isApplicationBought(activity)) {
                String aname = sp.getString("prefAuthor", getString(R.string.app_name));
                author.setText(aname);
                Context context = getContext();
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle(R.string.pref_ann_author_title);
                alert.setMessage(R.string.pref_ann_author_body);
                EditText input = new EditText(context);
                input.setText(aname);
                alert.setView(input);
                alert.setPositiveButton(R.string.pref_ann_author_positive_button, (dialog, whichButton) -> {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("prefAuthor", input.getText().toString());
                    editor.apply();
                    author.setText(input.getText().toString());
                });
                alert.show();
            } else {
                MessagingUtility.showBuyApplicationDailog(activity);
            }
        });
        SwitchCompat darkTheme = view.findViewById(R.id.prefDarkSwitch);
        darkTheme.setChecked(sp.getBoolean(DARK_THEME, false));
        darkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (BillinUtils.isApplicationBought(activity)) {
                PreferenceUtil.writeBoolean(sp, DARK_THEME, isChecked);
                activity.recreate();
            } else {
                MessagingUtility.showBuyApplicationDailog(activity);
            }
        });

        SwitchCompat prefKeepScreenOn = view.findViewById(R.id.prefKeepScreenOn);
        prefKeepScreenOn.setChecked(sp.getBoolean(KEEP_SCREEN_ON, false));
        prefKeepScreenOn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.writeBoolean(sp, KEEP_SCREEN_ON, isChecked);
            activity.recreate();
        });

        SwitchCompat doNotImportFile = view.findViewById(R.id.prefDoNotAutoImport);
        doNotImportFile.setChecked(sp.getBoolean(AUTO_IMPORT_FILES, false));
        doNotImportFile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.writeBoolean(sp, AUTO_IMPORT_FILES, isChecked);
            activity.recreate();
        });

        /*ContentResolver resolver1 = activity.getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (UriPermission permission : resolver1.getPersistedUriPermissions()) {
                        activity.revokeUriPermission(activity.getPackageName(), permission.getUri(), 0);
            }
        }*/

        Set<String> removableStorage = PreferenceUtil.getStringSet(activity, AppConstants.REMOVABLE_STORAGES);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                removableStorage.size() != 0) {
            view.findViewById(R.id.sdAccessDiv).setVisibility(View.VISIBLE);
            ContentResolver resolver = activity.getContentResolver();
            List<UriPermission> list = resolver.getPersistedUriPermissions();
            boolean breakOuter = false;
            for (UriPermission permission : list) {
                for (String extStorage : removableStorage) {
                    String sName = new File(extStorage).getName();
                    if (!permission.getUri().toString().contains(sName)) {
                        breakOuter = true;
                        break;
                    }
                }
                if (breakOuter) break;
            }
            view.findViewById(R.id.sdAccessDiv).setVisibility((list.isEmpty() || breakOuter) ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.grantAccessButton).setOnClickListener(v -> MessagingUtility.showSdcardAccessDialog(activity, getString(R.string.pref_saf_access_title),
                    getString(R.string.pref_saf_access_details),
                    getString(R.string.grant_permission), (String returnValue) -> {
                        PermissionUtils.takeCardUriPermission(activity);
                        //view.findViewById(R.id.sdAccessDiv).setVisibility(View.GONE);
                    }, true));

        }

    }

    private void setPageTheme(View view) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(AppConstants.PAGE_THEME, PAGE_THEME_IDS.indexOf(view.getId()));
        editor.apply();
        applySettings();
    }

    private void setAppTheme(View v) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(AppConstants.APP_THEME, APP_THEME_IDS.indexOf(v.getId()));
        editor.apply();
        activity.recreate();
    }

    private void setSliderPreference(View v) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(AppConstants.READER_HORIZONTAL_SWIPE, R.id.radio0 == v.getId());
        editor.apply();
    }
}

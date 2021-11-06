package com.avrapps.pdfviewer.settings_fragment;

import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.APP_THEME_IDS;
import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.AUTO_IMPORT_FILES;
import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.DARK_THEME;
import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.KEEP_SCREEN_ON;
import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.PAGE_THEME_IDS;
import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.VOLUME_BUTTON_SCROLL;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.SUPPORTED_LANGUAGES;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.settings_fragment.constants.AppConstants;
import com.avrapps.pdfviewer.utils.BillinUtils;
import com.avrapps.pdfviewer.utils.MessagingUtility;
import com.avrapps.pdfviewer.utils.PathUtils;
import com.avrapps.pdfviewer.utils.PermissionUtils;
import com.avrapps.pdfviewer.utils.PreferenceUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

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

        LinearLayout backup_settings = view.findViewById(R.id.backup_settings);
        backup_settings.setOnClickListener(v -> startActivity(new Intent(activity, BackupSettings.class)));

        SwitchCompat prefKeepScreenOn = view.findViewById(R.id.prefKeepScreenOn);
        prefKeepScreenOn.setChecked(sp.getBoolean(KEEP_SCREEN_ON, false));
        prefKeepScreenOn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.writeBoolean(sp, KEEP_SCREEN_ON, isChecked);
        });

        SwitchCompat prefVolumeButtonScroll = view.findViewById(R.id.prefVolumeButtonScroll);
        prefVolumeButtonScroll.setChecked(sp.getBoolean(VOLUME_BUTTON_SCROLL, true));
        prefVolumeButtonScroll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.writeBoolean(sp, VOLUME_BUTTON_SCROLL, isChecked);
        });

        TextView prefLanguage = view.findViewById(R.id.prefLanguageValue);
        prefLanguage.setOnClickListener(v-> showRadioButtonDialog());
        prefLanguage.setText( getResources().getConfiguration().locale.getDisplayLanguage());

        SwitchCompat doNotImportFile = view.findViewById(R.id.prefDoNotAutoImport);
        doNotImportFile.setChecked(sp.getBoolean(AUTO_IMPORT_FILES, false));
        doNotImportFile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.writeBoolean(sp, AUTO_IMPORT_FILES, isChecked);
        });

        view.findViewById(R.id.clearAllImports).setOnClickListener(v ->{
            MessagingUtility.showPositiveMessageDialog(activity, getString(R.string.delete_all), getString(R.string.delete_help), getString(R.string.clear), returnValue -> {
                File folder = new File(Environment.getExternalStorageDirectory(), "PDFViewerLite/imports/");
                if(PathUtils.deleteDirectory(folder)){
                    Log.e("Test","Error deleting folder "+ folder);
                }
            }, false);
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

    private void showRadioButtonDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle(R.string.choose_language);
        String selectedLocale = sp.getString("prefLocale", "device");
        CharSequence[] temp = new CharSequence[SUPPORTED_LANGUAGES.size()];
        int counter = 0, checked = 0;
        for (String language : SUPPORTED_LANGUAGES.keySet()) {
            temp[counter] = language;
            if (SUPPORTED_LANGUAGES.get(language).equals(selectedLocale))
                checked = counter;
            counter++;
        }
        dialog.setSingleChoiceItems(temp, checked, (dialog1, which) -> {

            String selected = temp[which].toString();
            Log.e("Selected RadioButton->", selected);
            setLanguagePreference(selected,null);
            /*
            //FakeSplitInstallManager splitInstallManager = FakeSplitInstallManagerFactory.create(activity);
            SplitInstallManager splitInstallManager = SplitInstallManagerFactory.create(activity);
            SplitInstallStateUpdatedListener listener = state -> {
                switch (state.status()) {
                    case SplitInstallSessionStatus.DOWNLOADING:
                        Toast.makeText(activity, "Downloading the additional language ", Toast.LENGTH_LONG).show();
                        break;
                    case SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION:
                        try {
                            splitInstallManager.startConfirmationDialogForResult(state, activity, 1452);
                        } catch (IntentSender.SendIntentException e) {
                            Toast.makeText(activity, "Failed to show user consent ", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                        break;
                    case SplitInstallSessionStatus.INSTALLED:
                        setLanguagePreference(selected, dialog1);
                        return;

                    case SplitInstallSessionStatus.INSTALLING:
                        Toast.makeText(activity, "Installing resources", Toast.LENGTH_LONG).show();
                        break;
                    case SplitInstallSessionStatus.FAILED:
                        Toast.makeText(activity, "Failed to install language. Error code " + state.errorCode(), Toast.LENGTH_LONG).show();

                }
            };

            //device default option selected.
            if ("device".equals(SUPPORTED_LANGUAGES.get(selected))) {
                setLanguagePreference(selected, dialog1);
                return;
            }
            //already downloaded language
            Set<String> installedLanguages = splitInstallManager.getInstalledLanguages();
            for (String language : installedLanguages) {
                if (language.contains(SUPPORTED_LANGUAGES.get(selected).split("_")[0])) {
                    Log.d("Settings", "Language already downloaded");
                    setLanguagePreference(selected, dialog1);
                    return;
                }
            }
            //new languge download request
            splitInstallManager.registerListener(listener);
            String[] localePref = SUPPORTED_LANGUAGES.get(selected).split("_");
            SplitInstallRequest request = SplitInstallRequest.newBuilder()
                    .addLanguage(new Locale(localePref[0], localePref[1], ""))
                    .build();
            splitInstallManager.startInstall(request);*/
        });
        dialog.show();
    }

    private void setLanguagePreference(String selected, DialogInterface dialog) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("prefLanguageName", selected);
        editor.putString("prefLocale", SUPPORTED_LANGUAGES.get(selected));
        editor.apply();
        editor.commit();
        if(dialog!=null)
            dialog.dismiss();
        activity.recreate();
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

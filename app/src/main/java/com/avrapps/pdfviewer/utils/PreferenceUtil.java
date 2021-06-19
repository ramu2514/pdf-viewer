package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.settings_fragment.constants.AppConstants;

import java.util.HashSet;
import java.util.Set;

import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.APP_THEME_IDS;

public class PreferenceUtil {
    public static void writeBoolean(SharedPreferences sp, String prefKey, boolean prefValue) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(prefKey, prefValue);
        editor.apply();
        editor.commit();
    }

    public static void writeStringSet(Activity context, String prefKey, Set<String> prefValue) {
        Log.e("Pref:", "removable storages -" + prefValue);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putStringSet(prefKey, prefValue);
        editor.apply();
        editor.commit();

    }

    public static boolean preferenceStringSetContains(Activity context, String prefKey, String value) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        for (String var : pref.getStringSet(prefKey, new HashSet<>())) {
            return value.contains(var);
        }
        return false;
    }

    public static Set<String> getStringSet(MainActivity activity, String prefKey) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
        return pref.getStringSet(prefKey, new HashSet<>());
    }

    public static int getPrimaryColor(MainActivity activity) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        int appTheme = sp.getInt(AppConstants.APP_THEME, 0);
        int mTheme = APP_THEME_IDS.get(appTheme);
        int color = R.color.black;
        switch (mTheme) {
            case R.id.app_theme_yellow:
                color = R.color.colorPrimaryDarkYellow;
                break;
            case R.id.app_theme_blue:
                color =  R.color.colorPrimaryDarkBlue;
                break;
            case R.id.app_theme_red:
                color = R.color.colorPrimaryDarkRed;
                break;
            case R.id.app_theme_green:
                color =  R.color.colorPrimaryDarkGreen;
                break;
        }
        return color;
    }
}

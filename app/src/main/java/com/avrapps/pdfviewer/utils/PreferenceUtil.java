package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.avrapps.pdfviewer.MainActivity;

import java.util.HashSet;
import java.util.Set;

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
}

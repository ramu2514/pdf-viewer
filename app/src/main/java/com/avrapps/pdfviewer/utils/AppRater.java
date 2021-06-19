package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.avrapps.pdfviewer.R;

public class AppRater {

    private static final long INTERVAL = 5;
    private static final long BUYPRO_INTERVAL = 100;
    private static String APP_TITLE;
    private static String APP_NAME;

    public static void app_launched(Activity mContext) {
        APP_TITLE = mContext.getResources().getString(R.string.app_name);
        APP_NAME = mContext.getPackageName();
        //Min number of days
        int DAYS_UNTIL_PROMPT = 1;
        SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
        SharedPreferences.Editor editor = prefs.edit();
        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launch_count);
        editor.apply();

        Log.e("TAG", "app_launched: " + launch_count % BUYPRO_INTERVAL);
        if (launch_count % BUYPRO_INTERVAL == 0) {
            if (!BillinUtils.isApplicationBought(mContext)) {
                showBuyProDialog(mContext);
            }
        }
        if (prefs.getBoolean("dontshowagain", false)) {
            return;
        }

        // Get date of first launch
        long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }

        // Wait at least n days before opening
        long launch_until_prompt = prefs.getLong("launch_until_promt", 4);
        if (launch_count >= launch_until_prompt) {
            if (System.currentTimeMillis() >= date_firstLaunch +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(mContext, prefs);
            }
        }

        editor.apply();
    }

    private static void showBuyProDialog(Activity context) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_buypro);

        Button btnNotNow = dialog.findViewById(R.id.btn_not_now);
        Button btnBuy = dialog.findViewById(R.id.btn_buy);

        btnBuy.setOnClickListener(v -> {new BillinUtils(context).buyApplication();dialog.dismiss();});
        btnNotNow.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private static void showRateDialog(final Context mContext, final SharedPreferences preferences) {
        String rate = "Rate " + APP_TITLE;
        SharedPreferences.Editor editor = preferences.edit();
        new AlertDialog.Builder(mContext)
                .setTitle(APP_TITLE)
                .setMessage(mContext.getString(R.string.rating_text))
                .setPositiveButton(rate, (dialogInterface, i) -> {
                    try {
                        mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_NAME)));
                    } catch (Exception ex) {
                        viewInBrowser(mContext, "https://play.google.com/store/apps/details?id=" + mContext.getPackageName());
                    }
                })
                .setNeutralButton(R.string.remind_later, (dialogInterface, i) -> {
                    long launch_until_prompt = preferences.getLong("launch_until_promt", INTERVAL);
                    editor.putLong("launch_until_promt", launch_until_prompt + INTERVAL);
                    editor.apply();
                    editor.commit();
                })
                .setNegativeButton(R.string.dialog_button_no, (dialogInterface, i) -> {
                    if (editor != null) {
                        editor.putBoolean("dontshowagain", true);
                        editor.commit();
                        editor.apply();
                    }
                })
                .show();
    }

    private static void viewInBrowser(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (null != intent.resolveActivity(context.getPackageManager())) {
            context.startActivity(intent);
        }
    }
}


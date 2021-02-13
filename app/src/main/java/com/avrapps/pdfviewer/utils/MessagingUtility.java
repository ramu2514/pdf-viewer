package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.avrapps.pdfviewer.CallbackInterface;
import com.avrapps.pdfviewer.R;

public class MessagingUtility {

    public static void alertView(Activity c, boolean shouldQuit) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(c);
        dialog.setTitle(c.getString(R.string.dialog_title_permision_denied));
        dialog.setCancelable(false);
        if (shouldQuit) {
            dialog.setMessage(R.string.error_grant_access_through_settings);
            dialog.setNegativeButton(c.getString(android.R.string.ok), (dialoginterface, i) -> {
                dialoginterface.dismiss();
                c.finish();
            });
        } else {
            dialog.setMessage(c.getString(R.string.dialog_body_permission_denied));
            dialog.setNegativeButton(R.string.quit, (dialoginterface, i) -> {
                dialoginterface.dismiss();
                c.finish();
            });
            dialog.setPositiveButton(c.getString(R.string.dialog_button_retry), (dialoginterface, i) -> {
                dialoginterface.dismiss();
                new PermissionUtils().checkRunTimePermission(c);
            });
        }
        dialog.show();
    }

    public static void showWebviewDialog(Context mContext, String url) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        WebView wv = new WebView(mContext);
        wv.loadUrl(url);
        wv.getSettings().setJavaScriptEnabled(true);
        alert.setCancelable(false);
        alert.setView(wv);
        alert.setPositiveButton(R.string.close, (dialog, i) -> dialog.dismiss());
        alert.show();
    }

    public static void showDialogWithPositiveOption(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.dismiss, null)
                .show();
    }

    public static boolean showSdcardAccessDialog(Activity activity, String title, String message,
                                                 String okMessage, CallbackInterface callback, boolean forceDisplay) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        if (!forceDisplay && settings.getBoolean("preference_sdcard_do_not_ask_again", false)) {
            return false;
        }
        ImageView view = new ImageView(activity);
        view.setImageResource(R.drawable.select_sd);
        view.setAdjustViewBounds(true);
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setView(view)
                .setPositiveButton(okMessage, (d, which) -> callback.onButtonClick(""));
        if (!forceDisplay) {
            dialog.setNeutralButton(R.string.dont_Show_again, (dialog1, which) -> {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.waring)
                        .setMessage(R.string.dont_show_again_description)
                        .setPositiveButton(R.string.dismiss, null)
                        .show();
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("preference_sdcard_do_not_ask_again", true);
                editor.apply();
            });
        }
        dialog.setNegativeButton(R.string.dismiss, (dialog12, which)
                -> Toast.makeText(activity, R.string.open_readonly, Toast.LENGTH_LONG).show());

        dialog.show();
        return true;
    }

    public static void showBuyApplicationDailog(Activity activity) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        WebView wv = new WebView(activity);
        WebSettings settings = wv.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        wv.loadUrl("file:///android_asset/features.html");
        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        wv.setLayoutParams(lp);

        alert.setView(wv);
        alert.setNegativeButton(R.string.close, (dialog, id) -> dialog.dismiss());
        alert.setPositiveButton(R.string.buy, (dialog, id) -> new BillinUtils(activity).buyApplication());
        alert.show();

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void askSafPermission(Activity activity) {

        MessagingUtility.showSdcardAccessDialog(activity, String.valueOf(R.string.pdf_not_editable),
                activity.getString(R.string.want_to_grant_Access),
                activity.getString(R.string.grant_permission), (String returnValue) -> PermissionUtils.takeCardUriPermission(activity), false);
    }

    public static void showCheckBoxDialog(Activity activity, String text, CallbackInterface callback) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        final EditText edittext = new EditText(activity);
        edittext.setText(text);
        edittext.setFocusable(true);
        alert.setTitle(R.string.enter_field_value);
        alert.setView(edittext);
        alert.setPositiveButton(R.string.update_text, (dialog, whichButton) -> {
            String textBoxValue = edittext.getText().toString();
            dialog.dismiss();
            callback.onButtonClick(textBoxValue);
        });
        alert.setNegativeButton(R.string.dismiss, (dialog, which) -> dialog.dismiss());
        alert.setCancelable(false);
        alert.show();
    }

    public static void showPositiveMessageDialog(Activity activity, String title, String body, String buttonMessage, CallbackInterface callback) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle(title);
        alert.setMessage(body);
        alert.setPositiveButton(buttonMessage, (dialog, whichButton) -> {
            dialog.dismiss();
            callback.onButtonClick("");
        });
        alert.setNegativeButton(R.string.dismiss, (dialog, which) -> dialog.dismiss());
        alert.setCancelable(false);
        alert.show();
    }
}
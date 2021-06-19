package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.avrapps.pdfviewer.R;

import java.io.File;

public class SharingUtils {
    public static void sharePdf(Activity activity, String path) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        try {
            String authority = activity.getPackageName() + ".fileprovider";
            Uri contentUri = FileProvider.getUriForFile(activity, authority, new File(path));

            if (contentUri != null) {

                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                intentShareFile.setType("application/pdf");
                intentShareFile.putExtra(Intent.EXTRA_STREAM, contentUri);
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share_subject));
                intentShareFile.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_message));
                activity.startActivity(Intent.createChooser(intentShareFile, "Share File"));
            }
        } catch (Exception ex) {
            Toast.makeText(activity, R.string.could_not_share, Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    public static void openDeveloperPage(Context c) {
        Intent intentDev = new Intent(Intent.ACTION_VIEW);
        intentDev.setData(Uri.parse("market://dev?id=AVR-Apps"));
        if (myStartActivity(c, intentDev)) {
            intentDev.setData(Uri.parse("https://play.google.com/store/apps/developer?id=AVR-Apps"));
            if (myStartActivity(c, intentDev)) {
                Toast.makeText(c, R.string.error_no_playstore, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static boolean myStartActivity(Context c, Intent aIntent) {
        try {
            c.startActivity(aIntent);
            return false;
        } catch (ActivityNotFoundException e) {
            return true;
        }
    }


    public static void sendBug(Context c) {
        openUrl(c,"https://forms.gle/QhAd7xD322ncXE3x5",R.string.error_no_browser);
    }
    public static void openUrl(Context c, String url, int onFailTextString) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (myStartActivity(c, browserIntent)) {
            Toast.makeText(c, onFailTextString, Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareApp(Context c) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                c.getString(R.string.check_out) +"https://play.google.com/store/apps/details?id=" + c.getPackageName());
        sendIntent.setType("text/plain");
        if (myStartActivity(c, sendIntent)) {
            Toast.makeText(c, R.string.error_no_apptoshare, Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareText(Context c, String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        if (myStartActivity(c, sendIntent)) {
            Toast.makeText(c, R.string.error_no_apptoshare, Toast.LENGTH_SHORT).show();
        }
    }

    public static void rateApp(Context c, String packageName) {
        Uri uri = Uri.parse("market://details?id=" + packageName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }
        try {
            c.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            c.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + c.getPackageName())));
        }
    }
}

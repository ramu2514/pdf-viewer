package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;

import com.avrapps.pdfviewer.BuildConfig;
import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.constants.AppConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import static com.avrapps.pdfviewer.constants.AppConstants.APP_THEME_IDS;
import static com.avrapps.pdfviewer.utils.PathUtils.TAG;

public class MiscUtils {
    public static void copyTextToClipBoard(Context context, String textCopied) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Text Copied", textCopied);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Text Copied", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setTheme(Activity activity) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        if (sp.getBoolean(AppConstants.KEEP_SCREEN_ON, false)) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (sp.getBoolean(AppConstants.DARK_THEME, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        int appTheme = sp.getInt(AppConstants.APP_THEME, 0);
        int mTheme = APP_THEME_IDS.get(appTheme);
        switch (mTheme) {
            case R.id.app_theme_black:
                activity.setTheme(R.style.AppThemeDark);
                break;
            case R.id.app_theme_yellow:
                activity.setTheme(R.style.AppThemeYellow);
                break;
            case R.id.app_theme_blue:
                activity.setTheme(R.style.AppThemeBlue);
                break;
            case R.id.app_theme_red:
                activity.setTheme(R.style.AppThemeRed);
                break;
            default:
                activity.setTheme(R.style.AppThemeGreen);
                break;

        }
    }

    //https://stackoverflow.com/questions/53670406/get-file-from-google-drive-using-intent
    public static void openDoc(Uri data, MainActivity activity, ArrayList<String> selectedFiles) {
        String log = "";
        File path = null;
        try {
            path = PathUtils.getPathNew(activity, data);
            Log.e(TAG, "Path:" + path.getAbsolutePath());
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            log = sw.toString();
        }
        if (path != null && path.exists()) {
            Log.e(TAG, "Path:" + path.getAbsolutePath());
            activity.openDocumentFragment(path.getAbsolutePath(), selectedFiles);
        } else {
            String appMetatdata = "PDF Viewer Lite: versionCode" + BuildConfig.VERSION_CODE;
            String uri = appMetatdata + "\n\nUri -Authority: " + data.getAuthority() +
                    ", Fragment: " + data.getFragment() +
                    ", Port: " + data.getPort() +
                    ", Query: " + data.getQuery() +
                    ", Scheme: " + data.getScheme() +
                    ", Host: " + data.getHost() +
                    ", Segments: " + data.getPathSegments().toString();

            Toast.makeText(activity, R.string.cant_load_file, Toast.LENGTH_LONG).show();
            MiscUtils.sendBugEmail(activity, log + uri);
        }
    }

    public static void sendBugEmail(Context activity, Exception ex1, Exception e) {
        e.printStackTrace();
        ex1.printStackTrace();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        ex1.printStackTrace(pw);
        sendBugEmail(activity, sw.toString());
    }

    public static void sendBugEmail(Context activity, String s) {
        StringWriter sw = new StringWriter();
        sw.append(s);
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        sw.append("\n\nDevice:").append(manufacturer).append(model);
        String stacktrace = sw.toString();
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"avrapps.reports@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "PDF Viewer Lite - App Error Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, stacktrace);

        activity.startActivity(Intent.createChooser(emailIntent, "Send error report..."));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void copyFileUsingStorageFramework(Activity activity, DocumentFile pickedDir, String inputPath, String outputpath) throws IOException {
        DocumentFile file = pickedDir.findFile(new File(outputpath).getName());
        if (file != null && file.exists()) {
            file.delete();
        }
        DocumentFile newFile = pickedDir.createFile("application/pdf", new File(outputpath).getName());
        Log.e("Main", "Doc Id : " + newFile.getUri());
        OutputStream out = activity.getContentResolver().openOutputStream(newFile.getUri());
        InputStream in = new FileInputStream(inputPath);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.flush();
        out.close();
    }
}

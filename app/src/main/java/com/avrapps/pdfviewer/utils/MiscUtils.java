package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.avrapps.pdfviewer.BuildConfig;
import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.results_fragment.FirebaseUtils;
import com.avrapps.pdfviewer.settings_fragment.constants.AppConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import static android.provider.MediaStore.VOLUME_EXTERNAL;
import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.APP_THEME_IDS;
import static com.avrapps.pdfviewer.utils.PathUtils.TAG;

public class MiscUtils {
    public static void copyTextToClipBoard(Context context, String textCopied) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(context.getString(R.string.text_copied), textCopied);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show();
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
        StringBuilder logs = new StringBuilder();
        try {
            path = PathUtils.getPathNew(activity, data, logs);
            Log.e(TAG, "Path:" + path.getAbsolutePath());
        } catch (Exception ex) {
            log = logs.toString();
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
            FirebaseUtils.analyticsSimpleCount(activity, "FAIL_OPEN_PDF_"+data.getScheme() );
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
        sw.append("\n\nDevice:").append(manufacturer).append("  ").append(model);
        sw.append("\nAndroid Version:").append(String.valueOf(Build.VERSION.SDK_INT));
        String stacktrace = sw.toString();
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"avrapps.reports@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.email_bug_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT, stacktrace);

        activity.startActivity(Intent.createChooser(emailIntent, activity.getString(R.string.send_Error)));
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

    public static void shareFile(Activity activity, File file) {
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        intentShareFile.setType("application/pdf");
        Uri pdfUri = FileProvider.getUriForFile(activity,
                activity.getApplicationContext().getPackageName() + ".fileprovider", file);
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intentShareFile.putExtra(Intent.EXTRA_STREAM, pdfUri);
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, R.string.share_pdf);
        intentShareFile.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.pdf_generated) +
                " https://play.google.com/store/apps/details?id=" + activity.getApplicationContext().getPackageName());
        activity.startActivity(Intent.createChooser(intentShareFile, activity.getString(R.string.share_pdf)));
    }

    public static void shareMultipleFiles(Activity activity, File destination, String mimeType) {
        if (destination.listFiles() == null) {
            Log.e("MiscUtils", "Empty destination Folder: " + destination.getAbsolutePath());
            return;
        }
        Intent intentShareFile = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intentShareFile.setType(mimeType);
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : destination.listFiles()) {
            Uri imageUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            uris.add(imageUri);
        }
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uris);
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, R.string.share_files);
        intentShareFile.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.pdf_generated) +
                " https://play.google.com/store/apps/details?id=" + activity.getApplicationContext().getPackageName());
        activity.startActivity(Intent.createChooser(intentShareFile, activity.getString(R.string.share_pdf)));
    }

    public static void openFile(MainActivity activity, File f) {
        Uri uri = Uri.fromFile(f);
        openDoc(uri, activity, new ArrayList<>());
        FirebaseUtils.analyticsFileOpen(activity,"FILE_OPEN_TOOL_RESULT",uri);
    }

    public static String downloadFile(Activity activity, @NonNull File file) throws IOException {
        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = activity.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
            Uri imageUri = resolver.insert(MediaStore.Files.getContentUri(VOLUME_EXTERNAL), contentValues);
            fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
            copyFileUsingStream(file, fos);
            return "Downloads Directoy";
        } else {
            String dest = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                dest = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
            } else {
                dest = Environment.getExternalStorageDirectory().getAbsolutePath();
            }
            copyFile(file, new File(dest + "/" + file.getName()));
            return dest + "/" + file.getName();
        }
    }

    private static void copyFileUsingStream(File source, OutputStream os) throws IOException {
        try (InputStream is = new FileInputStream(source)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private static void copyFile(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    public static void setLocale(MainActivity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String localePref = preferences.getString("prefLocale", Locale.getDefault().toString());
        Locale newLocale = new Locale("en", "US");
        try {
            if ("device".equals(localePref)) {
                newLocale = Locale.getDefault();
            } else {
                newLocale = new Locale(localePref.split("_")[0], localePref.split("_")[1], "");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Resources activityRes = activity.getResources();
        Configuration activityConf = activityRes.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activityConf.setLocale(newLocale);
        } else {
            activityConf.locale = newLocale;
        }
        activityRes.updateConfiguration(activityConf, activityRes.getDisplayMetrics());

        Resources applicationRes = activity.getApplicationContext().getResources();
        Configuration applicationConf = applicationRes.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activityConf.setLocale(newLocale);
        } else {
            activityConf.locale = newLocale;
        }
        applicationRes.updateConfiguration(applicationConf,
                applicationRes.getDisplayMetrics());
    }

    public static void viewInBrowser(Activity context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (null != intent.resolveActivity(context.getPackageManager())) {
            context.startActivity(intent);
        }
    }

    public static void viewMultipleFiles(MainActivity activity, File destination) {
        if (destination.listFiles() == null) {
            Log.e("MiscUtils", "Empty destination Folder: " + destination.getAbsolutePath());
            return;
        }
        ArrayList<String> files = new ArrayList<>();
        for (File file : destination.listFiles()) {
            files.add(file.getAbsolutePath());
        }
        activity.openDocumentFragment(files.get(0), files);
    }

    public static String getManifestDataFromActivity(MainActivity activity, String manifestKey ){
        try {
            ActivityInfo ai = activity.getPackageManager().getActivityInfo(
                    activity.getComponentName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            return bundle.getString(manifestKey);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}

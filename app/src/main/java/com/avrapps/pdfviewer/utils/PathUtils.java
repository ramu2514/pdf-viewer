package com.avrapps.pdfviewer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import static com.avrapps.pdfviewer.settings_fragment.constants.AppConstants.AUTO_IMPORT_FILES;


public class PathUtils {
    static final String TAG = "PathUtil";

    private PathUtils() {
    } //private constructor to enforce Singleton pattern


    public static File getPathNew(final Context context, final Uri uri, StringBuilder logs) {
        // File
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String filePath = uri.getPath();
            Log.d(TAG, "PathUtil1 From Path:" + filePath);
            if (filePath != null && new File(filePath).exists()) return new File(filePath);
            logs.append("PathUtil1 File does not exist:").append(filePath);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File path = saveFileFromUri(context, uri, logs);
            Log.d(TAG, "PathUtil2 Uri from PostKitkat:" + path);
            if (path != null && path.exists()) return path;
            logs.append("PathUtil2 File does not exist:").append(path);
        }
        String preKitkatFilePath = getPathForNonKitkat(context, uri);
        Log.d(TAG, "PathUtil3 Uri from PreKitkat:" + preKitkatFilePath);
        if (preKitkatFilePath != null && new File(preKitkatFilePath).exists())
            return new File(preKitkatFilePath);
        logs.append("PathUtil3 File does not exist:").append(preKitkatFilePath);
        throw new RuntimeException("Can't get filepath");
    }

    private static String getPathForNonKitkat(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        return null;
    }

    private static File saveFileFromUri(Context context, Uri uri, StringBuilder logs) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean noAutoImport = sp.getBoolean(AUTO_IMPORT_FILES, false);
        File folder = new File(Environment.getExternalStorageDirectory(), "PDFViewerLite/imports/");
        folder.mkdirs();
        if (!folder.exists() || noAutoImport) {
            folder = context.getFilesDir();
        }
        String getFileName = new Date().getTime() + ".pdf";
        try {
            getFileName = getFileName(uri, context);
        } catch (Exception ex) {
            logs.append("Error getting Filename.\n").append(getExceptionString(ex));
        }
        File destinationPath = new File(folder, getFileName);
        if (destinationPath.exists()) {
            destinationPath = new File(folder, new Date().getTime() + "_" + getFileName);
        }
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            is.read(buf);
            do {
                bos.write(buf);
            } while (is.read(buf) != -1);
            return destinationPath;
        } catch (IOException e) {
            e.printStackTrace();
            logs.append(getExceptionString(e));
        } finally {
            try {
                if (is != null) is.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
                logs.append(getExceptionString(e));
            }
        }
        return null;
    }

    private static String getExceptionString(Exception e) {
        StringWriter sw = new StringWriter();
        sw.append("\n\n");
        e.printStackTrace(new PrintWriter(sw));
        sw.append("\n");
        return sw.toString();
    }

    /**
     * Get the value of the data column for this Uri( which is typically a file path). This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static String getFileName(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return false;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    boolean wasSuccessful = file.delete();
                    if (wasSuccessful) {
                        Log.i("Deleted", "successfully");
                    }
                }
            }
        }
        return (path.delete());
    }

    public static String getExtension(Uri uri, Context context) {
        try {
            String fileName = getFileName(uri, context);
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                return fileName.substring(i+1).toUpperCase();
            }
        } catch (Exception ignored) {
        }
        return "UNKNOWN";
    }
}
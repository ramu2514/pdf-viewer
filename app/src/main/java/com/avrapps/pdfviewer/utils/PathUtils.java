package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.avrapps.pdfviewer.constants.AppConstants.AUTO_IMPORT_FILES;


public class PathUtils {
    static final String TAG = "PathUtil";

    private PathUtils() {
    } //private constructor to enforce Singleton pattern


    public static File getPathNew(final Context context, final Uri uri) {
        File path = null;
        // File
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String filePath = uri.getPath();
            Log.d(TAG, "PathUtil1 From Path:" + filePath);
            if (filePath != null && new File(filePath).exists()) return new File(filePath);

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            path = saveFileFromUri(context, uri);
            Log.d(TAG, "PathUtil1 Uri from PostKitkat:" + path);
            if (path != null && path.exists()) return path;
        }
        String preKitkatFilePath = getPathForNonKitkat(context, uri);
        Log.d(TAG, "PathUtil1 Uri from PreKitkat:" + path);
        if (preKitkatFilePath != null && new File(preKitkatFilePath).exists())
            return new File(preKitkatFilePath);
        Log.d(TAG, "PathUtil1 No Path:" + path);
        throw new RuntimeException("Can't get filepath");
    }

    private static String getPathForNonKitkat(Context context, Uri uri) {
        // MediaStore (and general)
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        return null;
    }

    public static File getPath(final Context context, final Uri uri) {
        String log = " Uri -Authority: " + uri.getAuthority() +
                ", Fragment: " + uri.getFragment() +
                ", Port: " + uri.getPort() +
                ", Query: " + uri.getQuery() +
                ", Scheme: " + uri.getScheme() +
                ", Host: " + uri.getHost() +
                ", Segments: " + uri.getPathSegments().toString();
        String path = null;
        try {
            path = getPathForDocument(context, uri);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (path != null && new File(path).exists()) {
            return new File(path);
        }
        return saveFileFromUri(context, uri);
    }

    public static String getRealPathFromUri(Activity c, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = c.managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     */
    private static String getPathForDocument(final Context context, final Uri uri) {

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                String id = DocumentsContract.getDocumentId(uri);
                id = id.replaceAll("\\D+", "");

                String[] contentUriPrefixesToTry = new String[]{
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                };

                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    try {
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                        String path = getDataColumn(context, contentUri, null, null);
                        if (path != null && new File(path).exists()) {
                            return path;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static File saveFileFromUri(Context context, Uri uri) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean noAutoImport = sp.getBoolean(AUTO_IMPORT_FILES, false);
        File folder = new File(Environment.getExternalStorageDirectory(), "PDFViewerLite/imports/");
        folder.mkdirs();
        if (!folder.exists() || noAutoImport) {
            folder = context.getFilesDir();
        }
        String getFileName = new Date().getTime() + "";
        try {
            getFileName = getFileName(uri, context);
        } catch (Exception ignored) {
        }
        File destinationPath = new File(folder, getFileName);
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
        } finally {
            try {
                if (is != null) is.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isVirtualFile(Context activity, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (!DocumentsContract.isDocumentUri(activity, uri)) {
                return false;
            }

            Cursor cursor = activity.getContentResolver().query(
                    uri,
                    new String[]{DocumentsContract.Document.COLUMN_FLAGS},
                    null, null, null);
            int flags = 0;
            if (cursor.moveToFirst()) {
                flags = cursor.getInt(0);
            }
            cursor.close();
            return (flags & DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0;
        } else {
            return false;
        }
    }

    private static InputStream getInputStreamForVirtualFile(Context activity, Uri uri, String mimeTypeFilter) throws IOException {

        ContentResolver resolver = activity.getContentResolver();

        String[] openableMimeTypes = resolver.getStreamTypes(uri, mimeTypeFilter);

        if (openableMimeTypes == null ||
                openableMimeTypes.length < 1) {
            throw new FileNotFoundException();
        }

        return resolver.openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
                .createInputStream();
    }    //https://stackoverflow.com/questions/53670406/get-file-from-google-drive-using-intent

    private static boolean isGoogleUri(Uri uri) {
        String uriScheme = uri.getScheme();
        if (uriScheme == null) return false;
        List<String> googleUriList = Arrays.asList("com.google.android.apps.docs.storage",
                "com.google.android.apps.photos.content");
        for (String uriPrefix : googleUriList) {
            return uriScheme.contains(uriPrefix);
        }
        return false;
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
                //if (DEBUG)
                //    DatabaseUtils.dumpCursor(cursor);

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

}
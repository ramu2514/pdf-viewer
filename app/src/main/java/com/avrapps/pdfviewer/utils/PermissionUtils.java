package com.avrapps.pdfviewer.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.settings_fragment.constants.AppConstants;

import java.util.Arrays;

public class PermissionUtils {
    private static final String TAG = "PermissionUtil";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void takeCardUriPermission(Activity context) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        context.startActivityForResult(intent, 4010);
    }

    public boolean checkRunTimePermission(Activity activity) {
        String[] permissionArrays = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            if (Environment.isExternalStorageManager()) {
                //when permission is granted
                ((MainActivity) activity).continueOperations();
            } else {
                //request for the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int res = ActivityCompat.checkSelfPermission(activity, permissionArrays[0]);
            if (res != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, permissionArrays, AppConstants.PERMISSIONS_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    public void validatePermissions(Activity a, int requestCode, String[] permissions, int[] grantResults) {
        Log.e(TAG, requestCode + "\n" + Arrays.toString(permissions) + Arrays.toString(grantResults));
        if (requestCode == AppConstants.PERMISSIONS_REQUEST_CODE && permissions != null) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "allowed" + permission);
                    ((MainActivity) a).continueOperations();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(a, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Log.e(TAG, "denied" + permission);
                        MessagingUtility.alertView(a, false);
                        break;
                    } else {
                        Log.e(TAG, "set to never ask again" + permission + ActivityCompat.checkSelfPermission(a, permission));
                        MessagingUtility.alertView(a, true);
                        break;
                    }
                }
            }
        }
    }
}

package com.avrapps.pdfviewer.results_fragment;

import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_NAME_CODES;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.utils.PathUtils;
import com.google.firebase.analytics.FirebaseAnalytics;

public class FirebaseUtils {

    public static void analyticsToolSuccessAction(Activity activity, int operation, String openedFrom) {
        Bundle bundle = new Bundle();
        bundle.putString("openedFrom", openedFrom);
        String operationName = TOOL_NAME_CODES.get(operation);
        FirebaseAnalytics.getInstance(activity).logEvent("TOOL_SUCCESS_" + operationName, bundle);
    }

    public static void analyticsFileOpen(Activity activity, String operation, Uri uri) {
        Bundle bundle = new Bundle();
        bundle.putString("extension", PathUtils.getExtension(uri,activity));
        FirebaseAnalytics.getInstance(activity).logEvent(operation , bundle);
    }

    public static void analyticsSimpleCount(MainActivity activity, String operation) {
        Bundle bundle = new Bundle();
        FirebaseAnalytics.getInstance(activity).logEvent(operation , bundle);
    }
}

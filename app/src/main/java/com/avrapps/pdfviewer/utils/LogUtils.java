package com.avrapps.pdfviewer.utils;

import android.util.Log;

import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PDFWidget;

import java.util.List;

public class LogUtils {

    private static final String LOG_TAG = "LogUtils";

    public static void logPDFAnnotations(List<PDFAnnotation> annotations) {
        if (annotations == null) {
            Log.e(LOG_TAG, "PDFAnnotations are null");
            return;
        }
        if (annotations.isEmpty()) {
            Log.e(LOG_TAG, "PDFAnnotations are Empty");
            return;
        }
        for (PDFAnnotation annotation : annotations) {
            Log.e(LOG_TAG, annotation.getType() + annotation.getAuthor());
        }
    }

    public static void logPDFWidgets(List<PDFWidget> pdfWidgetList) {
        if (pdfWidgetList == null) {
            Log.e(LOG_TAG, "PDFWidgets are null");
            return;
        }
        if (pdfWidgetList.isEmpty()) {
            Log.e(LOG_TAG, "PDFWidgets are Empty");
            return;
        }
        for (PDFWidget widget : pdfWidgetList) {
            Log.e(LOG_TAG, widget.getFieldType() + ":Flags" + widget.getFieldFlags());
        }
    }
}

package com.avrapps.pdfviewer.viewer_fragment;

import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_COMPRESS;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_DELETE_PAGES;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_PDF_TO_IMAGES;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_PROTECT;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_ROTATE_PAGES;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_SPLIT_PDF;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_UNLOCK_PDF;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.print.PrintHelper;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.SharingUtils;
import com.avrapps.pdfviewer.utils.TTSUtil;

import java.io.File;
import java.util.Map;

class OptionsDialog extends Dialog {

    private final DocumentFragment documentFragment;
    private final MainActivity activity;
    private final MuPDFCore core;

    public OptionsDialog(@NonNull DocumentFragment documentFragment, @NonNull MainActivity activity) {
        super(documentFragment.getContext(), R.style.DialogSlideAnim);
        this.documentFragment = documentFragment;
        this.core = documentFragment.getCore();
        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("LifeCycle", "OnCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
        setDialogView(getWindow(), R.layout.options_layout);

        //features that are not related to PDF & MUPDF Cre
        View navHelp = findViewById(R.id.tv_bm_navigation_help);
        TextView markFavorite = findViewById(R.id.tv_bm_mark_favorite);
        View share = findViewById(R.id.tv_bm_share);
        navHelp.setOnClickListener(v -> {
            dismiss();
            showControls();
        });
        markFavorite.setOnClickListener(v -> {
            dismiss();
            handleFavoriteClick();
        });
        markFavorite.setText(core.isFavorite() ? R.string.unmark_favorite : R.string.mark_favorite);
        share.setOnClickListener(v -> {
            dismiss();
            SharingUtils.sharePdf(documentFragment.getActivity(), documentFragment.path);
        });

        //features not related to pdf, but requires core
        View docDetails = findViewById(R.id.tv_bm_document_details);
        if (core != null) {
            docDetails.setVisibility(View.VISIBLE);
            docDetails.setOnClickListener(v -> {
                dismiss();
                showDocInfo();
            });
        }

        //Features related to PDF with out any limitations
        View deletePages = findViewById(R.id.tv_bm_delete_pages);
        View compressPDF = findViewById(R.id.tv_bm_compress_pdf);
        View splitPDF = findViewById(R.id.tv_split_pdf);
        View pdfToImages = findViewById(R.id.tv_bm_pdf_to_images);
        View rotatePdfPage = findViewById(R.id.rotate_page);
        View speakPage = findViewById(R.id.speak_page);

        boolean corePdfOperationsSupported = core != null && core.isPDF();
        int pdfVisibility = corePdfOperationsSupported ? View.VISIBLE : View.GONE;
        deletePages.setVisibility(pdfVisibility);
        compressPDF.setVisibility(pdfVisibility);
        splitPDF.setVisibility(pdfVisibility);
        pdfToImages.setVisibility(pdfVisibility);
        rotatePdfPage.setVisibility(pdfVisibility);
        speakPage.setVisibility(pdfVisibility);

        if (corePdfOperationsSupported) {
            deletePages.setOnClickListener(v -> {
                dismiss();
                activity.continueOperationsOnFileSelect(documentFragment.path, documentFragment.password, TOOL_DELETE_PAGES, new Bundle());
            });
            compressPDF.setOnClickListener(v -> {
                dismiss();
                activity.continueOperationsOnFileSelect(documentFragment.path, documentFragment.password, TOOL_COMPRESS, new Bundle());
            });
            splitPDF.setOnClickListener(v -> {
                dismiss();
                activity.continueOperationsOnFileSelect(documentFragment.path, documentFragment.password, TOOL_SPLIT_PDF, new Bundle());
            });
            pdfToImages.setOnClickListener(v -> {
                dismiss();
                activity.continueOperationsOnFileSelect(documentFragment.path, documentFragment.password, TOOL_PDF_TO_IMAGES, new Bundle());
            });
            ReaderView mDocView = documentFragment.getDocView();
            rotatePdfPage.setOnClickListener(v -> {
                dismiss();
                int currentPage = mDocView.getDisplayedViewIndex() + 1;
                Bundle b = new Bundle();
                b.putInt("pageNumberToRotate", currentPage);
                activity.continueOperationsOnFileSelect(documentFragment.path, documentFragment.password, TOOL_ROTATE_PAGES, b);
            });
            speakPage.setOnClickListener(v -> {
                dismiss();
                core.gotoPage(mDocView.getDisplayedViewIndex());
                String text = mDocView.getVisibleText();
                TTSUtil ttsUtils = new TTSUtil(activity);
                ttsUtils.showNoteWithSpeaker(text);
            });
        }

        //Features related to PDF with print restrictions
        boolean printSupported = (core.isImage() || (core.isPDF() && !core.needsPassword()))
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        int printPDFVisibility = printSupported ? View.VISIBLE : View.GONE;
        View printPDF = findViewById(R.id.tv_bm_print);
        printPDF.setVisibility(printPDFVisibility);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (core.isImage()) {
                printPDF.setOnClickListener(view -> printImage());
            } else if (core.isPDF() && !core.needsPassword()) {
                printPDF.setOnClickListener(view -> printPdfDocument());
            }
        }

        //Protected PDF features.
        boolean hasPassword = core != null && core.isPDF() && core.needsPassword();
        int removePasswordVisibility = hasPassword ? View.VISIBLE : View.GONE;
        View removePassword = findViewById(R.id.tv_bm_remove_password);
        removePassword.setVisibility(removePasswordVisibility);
        if (hasPassword) {
            removePassword.setOnClickListener(v -> {
                dismiss();
                activity.continueOperationsOnFileSelect(documentFragment.path, documentFragment.password, TOOL_UNLOCK_PDF, new Bundle());
            });
        }

        //Unprotected PDF features
        boolean hasNoPassword = core != null &&
                core.isPDF() &&
                !core.needsPassword();
        int protectPdfVisibility = hasNoPassword ? View.VISIBLE : View.GONE;
        View protectPDF = findViewById(R.id.tv_bm_protect_pdf);
        protectPDF.setVisibility(protectPdfVisibility);
        if (hasNoPassword) {
            protectPDF.setOnClickListener(v -> {
                dismiss();
                activity.continueOperationsOnFileSelect(documentFragment.path, documentFragment.password, TOOL_PROTECT, new Bundle());
            });
        }

        //Local file based features
        boolean isLocalFile = (!documentFragment.path.contains(activity.getFilesDir().getAbsolutePath())
                || !documentFragment.path.contains("PDFViewerLite/imports")) && hasWriteAccess();
        int localFileVisibility = isLocalFile ? View.VISIBLE : View.GONE;
        TextView deleteFile = findViewById(R.id.tv_bm_delete);
        TextView renameFile = findViewById(R.id.tv_bm_rename);
        deleteFile.setVisibility(localFileVisibility);
        renameFile.setVisibility(localFileVisibility);
        if (isLocalFile) {
            renameFile.setOnClickListener(v -> {
                dismiss();
                showRenameDialog();
            });
            deleteFile.setOnClickListener(v -> {
                dismiss();
                boolean delete = new File(documentFragment.path).delete();
                if (!delete) {
                    Toast.makeText(activity, R.string.err_file_delete, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, R.string.file_delete_success, Toast.LENGTH_LONG).show();
                }
                documentFragment.goToPreviousFragment();
            });
        }
    }

    private void showRenameDialog() {
        String extension = "";
        String fileName = new File(documentFragment.path).getName();
        String fileNameWithOutExt = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
            fileNameWithOutExt = fileName.substring(0, i);
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        final EditText edittext = new EditText(activity);
        edittext.setText(fileNameWithOutExt);
        alert.setMessage(R.string.rename);
        alert.setTitle(R.string.rename_description);
        alert.setView(edittext);
        String finalExtension = extension;
        alert.setPositiveButton(R.string.rename, (dialog, whichButton) -> {
            File destinationFile = new File(new File(documentFragment.path).getParent(),
                    edittext.getText().toString() + "." + finalExtension);
            boolean rename_status = new File(documentFragment.path).renameTo(destinationFile);
            if (!rename_status) {
                Toast.makeText(activity, R.string.err_file_rename, Toast.LENGTH_LONG).show();
            } else {
                MiscUtils.openFile(activity, destinationFile);
                Toast.makeText(activity, R.string.rename_success, Toast.LENGTH_LONG).show();
            }
        });
        alert.setNegativeButton(R.string.dismiss, null);
        alert.show();
    }

    private boolean hasWriteAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        } else return true;
    }

    private void setDialogView(Window window, int layout) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.7f); //0 for no dim to 1 for full dim
        window.setContentView(layout);
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void handleFavoriteClick() {
        if (core.isFavorite()) {
            core.removeFromFavorites();
        } else {
            core.addToFavorites();
        }
    }

    private void showControls() {
        AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(activity);
        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(activity.getString(R.string.shortcuts_title));
        String printable = activity.getString(R.string.shortcuts_description);
        alert.setMessage(printable);
        alert.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.ok), (dialog, which) -> {
        });
        alert.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void printPdfDocument() {
        PrintManager printManager;
        printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        try {
            PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(activity, documentFragment.path);
            printManager.print(new File(documentFragment.path).getName(), printAdapter, new PrintAttributes.Builder().build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void printImage() {
        File printFile = new File(documentFragment.path);
        PrintHelper photoPrinter = new PrintHelper(activity);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        Bitmap bitmap = BitmapFactory.decodeFile(printFile.getAbsolutePath());
        photoPrinter.printBitmap(printFile.getName(), bitmap);
    }

    private void showDocInfo() {
        Map<Integer, String> docInfo = core.getMetadataMap();
        StringBuilder printable = new StringBuilder();
        for (int name : docInfo.keySet()) {
            printable.append(activity.getString(name)).append(" ").append(docInfo.get(name)).append("\n");
        }
        AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(activity);
        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(activity.getString(R.string.pdf_info));
        alert.setMessage(printable);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(R.string.copy),
                (dialog, which) -> MiscUtils.copyTextToClipBoard(activity, printable.toString()));
        alert.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.share),
                (dialog, which) -> SharingUtils.shareText(activity, printable.toString()));
        alert.show();
    }

}

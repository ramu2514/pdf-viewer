package com.avrapps.pdfviewer.results_fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.tools_fragment.PDFUtilities;
import com.avrapps.pdfviewer.tools_fragment.data.PDFUtilsHistory;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.itextpdf.kernel.crypto.BadPasswordException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_NAMES;


public class ResultsFragment extends Fragment {
    private static final int GET_PAGES = 121;
    MainActivity activity;
    String sourceFile, sourceFilePassword, destinationFile;
    HashMap<String, String> selectedFiles;
    int operation = 0;

    public ResultsFragment(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.show_pdf_operation_result_fragment, container, false);
    }

    private void openPageSelectionActivity() {
        Intent i = new Intent(activity, ThumbnailSelectionActivity.class);
        i.putExtra("filePath", sourceFile);
        startActivityForResult(i, GET_PAGES);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (GET_PAGES == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<Integer> result = data.getIntegerArrayListExtra("result");
                int pageNumbers = data.getIntExtra("pageNumbers", -1);
                Collections.sort(result, Collections.reverseOrder());
                if (pageNumbers == result.size()) {
                    Toast.makeText(activity, R.string.donot_select_all_apges, Toast.LENGTH_LONG).show();
                    return;
                }
                deletePages(result);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = activity.findViewById(R.id.my_toolbar);
        toolbar.setTitle("Result");
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            operation = bundle.getInt("operationCode", 0);
            if (operation == 4 || operation == 8) {
                selectedFiles = (HashMap<String, String>) bundle.getSerializable("filePaths");
                ((TextView) view.findViewById(R.id.destination))
                        .setText(selectedFiles.keySet().toString());
            } else {
                sourceFile = bundle.getString("filePath");
                sourceFilePassword = bundle.getString("password", null);
            }
            ((TextView) view.findViewById(R.id.operation)).setText(TOOL_NAMES.get(operation));
            performOperation(operation, view);
        }
    }

    private void performOperation(int operation, View view) {
        view.findViewById(R.id.result_div).setVisibility(View.GONE);

        LinearLayout simpleButtonView = view.findViewById(R.id.button_div);
        View textboxButtonView = view.findViewById(R.id.password_div);
        View pagenumberButtonView = view.findViewById(R.id.page_numbers);
        View deletePagesView = view.findViewById(R.id.two_buttons_with_summary);

        simpleButtonView.setVisibility(View.GONE);
        textboxButtonView.setVisibility(View.GONE);
        pagenumberButtonView.setVisibility(View.GONE);
        deletePagesView.setVisibility(View.GONE);

        File outputDir = new File(Environment.getExternalStorageDirectory(), "PDFViewerLite/Tools/");
        outputDir.mkdirs();

        if (sourceFile == null) {
            String fileName = "";
            for (String file : selectedFiles.keySet()) {
                fileName = file;
                break;
            }
            String extension = "";
            if (operation == 4) {
                extension = ".pdf";
            }
            ((TextView) view.findViewById(R.id.filePath)).setText(selectedFiles.keySet().toString());
            destinationFile = outputDir + "/" + new Date().getTime() + "_" + new File(fileName).getName() + extension;
        } else {
            ((TextView) view.findViewById(R.id.filePath)).setText(sourceFile);
            destinationFile = outputDir + "/" + new Date().getTime() + "_" + new File(sourceFile).getName();
        }
        switch (operation) {
            case 0:
                simpleButtonView.setVisibility(View.VISIBLE);
                compressPdf();
                break;
            case 1:
                textboxButtonView.setVisibility(View.VISIBLE);
                protectPdf();
                break;
            case 2:
                textboxButtonView.setVisibility(View.VISIBLE);
                removePassword();
                break;
            case 3:
                deletePagesView.setVisibility(View.VISIBLE);
                openPageSelectionActivity();
                break;
            case 4:
                simpleButtonView.setVisibility(View.VISIBLE);
                imagesToPdf();
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            case 8:
                simpleButtonView.setVisibility(View.VISIBLE);
                mergePdfFiles();
                break;
            default:
                break;
        }
    }

    private void imagesToPdf() {
        View resultDiv = activity.findViewById(R.id.result_div);
        resultDiv.setVisibility(View.GONE);
        Button button = activity.findViewById(R.id.buttonInButtonDiv);
        button.setText(TOOL_NAMES.get(4));
        button.setOnClickListener(v -> {
            String result = getString(R.string.file_created_images_success);
            try {
                PDFUtilities.imagesToPdf(selectedFiles, destinationFile);
                setDestinationDetails();
            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.failed_pdf_creatoion) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);
        });
    }

    private void mergePdfFiles() {
        View resultDiv = activity.findViewById(R.id.result_div);
        resultDiv.setVisibility(View.GONE);
        Button button = activity.findViewById(R.id.buttonInButtonDiv);
        button.setText(TOOL_NAMES.get(8));
        resultDiv.setVisibility(View.GONE);
        button.setOnClickListener(v -> {
            String result = getString(R.string.pdf_merge_success);
            try {
                PDFUtilities.mergePdfFiles(selectedFiles, destinationFile);
                setDestinationDetails();
            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.failed_merge_pdf) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);
        });
    }

    private void removePassword() {
        View resultDiv = activity.findViewById(R.id.result_div);
        EditText password = activity.findViewById(R.id.password);
        Button action = activity.findViewById(R.id.actionButton);
        action.setText(TOOL_NAMES.get(2));
        resultDiv.setVisibility(View.GONE);
        action.setOnClickListener(v -> {
            action.setEnabled(false);
            String passwordToEncrypt = password.getText().toString();
            String result = getString(R.string.file_decrypt_success);
            try {
                PDFUtilities.removePasswordFromPDF(sourceFile, destinationFile, passwordToEncrypt);
                setDestinationDetails();

            } catch (BadPasswordException ex) {
                ex.printStackTrace();
                result = getString(R.string.bad_password_error);
                action.setEnabled(true);
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.pdf_decrypt_failed) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);

        });
    }

    private void compressPdf() {
        View resultDiv = activity.findViewById(R.id.result_div);
        Button button = activity.findViewById(R.id.buttonInButtonDiv);
        button.setText(TOOL_NAMES.get(0));
        button.setOnClickListener(v -> {
            String result = getString(R.string.file_compress_success);
            try {
                PDFUtilities.compressPdf(sourceFile, sourceFilePassword, destinationFile);
                setDestinationDetails();
            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.error_compress_pdf) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);
        });
    }

    @SuppressLint("SetTextI18n")
    private void deletePages(ArrayList<Integer> deletedPages) {
        ((TextView) activity.findViewById(R.id.text_1)).setText(getString(R.string.selected_pages) + deletedPages);
        View resultDiv = activity.findViewById(R.id.result_div);
        Button reselectButton = activity.findViewById(R.id.button_1);
        reselectButton.setOnClickListener(v -> openPageSelectionActivity());
        Button button = activity.findViewById(R.id.button_2);
        if (deletedPages.size() == 0) {
            Toast.makeText(activity, R.string.select_atleast_one_page, Toast.LENGTH_LONG).show();
        }
        button.setOnClickListener(v -> {
            if (deletedPages.size() == 0) {
                Toast.makeText(activity, R.string.cant_continue_select_atleast_a_page, Toast.LENGTH_LONG).show();
                return;
            }
            reselectButton.setEnabled(false);
            button.setEnabled(false);
            String result = getString(R.string.page_deleted_success);
            try {
                PDFUtilities.deletePages(sourceFile, sourceFilePassword, destinationFile, deletedPages);
                setDestinationDetails();
            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.error_deleting_pages) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);
        });
    }

    private void setDestinationDetails() {
        String extraDetails = "";
        if (operation == 0) {
            String size = Formatter.formatFileSize(activity, new File(sourceFile).length());
            extraDetails = getString(R.string.original_file_size,size);
        }
        File f = new File(destinationFile);
        if (f.exists()) {
            activity.findViewById(R.id.middle_layer).setVisibility(View.GONE);
            String sourceFileName = (sourceFile == null) ? f.getName() : sourceFile;
            String sourceFilePath = (sourceFile == null) ? selectedFiles.keySet().toString()
                    : new File(sourceFile).getAbsolutePath();
            PDFUtilsHistory item = new PDFUtilsHistory(
                    new File(sourceFileName).getName(),
                    sourceFilePath,
                    new File(destinationFile).getAbsolutePath(),
                    operation,
                    new Date().getTime()
            );
            item.save();
            activity.findViewById(R.id.result_share).setVisibility(View.VISIBLE);
            TextView tv = activity.findViewById(R.id.destination_file_details);
            //https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
            String size = Formatter.formatFileSize(activity, f.length());
            tv.setText(getString(R.string.extra_details,f.getName(),size,extraDetails));
            activity.findViewById(R.id.share_file).setOnClickListener((v -> MiscUtils.shareFile(activity, f)));
            activity.findViewById(R.id.open_file).setOnClickListener((v -> MiscUtils.openFile(activity, f)));
            activity.findViewById(R.id.download).setOnClickListener(v -> {
                try {
                    MiscUtils.downloadFile(activity, f);
                    String dest = "";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        dest = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
                    } else {
                        dest = Environment.getExternalStorageDirectory().getAbsolutePath();
                    }
                    String destination = dest + "/" + f.getName();
                    Toast.makeText(activity, getString(R.string.file_saved_to_docs) + destination, Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(activity, R.string.error_saving_pdf, Toast.LENGTH_LONG).show();
                }
            });
            activity.findViewById(R.id.go_home).setOnClickListener((v -> activity.openLibraryFragment(null)));
        }
    }

    private void protectPdf() {
        View resultDiv = activity.findViewById(R.id.result_div);
        EditText password = activity.findViewById(R.id.password);
        Button action = activity.findViewById(R.id.actionButton);
        action.setText(TOOL_NAMES.get(1));
        resultDiv.setVisibility(View.GONE);
        action.setOnClickListener(v -> {
            action.setEnabled(false);
            String passwordToEncrypt = password.getText().toString();
            String result = getString(R.string.file_encrypt_success);
            try {
                PDFUtilities.setPasswordToPDF(sourceFile, sourceFilePassword, destinationFile,
                        passwordToEncrypt, passwordToEncrypt);
                setDestinationDetails();

            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.error_encrypt_pdf) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);
        });
    }

}
package com.avrapps.pdfviewer.results_fragment;

import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_NAMES;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.artifex.mupdf.fitz.Document;
import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.tools_fragment.PDFUtilities;
import com.avrapps.pdfviewer.tools_fragment.data.PDFUtilsHistory;
import com.avrapps.pdfviewer.utils.MessagingUtility;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.itextpdf.kernel.crypto.BadPasswordException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class ResultsFragment extends Fragment {
    private static final int GET_PAGES = 121;
    MainActivity activity;
    String sourceFile, sourceFilePassword, destinationFile;
    LinkedHashMap<String, String> selectedFiles;
    int operation = 0;
    private String openedFrom;

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
                if (operation == 10) {
                    Collections.sort(result, Collections.reverseOrder());
                    rotatePages(result);
                } else {
                    Collections.sort(result, Collections.reverseOrder());
                    if (pageNumbers == result.size()) {
                        Toast.makeText(activity, R.string.donot_select_all_apges, Toast.LENGTH_LONG).show();
                        activity.findViewById(R.id.button_1).setOnClickListener(v -> openPageSelectionActivity());
                        return;
                    }
                    deletePages(result);
                }
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = activity.findViewById(R.id.my_toolbar);
        activity.findViewById(R.id.buttonPanel).setVisibility(View.GONE);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            operation = bundle.getInt("operationCode", 0);
            openedFrom = bundle.getString("openedFrom", "VIEWER");
            if (operation == 4 || operation == 8) {
                selectedFiles = (LinkedHashMap<String, String>) bundle.getSerializable("filePaths");
                ((TextView) view.findViewById(R.id.destination))
                        .setText(selectedFiles.keySet().toString());
            } else {
                sourceFile = bundle.getString("filePath");
                sourceFilePassword = bundle.getString("password", null);
            }
            ((TextView) view.findViewById(R.id.operation)).setText(TOOL_NAMES.get(operation));
            toolbar.setTitle(TOOL_NAMES.get(operation));
            performOperation(operation, view);
        }
    }

    private void performOperation(int operation, View view) {
        view.findViewById(R.id.result_div).setVisibility(View.GONE);

        LinearLayout simpleButtonView = view.findViewById(R.id.button_div);
        View textboxButtonView = view.findViewById(R.id.password_div);
        View pagenumberButtonView = view.findViewById(R.id.split_pages);
        View deletePagesView = view.findViewById(R.id.two_buttons_with_summary);
        View extractPagesView = view.findViewById(R.id.extract_pages);

        simpleButtonView.setVisibility(View.GONE);
        textboxButtonView.setVisibility(View.GONE);
        pagenumberButtonView.setVisibility(View.GONE);
        deletePagesView.setVisibility(View.GONE);
        extractPagesView.setVisibility(View.GONE);

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
                pagenumberButtonView.setVisibility(View.VISIBLE);
                splitPDF();
                break;
            case 8:
                simpleButtonView.setVisibility(View.VISIBLE);
                mergePdfFiles();
                break;
            case 9:
                extractPagesView.setVisibility(View.VISIBLE);
                extractPages();
                break;
            case 10:
                deletePagesView.setVisibility(View.VISIBLE);
                Bundle bundle = this.getArguments();
                int pageNumberToRotateExtra = bundle.getInt("pageNumberToRotate", -1);
                if (pageNumberToRotateExtra != -1) {
                    ArrayList<Integer> list = new ArrayList<>();
                    list.add(pageNumberToRotateExtra);
                    rotatePages(list);
                } else {
                    Toast.makeText(getContext(), R.string.select_page_rotate, Toast.LENGTH_LONG).show();
                    openPageSelectionActivity();
                }
                break;
            case 11:
                simpleButtonView.setVisibility(View.VISIBLE);
                removeWatermark();
                break;
            default:
                break;
        }
        FirebaseUtils.analyticsToolSuccessAction(activity,operation,openedFrom);
    }

    private void removeWatermark() {
        View resultDiv = activity.findViewById(R.id.result_div);
        Button didItWork = activity.findViewById(R.id.didItWork);
        didItWork.setVisibility(View.VISIBLE);
        didItWork.setOnClickListener(v -> MessagingUtility.showHtmlDialogWithPositiveOption(activity,getString(R.string.watermark_failed)));
        Button button = activity.findViewById(R.id.buttonInButtonDiv);
        button.setText(TOOL_NAMES.get(11));
        button.setOnClickListener(v -> {
            didItWork.setVisibility(View.GONE);
            String result = getString(R.string.watermark_remove_success);
            try {
                PDFUtilities.removeWatermark(sourceFile, destinationFile, sourceFilePassword);
                setDestinationDetails();
            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.error_watermark_remove) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);
        });
    }

    private void splitPDF() {
        Document doc = Document.openDocument(sourceFile);
        int totalPages = doc.countPages();
        TextView totalPagesView = activity.findViewById(R.id.totalPages);
        totalPagesView.setText(activity.getString(R.string.total_number_of_pages_in_pdf, String.valueOf(totalPages)));
        Button button = activity.findViewById(R.id.more_info_button);
        button.setOnClickListener((v) -> showSplitHelpWebviewDialog());
        EditText pageNumbers = activity.findViewById(R.id.pageNumbers);
        Button action = activity.findViewById(R.id.split_pages_action);
        View resultDiv = activity.findViewById(R.id.result_div);
        action.setOnClickListener((v) -> {
            HashMap<Integer, Integer> pdfRanges = validatePageNumberRange(pageNumbers.getText().toString(), totalPages);
            if (pdfRanges.size() > 0) {
                String result = getString(R.string.split_pdf_success);
                try {
                    String folderName = new Date().getTime() + new File(sourceFile).getName()
                            .replaceAll(" ", "")
                            .replaceAll("\\.", "")
                            .replaceAll("PDF", "")
                            .replaceAll("pdf", "");
                    File outputDir = new File(Environment.getExternalStorageDirectory(), "PDFViewerLite/Tools/SplitPDF/" + folderName);
                    outputDir.mkdirs();
                    PDFUtilities.splitPDF(sourceFile, sourceFilePassword, outputDir.getAbsolutePath(), pdfRanges);
                    setResultSplitPdf(outputDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    result = getString(R.string.split_pdf_failed) + e.getMessage();
                    activity.findViewById(R.id.result_share).setVisibility(View.GONE);
                }
                resultDiv.setVisibility(View.VISIBLE);
                ((TextView) activity.findViewById(R.id.result_text)).setText(result);
            } else {
                Toast.makeText(activity, R.string.check_error, Toast.LENGTH_LONG).show();
            }
        });
        pageNumbers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePageNumberRange(s.toString(), totalPages);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }


    public void showSplitHelpWebviewDialog() {
        String input = activity.getString(R.string.input);
        String result = activity.getString(R.string.result);
        String hintRangeSplit = activity.getString(R.string.hint_range_split);
        String hintFixedSplit = activity.getString(R.string.hint_fix_split);
        String html = "<html><head><style>" +
                "table {  font-family: Arial, Helvetica, sans-serif;  border-collapse: collapse;  width: 100%;}td, th {  border: 1px solid #ddd;  padding: 8px;}th {  padding-top: 12px;  padding-bottom: 12px;  text-align: left;  background-color: #4CAF50;  color: white;}" +
                "</style></head><body><table>" +
                "<tr><th style='width:30%'>" + input + "</th><th>" + result + "</th></tr>" +
                "<tr><td>1-2,5,6-8</td><td>" + hintRangeSplit + "</td></tr>" +
                "<tr><td>R2</td><td>" + hintFixedSplit + "</td></tr>" +
                "</table></body></html>";
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        WebView webView = new WebView(activity);
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        alert.setCancelable(false);
        alert.setView(webView);
        alert.setPositiveButton(R.string.close, (dialog, i) -> dialog.dismiss());
        alert.show();
    }

    private HashMap<Integer, Integer> validatePageNumberRange(String input, int totalPages) {
        TextView summaryView = activity.findViewById(R.id.page_ranges);
        TextView errorView = activity.findViewById(R.id.error_message);
        summaryView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);

        HashMap<Integer, Integer> pdfRanges = new HashMap<>();
        if (input.matches("^[0-9,-]*$")) {
            try {
                if (input.contains(",")) {
                    String[] ranges = input.split(",");
                    pdfRanges = validateAndGetRanges(ranges, totalPages);
                } else {
                    pdfRanges = validateAndGetRanges(new String[]{input}, totalPages);
                }
            } catch (Exception ex) {
                errorView.setText(ex.getMessage());
                return new HashMap<>();
            }
        } else if (input.matches("^R[0-9]*$")) {
            String rangeNumber = input.replaceAll("R", "");
            if (rangeNumber.isEmpty()) {
                errorView.setText(R.string.split_pdf_provide_valid_range);
                return new HashMap<>();
            } else if (Integer.parseInt(rangeNumber) >= totalPages) {
                errorView.setText(R.string.pdf_split_range_out_of_order);
                return new HashMap<>();
            } else {
                int rangeInt = Integer.parseInt(rangeNumber);
                for (int i = 0; i < totalPages; i += rangeInt) {
                    pdfRanges.put(i + 1, Math.min(i + rangeInt, totalPages));
                }

            }
        } else {
            errorView.setText(R.string.split_pdf_illegal_characters);
            return new HashMap<>();
        }
        String summary = getPDFRageSummary(pdfRanges);
        summaryView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        summaryView.setText(summary);
        return pdfRanges;
    }

    private String getPDFRageSummary(HashMap<Integer, Integer> pdfRanges) {
        if (pdfRanges.size() != 0) {
            int i = 0;
            StringBuilder summary = new StringBuilder();
            for (Integer startPage : pdfRanges.keySet()) {
                i++;
                summary.append(activity.getString(R.string.result_summary_row,
                        String.valueOf(i), String.valueOf(startPage), String.valueOf(pdfRanges.get(startPage))));
            }
            return summary.toString();
        }
        return "";
    }

    private HashMap<Integer, Integer> validateAndGetRanges(final String[] ranges, int totalPages) {
        HashMap<Integer, Integer> pdfRanges = new HashMap<>();
        for (String range : ranges) {
            if (range.contains("-")) {
                String[] numbers = range.split("-");
                String errorMessage = "";
                if (numbers.length != 2) {
                    errorMessage = getString(R.string.split_pdf_invalid_range) + range;
                } else if (numbers[0].isEmpty()) {
                    errorMessage = getString(R.string.split_pdf_invalid_range_start) + range;
                } else if (numbers[1].isEmpty()) {
                    errorMessage = getString(R.string.split_pdf_invalid_range_end) + range;
                } else if (Integer.parseInt(numbers[0]) < 1) {
                    errorMessage = getString(R.string.split_pdf_range_start_out_of_order) + range;
                } else if (Integer.parseInt(numbers[0]) > totalPages) {
                    errorMessage = getString(R.string.split_pdf_range_start_out_of_order_end) + range;
                } else if (Integer.parseInt(numbers[1]) < 1) {
                    errorMessage = getString(R.string.split_pdf_range_end_out_of_order) + range;
                } else if (Integer.parseInt(numbers[1]) > totalPages) {
                    errorMessage = getString(R.string.split_pdf_range_end_out_of_order_end) + range;
                } else if (Integer.parseInt(numbers[1]) < Integer.parseInt(numbers[0])) {
                    errorMessage = getString(R.string.split_pdf_range_end_page_less_than_start) + range;
                }
                if (errorMessage.isEmpty()) {
                    pdfRanges.put(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]));
                } else {
                    throw new RuntimeException(errorMessage);
                }
            } else if (isNumber(range)) {
                if (Integer.parseInt(range) > totalPages) {
                    String errorMessage = getString(R.string.split_pdf_single_page_out_of_range);
                    throw new RuntimeException(errorMessage);
                }
                pdfRanges.put(Integer.parseInt(range), Integer.parseInt(range));
            }
        }
        return pdfRanges;
    }

    private boolean isNumber(String range) {
        for (char c : range.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private void setResultSplitPdf(File outputDir) {
        hideKeyboard();
        activity.findViewById(R.id.go_home).setOnClickListener((v2 -> activity.openLibraryFragment(null)));
        Button shareImagesView = activity.findViewById(R.id.share_file);
        shareImagesView.setText(R.string.share_pdfs);
        shareImagesView.setOnClickListener((v2 -> MiscUtils.shareMultipleFiles(activity, outputDir, "image/*")));
        Button openFileView = activity.findViewById(R.id.open_file);
        openFileView.setText(R.string.view_pdfs);
        openFileView.setOnClickListener((v2 -> MiscUtils.viewMultipleFiles(activity, outputDir)));
        PDFUtilsHistory item = new PDFUtilsHistory(
                new File(sourceFile).getName(),
                new File(sourceFile).getAbsolutePath(),
                outputDir.getAbsolutePath(),
                operation,
                new Date().getTime()
        );
        item.save();
    }

    private void extractPages() {
        String fileName = "PDFViewerLite/Tools/ImageToPDF/" +
                new Date().getTime() + new File(sourceFile).getName()
                .replaceAll(" ", "")
                .replaceAll("\\.", "")
                .replaceAll("PDF", "")
                .replaceAll("pdf", "");
        File outputDir = new File(Environment.getExternalStorageDirectory(), fileName);
        outputDir.mkdirs();

        ((TextView) activity.findViewById(R.id.extract_pages_desc)).setText(activity.getString(R.string.destination_folder_images, fileName));
        View resultDiv = activity.findViewById(R.id.result_div);
        Button button = activity.findViewById(R.id.extract_pages_button);
        button.setOnClickListener(v -> {
            button.setEnabled(false);
            String result = getString(R.string.pages_extracted_success);
            try {
                PDFUtilities.pdfToImages(sourceFile, sourceFilePassword, outputDir);
                activity.findViewById(R.id.download).setVisibility(View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.pages_extract_failed) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            setResultPdfToImages(outputDir);
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);
        });
    }

    private void setResultPdfToImages(File outputDir) {
        hideKeyboard();
        activity.findViewById(R.id.go_home).setOnClickListener((v2 -> activity.openLibraryFragment(null)));
        Button shareImagesView = activity.findViewById(R.id.share_file);
        shareImagesView.setText(R.string.share_images);
        shareImagesView.setOnClickListener((v2 -> MiscUtils.shareMultipleFiles(activity, outputDir, "image/*")));
        Button openFileView = activity.findViewById(R.id.open_file);
        openFileView.setText(R.string.view_images);
        openFileView.setOnClickListener((v2 -> MiscUtils.viewMultipleFiles(activity, outputDir)));
        PDFUtilsHistory item = new PDFUtilsHistory(
                new File(sourceFile).getName(),
                new File(sourceFile).getAbsolutePath(),
                outputDir.getAbsolutePath(),
                operation,
                new Date().getTime()
        );
        item.save();
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
        if (sourceFilePassword != null && !sourceFilePassword.isEmpty()) {
            password.setText(sourceFilePassword);
        }
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
        button.setVisibility(View.VISIBLE);
        activity.findViewById(R.id.rotate_page).setVisibility(View.GONE);

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

    @SuppressLint("SetTextI18n")
    private void rotatePages(ArrayList<Integer> pagesToRotate) {
        ((TextView) activity.findViewById(R.id.text_1)).setText(getString(R.string.selected_pages) + pagesToRotate);
        View resultDiv = activity.findViewById(R.id.result_div);
        Button reselectButton = activity.findViewById(R.id.button_1);
        reselectButton.setOnClickListener(v -> openPageSelectionActivity());
        activity.findViewById(R.id.button_2).setVisibility(View.GONE);
        activity.findViewById(R.id.rotate_page).setVisibility(View.VISIBLE);

        Button button = activity.findViewById(R.id.button_3);
        if (pagesToRotate.size() == 0) {
            Toast.makeText(activity, R.string.select_atleast_one_page, Toast.LENGTH_LONG).show();
        }
        button.setOnClickListener(v -> {
            RadioGroup radioGroup = activity.findViewById(R.id.radioGroup);
            int selectedId = radioGroup.getCheckedRadioButtonId();
            RadioButton radioButton = activity.findViewById(selectedId);
            int angle = 90;
            if (radioButton.getId() == R.id.rotate_180) angle = 180;
            if (radioButton.getId() == R.id.rotate_270) angle = 270;
            if (pagesToRotate.size() == 0) {
                Toast.makeText(activity, R.string.cant_continue_select_atleast_a_page, Toast.LENGTH_LONG).show();
                return;
            }
            reselectButton.setEnabled(false);
            button.setEnabled(false);
            String result = getString(R.string.page_rotate_success);
            try {
                PDFUtilities.rotatePages(sourceFile, sourceFilePassword, destinationFile, pagesToRotate, angle);
                setDestinationDetails();
            } catch (Exception e) {
                e.printStackTrace();
                result = getString(R.string.error_rotating_pages) + e.getMessage();
                activity.findViewById(R.id.result_share).setVisibility(View.GONE);
            }
            resultDiv.setVisibility(View.VISIBLE);
            ((TextView) activity.findViewById(R.id.result_text)).setText(result);
        });
    }

    void hideKeyboard() {
        try {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception ignored) {

        }
    }

    private void setDestinationDetails() {
        hideKeyboard();

        String extraDetails = "";
        if (operation == 0) {
            String size = Formatter.formatFileSize(activity, new File(sourceFile).length());
            extraDetails = getString(R.string.original_file_size, size);
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
            tv.setText(getString(R.string.extra_details, f.getName(), size, extraDetails));
            activity.findViewById(R.id.share_file).setOnClickListener((v -> MiscUtils.shareFile(activity, f)));
            activity.findViewById(R.id.open_file).setOnClickListener((v -> MiscUtils.openFile(activity, f)));
            activity.findViewById(R.id.download).setOnClickListener(v -> {
                try {
                    MiscUtils.downloadFile(activity, f);
                    String dest;
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
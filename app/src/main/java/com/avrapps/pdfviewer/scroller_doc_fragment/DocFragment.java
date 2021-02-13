package com.avrapps.pdfviewer.scroller_doc_fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.avrapps.pdfviewer.R;

import java.io.File;

import static android.app.Activity.RESULT_FIRST_USER;

public class DocFragment extends Fragment {

    private static final int NAVIGATE_REQUEST = 12345;
    private static final String TAG = "DocFragment";
    private final String APP = "Tag";
    protected int pageCount;
    protected int currentPage;
    protected int searchHitPage;
    protected String searchNeedle;
    protected boolean stopSearch;
    protected View currentBar;
    protected View actionBar;
    protected TextView titleLabel;
    protected View searchBar;
    EditText searchText;
    ImageButton searchButton;
    ImageButton searchCloseButton, searchBackwardButton, searchForwardButton;
    View outlineButton;
    ZoomRecyclerView recyclerView;
    NestedScrollView nestedScrollView;
    GalleryAdapter mAdapter;
    private FragmentActivity activity;
    //protected TextView pageLabel;
    private Worker worker;
    private String path;
    private Document doc;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.doc_frag_new, container, false);
        this.activity = getActivity();
        openPDF(view);
        return view;
    }

    protected void askPassword(int message) {
        final EditText passwordView = new EditText(activity);
        passwordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        passwordView.setTransformationMethod(PasswordTransformationMethod.getInstance());

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.enter_password);
        builder.setMessage(message);
        builder.setView(passwordView);
        builder.setPositiveButton(android.R.string.ok, (dialog, id) -> checkPassword(passwordView.getText().toString()));
        builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> goToPreviousFragment());
        builder.setOnCancelListener(dialog -> goToPreviousFragment());
        builder.create().show();
    }

    protected void checkPassword(final String password) {
        worker.add(new Worker.Task() {
            boolean passwordOkay;

            public void work() {
                Log.i(APP, "check password");
                passwordOkay = doc.authenticatePassword(password);
            }

            public void run() {
                if (passwordOkay)
                    loadDocument();
                else
                    askPassword(R.string.enter_password);
            }
        });
    }

    private void openPDF(View view) {
        path = getArguments().getString("path");
        if (!path.contains(".pdf") && !Document.recognize(path)) {
            Toast.makeText(getActivity(), R.string.document_not_supported, Toast.LENGTH_LONG).show();
            getActivity().onBackPressed();
            return;
        }
        titleLabel = view.findViewById(R.id.title_label);
        titleLabel.setText(Uri.fromFile(new File(path)).getLastPathSegment());
        actionBar = view.findViewById(R.id.action_bar);
        searchBar = view.findViewById(R.id.search_bar);
        searchButton = view.findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> showSearch());
        searchText = view.findViewById(R.id.search_text);
        searchText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                search(1);
                return true;
            }
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(1);
                return true;
            }
            return false;
        });
        searchText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                resetSearch();
            }
        });
        searchCloseButton = view.findViewById(R.id.search_close_button);
        searchCloseButton.setOnClickListener(v -> hideSearch());
        searchBackwardButton = view.findViewById(R.id.search_backward_button);
        searchBackwardButton.setOnClickListener(v -> search(-1));
        searchForwardButton = view.findViewById(R.id.search_forward_button);
        searchForwardButton.setOnClickListener(v -> search(1));

        outlineButton = view.findViewById(R.id.outline_button);
        outlineButton.setOnClickListener(v -> {
        });

        worker = new Worker(activity);
        worker.start();
        openDocument();
    }

    protected void openDocument() {
        worker.add(new Worker.Task() {
            boolean needsPassword;

            public void work() {
                Log.i(APP, "open document " + path);
                doc = Document.openDocument(path);
                needsPassword = doc.needsPassword();
            }

            public void run() {
                if (needsPassword)
                    askPassword(R.string.enter_password);
                else {
                    loadDocument();
                }
            }
        });
    }

    protected void loadDocument() {
        worker.add(new Worker.Task() {
            String metaTitle;

            public void work() {
                Log.i(APP, "load document");
                metaTitle = doc.getMetaData(Document.META_INFO_TITLE);
                pageCount = doc.countPages();
            }

            public void run() {
                if (metaTitle != null && !metaTitle.isEmpty())
                    titleLabel.setText(metaTitle);
                loadDocumentFinal();
            }
        });
    }

    private void loadDocumentFinal() {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        recyclerView = activity.findViewById(R.id.recycler_view);
        nestedScrollView = activity.findViewById(R.id.scrollView);
        mAdapter = new GalleryAdapter(activity, doc, metrics.widthPixels, recyclerView);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(activity, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        recyclerView.setMotionEventSplittingEnabled(false);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setEnableScale(true);
    }

    private void goToPreviousFragment() {
        getActivity().onBackPressed();
    }

    protected void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(searchText, 0);
    }

    protected void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
    }

    protected void resetSearch() {
        stopSearch = true;
        searchHitPage = -1;
        searchNeedle = null;
    }

    protected void runSearch(final int startPage, final int direction, final String needle) {
        stopSearch = false;
        worker.add(new Worker.Task() {
            int searchPage = startPage;

            public void work() {
                if (stopSearch || !needle.equals(searchNeedle))
                    return;
                for (int i = 0; i < 9; ++i) {
                    Log.i(APP, "search page " + searchPage);
                    Page page = doc.loadPage(searchPage);
                    Quad[] hits = page.search(searchNeedle);
                    page.destroy();
                    if (hits != null && hits.length > 0) {
                        searchHitPage = searchPage;
                        break;
                    }
                    searchPage += direction;
                    if (searchPage < 0 || searchPage >= pageCount)
                        break;
                }
            }

            public void run() {
                if (stopSearch || !needle.equals(searchNeedle)) {
                    //pageLabel.setText((currentPage + 1) + " / " + pageCount);
                } else if (searchHitPage >= 0) {
                    gotoPage(searchHitPage, true);
                } else {
                    if (searchPage >= 0 && searchPage < pageCount) {
                        //pageLabel.setText((searchPage + 1) + " / " + pageCount);
                        worker.add(this);
                    } else {
                        //pageLabel.setText((currentPage + 1) + " / " + pageCount);
                        Log.i(APP, "search not found");
                        Toast.makeText(activity, getString(R.string.search), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    protected void search(int direction) {
        hideKeyboard();
        int startPage;
        if (searchHitPage == currentPage)
            startPage = currentPage + direction;
        else
            startPage = currentPage;
        searchHitPage = -1;
        searchNeedle = searchText.getText().toString();
        if (searchNeedle.length() == 0)
            searchNeedle = null;
        if (searchNeedle != null)
            if (startPage >= 0 && startPage < pageCount)
                runSearch(startPage, direction, searchNeedle);
    }

    protected void showSearch() {
        currentBar = searchBar;
        actionBar.setVisibility(View.GONE);
        searchBar.setVisibility(View.VISIBLE);
        searchBar.requestFocus();
        showKeyboard();
    }

    protected void hideSearch() {
        currentBar = actionBar;
        actionBar.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.GONE);
        hideKeyboard();
        resetSearch();
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        if (request == NAVIGATE_REQUEST && result >= RESULT_FIRST_USER)
            gotoPage(result - RESULT_FIRST_USER, false);
    }

    public void gotoPage(int p, boolean reloadPage) {
        Log.d(TAG, "Scroll Position : " + p + " : " + pageCount + " : " + currentPage);
        if (p >= 0 && p <= pageCount && currentPage != p) {
            Log.d(TAG, "Scroll Position : " + currentPage);
            currentPage = p;
            final float y = recyclerView.getY() + recyclerView.getChildAt(p).getY();
            nestedScrollView.post(() -> {
                nestedScrollView.fling(0);
                nestedScrollView.smoothScrollTo(0, (int) y);
            });
        }
        if (reloadPage) {
            mAdapter.notifyItemChanged(p);
        }
    }

}

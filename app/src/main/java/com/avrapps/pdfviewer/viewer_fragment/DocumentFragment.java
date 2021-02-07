package com.avrapps.pdfviewer.viewer_fragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PDFWidget;
import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.constants.AppConstants;
import com.avrapps.pdfviewer.data.LastOpenDocuments;
import com.avrapps.pdfviewer.data.PdfBookmarks;
import com.avrapps.pdfviewer.utils.LogUtils;
import com.avrapps.pdfviewer.utils.MessagingUtility;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.PreferenceUtil;
import com.avrapps.pdfviewer.utils.SharingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.avrapps.pdfviewer.constants.AppConstants.AUTO_IMPORT_FILES;

public class DocumentFragment extends Fragment {
    private static final String APP = "Doc2Frag";

    protected int mDisplayDPI;
    protected View mLayoutButton;
    protected ImageButton bookmarksButton;
    protected PopupMenu mLayoutPopupMenu;
    String path;
    String docTitle;
    DrawerLayout drawer;
    boolean isBookmarkTabOpen = false;
    boolean displayDocsList = false;
    boolean shouldExitOnBackButton = false;
    AnnotationsDialog annotationsDialog;
    boolean isEditable = true;
    private MuPDFCore core;
    private String mFileName;
    private String mFileKey;
    private RelativeLayout readerView;
    private ReaderView mDocView;
    private View view;
    private boolean mButtonsVisible;
    private EditText mPasswordView;
    private TextView mFilenameView;
    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private TextView mPageNumberView;
    private ImageButton mSearchButton;
    private MainActivity activity;
    private ImageButton mOutlineButton;
    private ViewAnimator mTopBarSwitcher;
    private RelativeLayout mLowerButtons;
    private TopBarMode mTopBarMode = TopBarMode.Main;
    private ImageButton mSearchBack;
    private ImageButton mSearchFwd;
    private ImageButton mSearchClose;
    private EditText mSearchText;
    private SearchTask mSearchTask;
    private AlertDialog.Builder mAlertBuilder;
    private ArrayList<Item> mFlatOutline;
    private int mLayoutEM = 5;
    private int mLayoutW = 760;
    private int mLayoutH = 1024;
    private boolean isFullscreen = false;
    private ImageButton moreFilesButton;

    private MuPDFCore openFile(String path) {
        int lastSlashPos = path.lastIndexOf('/');
        mFileName = lastSlashPos == -1
                ? path
                : path.substring(lastSlashPos + 1);
        System.out.println("Trying to open " + path);
        try {
            mFileKey = mFileName;
            core = new MuPDFCore(path);
        } catch (Exception e) {
            Log.e(APP, e.toString());
            return null;
        } catch (OutOfMemoryError e) {
            //  out of memory is not an Exception, so we catch it separately.
            Log.e(APP, e.toString());
            return null;
        }
        boolean isFav = core.isFavorite();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean noAutoImport = sp.getBoolean(AUTO_IMPORT_FILES, false);
        if (!noAutoImport) {
            LastOpenDocuments lastOpenDocument = new LastOpenDocuments(path, new Date().getTime(), isFav);
            lastOpenDocument.save();
        }
        return core;
    }

    private void setParents(int visibilty) {
        activity.findViewById(R.id.my_toolbar).setVisibility(visibilty);
        activity.findViewById(R.id.buttonPanel).setVisibility(visibilty);
    }

    void openPDF(Bundle savedInstanceState) {
        if (core == null) {
            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
                mFileName = savedInstanceState.getString("FileName");
            }
        }
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDisplayDPI = metrics.densityDpi;
        mAlertBuilder = new AlertDialog.Builder(activity);
        if (core == null) {
            path = getArguments().getString("path");
            shouldExitOnBackButton = getArguments().getBoolean("shouldQuit", false);
            System.out.println("Path to open is: " + path);
            if (!path.contains(".pdf") && !Document.recognize(path)) {
                Toast.makeText(getActivity(), "Unsupported Document Format", Toast.LENGTH_LONG).show();
                goToPreviousFragment();
                return;
            }
            core = openFile(path);
            SearchTaskResult.set(null);
            if (core != null && core.needsPassword()) {
                requestPassword(savedInstanceState);
                return;
            }
            if (core != null && core.countPages() == 0) {
                core = null;
            }
        }
        if (core == null) {
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle(R.string.cannot_open_document);
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                    (dialog, which) -> goToPreviousFragment());
            alert.setOnCancelListener(dialog -> goToPreviousFragment());
            alert.show();
            return;
        }

        createUI(savedInstanceState);
    }

    public void requestPassword(final Bundle savedInstanceState) {
        mPasswordView = new EditText(activity);
        mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(R.string.enter_password);
        alert.setView(mPasswordView);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
                (dialog, which) -> {
                    if (core.authenticatePassword(mPasswordView.getText().toString())) {
                        createUI(savedInstanceState);
                    } else {
                        requestPassword(savedInstanceState);
                    }
                });
        alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                (dialog, which) -> goToPreviousFragment());
        alert.show();
    }

    public void relayoutDocument() {
        int loc = core.layout(mDocView.mCurrent, mLayoutW, mLayoutH, mLayoutEM);
        mFlatOutline = null;
        mDocView.mHistory.clear();
        mDocView.refresh();
        mDocView.setDisplayedViewIndex(loc);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.document_viewer_fragment, container, false);
        this.activity = (MainActivity) getActivity();
        setParents(View.GONE);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        openPDF(savedInstanceState);
        checkSDCardPermission();
    }

    public void checkSDCardPermission() {
        boolean isPdfFromExternalStorage = false;
        path = getArguments().getString("path");
        DocumentFile fileDirectoty = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Set<String> removableStorages = PreferenceUtil.getStringSet(activity, AppConstants.REMOVABLE_STORAGES);
            if (removableStorages.size() == 0) return;
            isEditable = false;
            boolean isPermissionAlreadyGranted = false;
            for (String removableStorage : removableStorages) {
                String removableStorageName = new File(removableStorage).getName();
                if (path.contains(removableStorageName)) {
                    //pdf file  is from removable storage& should be accessed through SAF.
                    isPdfFromExternalStorage = true;
                    ContentResolver resolver = activity.getContentResolver();
                    List<UriPermission> list = resolver.getPersistedUriPermissions();
                    for (UriPermission permission : list) {
                        if (permission.getUri().toString().contains(removableStorageName)) {
                            isPermissionAlreadyGranted = true;
                            fileDirectoty = getFileDirectoryFromUri(path, permission.getUri(), new File(removableStorage).getAbsolutePath());
                            break;
                        }
                    }
                    break;
                }
            }
            if (!isPdfFromExternalStorage) {
                core.setInternalFile();
                isEditable = true;
                return;
            }
            if (!isPermissionAlreadyGranted) {
                core.setPickedDir(null);
                MessagingUtility.askSafPermission(activity);
                Log.e("Main", "Requesting SAF Permission");
            } else {
                core.setPickedDir(fileDirectoty);
                Log.e("Main", "Doc Id : " + fileDirectoty.getUri());
                isEditable = true;
            }
        }
    }

    /**
     * @param path             Full FolderName = /storage/MicroSD/MyPictures/Wallpapers
     * @param uri              is the persisted permission
     * @param rootAbsolutePath It is String and contains string like /storage/MicroSD
     * @return
     */
    private DocumentFile getFileDirectoryFromUri(String path, Uri uri, String rootAbsolutePath) {
        rootAbsolutePath = rootAbsolutePath + "/";
        int endIndex = path.indexOf(rootAbsolutePath) + rootAbsolutePath.length();
        String newPath = path.substring(endIndex);
        String[] folders = newPath.split("/");
        //folders[] will have folders[0]="MyPictures" folders[1]="Wallpapers"
        DocumentFile directory = DocumentFile.fromTreeUri(activity, uri);
        for (int i = 0; i < folders.length - 1; i++) {
            if (directory != null) {
                directory = directory.findFile(folders[i]);
            } else {
                Toast.makeText(activity, "Can't access the path to document. PDF is not editable", Toast.LENGTH_LONG).show();
            }
        }
        return directory;
    }

    public void createUI(Bundle savedInstanceState) {
        if (core == null)
            return;

        // Now create the UI.
        // First create the document view
        mDocView = new ReaderView(activity, core.isPDF()) {
            @Override
            protected void onMoveToChild(int i) {
                if (core == null)
                    return;
                updatePageNumView(i);
                mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
                mPageSlider.setProgress(i * mPageSliderRes);
                super.onMoveToChild(i);
            }

            @Override
            protected void onSelectModeChange(boolean status) {
                //mFilenameView.setText(status ? "Selection ON. Click 'X' to dismiss" : docTitle);
            }

            @Override
            protected void onTapMainDocArea(List<PDFAnnotation> annotations, List<PDFWidget> widgets) {
                if (annotations.size() > 0 || widgets.size() > 0) {
                    showAnnotations(annotations, widgets);
                    return;
                }
                if (!mButtonsVisible) {
                    showButtons();
                } else {
                    if (mTopBarMode == TopBarMode.Main)
                        hideButtons();
                }
            }

            @Override
            protected void onDocMotion() {
                hideButtons();
            }

            @Override
            public void onSizeChanged(int w, int h, int oldw, int oldh) {
                if (core == null)
                    return;
                if (core.isReflowable()) {
                    mLayoutW = w * 72 / mDisplayDPI;
                    mLayoutH = h * 72 / mDisplayDPI;
                    relayoutDocument();
                } else {
                    refresh();
                }
            }
        };
        mDocView.setAdapter(new PageAdapter(activity, core));

        mSearchTask = new SearchTask(activity, core) {
            @Override
            protected void onTextFound(SearchTaskResult result) {
                SearchTaskResult.set(result);
                // Ask the ReaderView to move to the resulting page
                mDocView.setDisplayedViewIndex(result.pageNumber);
                // Make the ReaderView act on the change to SearchTaskResult
                // via overridden onChildSetup method.
                mDocView.resetupChildren();
            }
        };

        // Make the buttons overlay, and store all its
        // controls in variables
        makeButtonsView();

        // Set up the page slider
        int smax = Math.max(core.countPages() - 1, 1);
        mPageSliderRes = ((10 + smax - 1) / smax) * 2;

        // Set the file-name text
        docTitle = core.getTitle();
        mFilenameView.setText(docTitle);
        // Activate the seekbar
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDocView.pushHistory();
                mDocView.setDisplayedViewIndex((seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                updatePageNumView((progress + mPageSliderRes / 2) / mPageSliderRes);
            }
        });

        // Activate the search-preparing button
        mSearchButton.setOnClickListener(v -> searchModeOn());
        if (getArguments() != null) {
            ArrayList<String> selectedFiles = getArguments().getStringArrayList("additionalFiles");
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                moreFilesButton.setVisibility(View.VISIBLE);
                moreFilesButton.setOnClickListener(v -> showDocList());
            }
        }


        mSearchClose.setOnClickListener(v -> searchModeOff());

        // Search invoking buttons are disabled while there is no text specified
        mSearchBack.setEnabled(false);
        mSearchFwd.setEnabled(false);
        mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

        // React to interaction with the text widget
        mSearchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                // Remove any previous search results
                if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    mDocView.resetupChildren();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }
        });

        //React to Done button on keyboard
        mSearchText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE)
                search(1);
            return false;
        });

        mSearchText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
                search(1);
            return false;
        });

        // Activate search invoking buttons
        mSearchBack.setOnClickListener(v -> search(-1));
        mSearchFwd.setOnClickListener(v -> search(1));


        if (core.isReflowable()) {
            mLayoutButton.setVisibility(View.VISIBLE);
            mLayoutPopupMenu = new PopupMenu(activity, mLayoutButton);
            mLayoutPopupMenu.getMenuInflater().inflate(R.menu.layout_menu, mLayoutPopupMenu.getMenu());
            mLayoutPopupMenu.setOnMenuItemClickListener(item -> {
                float oldLayoutEM = mLayoutEM;
                int id = item.getItemId();
                if (id == R.id.action_layout_4pt) mLayoutEM = 4;
                else if (id == R.id.action_layout_5pt) mLayoutEM = 5;
                else if (id == R.id.action_layout_6pt) mLayoutEM = 6;
                else if (id == R.id.action_layout_7pt) mLayoutEM = 7;
                else if (id == R.id.action_layout_8pt) mLayoutEM = 8;
                else if (id == R.id.action_layout_9pt) mLayoutEM = 9;
                else if (id == R.id.action_layout_10pt) mLayoutEM = 10;
                if (oldLayoutEM != mLayoutEM)
                    relayoutDocument();
                return true;
            });
            mLayoutButton.setOnClickListener(v -> mLayoutPopupMenu.show());
        }

        manageOutline();

        // Reenstate last state if it was recorded
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileKey, 0));

        if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
            showButtons();

        if (savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
            searchModeOn();

        // Stick the document view and the buttons overlay into a parent view
        readerView = view.findViewById(R.id.reader_view);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        int pageTheme = sp.getInt(AppConstants.PAGE_THEME, 0);
        int mTheme = AppConstants.PAGE_THEME_IDS.get(pageTheme);
        int readerBg = getResources().getColor(R.color.white);
        switch (mTheme) {
            case R.id.page_theme_blue:
                readerBg = getResources().getColor(R.color.blue);
                break;
            case R.id.page_theme_dark:
                readerBg = getResources().getColor(R.color.black);
                break;
            case R.id.page_theme_pink:
                readerBg = getResources().getColor(R.color.pink);
                break;
        }
        view.findViewById(R.id.docfragment).setBackgroundColor(readerBg);
        readerView.addView(mDocView);
    }

    private void showAnnotations(List<PDFAnnotation> annotations, List<PDFWidget> widgets) {
        LogUtils.logPDFWidgets(widgets);
        if (!isEditable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                MessagingUtility.askSafPermission(activity);
                return;
            }
        }
        if (isEditable && widgets != null && widgets.size() > 0) {
            PDFWidget widget = widgets.get(widgets.size() - 1);
            if (widget.isText()) {
                MessagingUtility.showCheckBoxDialog(activity, widget.getValue(), returnValue -> {
                    widget.setEditing(true);
                    widget.setValue(returnValue);
                    widget.setEditing(false);
                    widget.update();
                    reload();
                });
            } else if (widget.isCheckbox() || widget.isRadioButton()) {
                widget.toggle();
                reload();
            } else if (widget.isButton() || widget.isPushButton()) {
                core.clickWidget(widget);
                reload();
            } else if (widget.isChoice() || widget.isListBox()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Choose Item");
                builder.setSingleChoiceItems(widget.getOptions(), -1, (dialog, item) -> {
                    widget.setChoiceValue(widget.getOptions()[item]);
                    reload();
                    dialog.dismiss();
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else if (widget.isComboBox()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Choose Item");
                boolean[] ops = new boolean[widget.getOptions().length];
                builder.setMultiChoiceItems(widget.getOptions(), ops, (dialog, which, isChecked) -> {
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
            return;//Do not show annotations further
        }

        LogUtils.logPDFAnnotations(annotations);
        if (annotations != null && annotations.size() > 0) {
            if (annotationsDialog == null) {
                annotationsDialog = new AnnotationsDialog(activity, annotations, core, () -> mDocView.reloadPage(), isEditable);
                annotationsDialog.show();
            } else {
                annotationsDialog.replaceContent(annotations);
                annotationsDialog.show();
            }
        }
    }

    private void showDocList() {
        displayDocsList = true;
        displayDrawerLayout();
        if (drawer != null) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            drawer.openDrawer(GravityCompat.END);
        }
    }

    private void reload() {
        core.savePage();
        core.saveDocument(activity, x -> {
            mDocView.reloadPage();
        });
        Toast.makeText(activity, "Form might not refresh until document is reopened", Toast.LENGTH_LONG).show();
    }

    private void manageOutline() {
        drawer = view.findViewById(R.id.drawer_layout);
        if (core.hasOutline() || core.hasBookmarks()) {
            mOutlineButton.setVisibility(View.VISIBLE);
            if (mFlatOutline == null)
                mFlatOutline = core.getOutline();
            if (mFlatOutline != null) {
                drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                    }

                    @Override
                    public void onDrawerOpened(@NonNull View drawerView) {
                        if (!displayDocsList) {
                            displayDrawerLayout();
                        }
                    }

                    @Override
                    public void onDrawerClosed(@NonNull View drawerView) {
                        if (displayDocsList) {
                            displayDocsList = false;
                        }
                    }

                    @Override
                    public void onDrawerStateChanged(int newState) {
                    }
                });
                mOutlineButton.setOnClickListener(v -> {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    drawer.openDrawer(GravityCompat.END);
                });
            }
        } else {
            mOutlineButton.setVisibility(View.GONE);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    private void displayDrawerLayout() {
        boolean hasOutline = core.hasOutline();
        boolean hasBookmarks = core.hasBookmarks();
        RadioGroup outlineGroup = view.findViewById(R.id.outline_groups);
        if (displayDocsList) {
            view.findViewById(R.id.outline_groups).setVisibility(View.GONE);
            assert getArguments() != null;
            ArrayList<String> selectedFiles = getArguments().getStringArrayList("additionalFiles");
            ArrayList<String> fileNames = new ArrayList<>();
            assert selectedFiles != null;
            for (String file : selectedFiles) {
                int lastSlashPos = file.lastIndexOf('/');
                fileNames.add(lastSlashPos == -1 ? file : file.substring(lastSlashPos + 1));
            }
            ((TextView) view.findViewById(R.id.header)).setText(R.string.other_files);
            ListView lv = view.findViewById(R.id.list_slidemenu);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
            adapter.addAll(fileNames);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener((parent, view, position, id) -> {
                Uri fileUri = Uri.fromFile(new File(selectedFiles.get(position)));
                MiscUtils.openDoc(fileUri, activity, selectedFiles);
                drawer.closeDrawer(GravityCompat.END);
            });
            return;
        }
        if (hasOutline && hasBookmarks) {
            outlineGroup.setVisibility(View.VISIBLE);
            view.findViewById(R.id.radio0).setOnClickListener(v -> displayOutline());
            view.findViewById(R.id.radio1).setOnClickListener(v -> displayBookmarks());
            if (isBookmarkTabOpen) displayBookmarks();
            else displayOutline();
        } else {
            outlineGroup.setVisibility(View.GONE);
            if (hasBookmarks) {
                displayBookmarks();
            } else {
                displayOutline();
            }
        }
    }

    private void displayBookmarks() {
        isBookmarkTabOpen = true;
        ((TextView) view.findViewById(R.id.header)).setText(R.string.bookmarks);
        ListView lv = view.findViewById(R.id.list_slidemenu);
        ArrayAdapter<PdfBookmarks> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
        adapter.addAll(core.getBookmarks());
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            long page = adapter.getItem(position).getPage();
            mDocView.setDisplayedViewIndex((int) page - 1);
            drawer.closeDrawer(GravityCompat.END);
        });

    }

    private void displayOutline() {
        isBookmarkTabOpen = false;
        ((TextView) view.findViewById(R.id.header)).setText(R.string.toc);
        ListView lv = view.findViewById(R.id.list_slidemenu);
        ArrayAdapter<Item> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
        int currentPage = mDocView.getDisplayedViewIndex();
        int found = -1;
        if (mFlatOutline == null) mFlatOutline = core.getOutline();
        for (int i = 0; i < mFlatOutline.size(); ++i) {
            Item item = mFlatOutline.get(i);
            if (found < 0 && item.page >= currentPage)
                found = i;
            adapter.add(item);
        }
        if (found >= 0)
            lv.setSelection(found);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            int page = adapter.getItem(position).page;
            mDocView.setDisplayedViewIndex(page);
            drawer.closeDrawer(GravityCompat.END);
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFileKey != null && mDocView != null) {
            if (mFileName != null)
                outState.putString("FileName", mFileName);

            // Store current page in the prefs against the file name,
            // so that we can pick it up each time the file is loaded
            // Other info is needed only for screen-orientation change,
            // so it can go in the bundle
            SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileKey, mDocView.getDisplayedViewIndex());
            edit.apply();
        }

        if (!mButtonsVisible)
            outState.putBoolean("ButtonsHidden", true);

        if (mTopBarMode == TopBarMode.Search)
            outState.putBoolean("SearchMode", true);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSearchTask != null)
            mSearchTask.stop();

        if (mFileKey != null && mDocView != null) {
            SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileKey, mDocView.getDisplayedViewIndex());
            edit.apply();
        }
    }

    public void onDestroy() {
        if (mDocView != null) {
            mDocView.applyToChildren(new ReaderView.ViewMapper() {
                void applyToView(View view) {
                    ((PageView) view).releaseBitmaps();
                }
            });
        }
        if (core != null)
            core.onDestroy();
        core = null;
        super.onDestroy();
    }

    private void setButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setColorFilter(enabled ? Color.argb(255, 255, 255, 255) : Color.argb(255, 128, 128, 128));
    }

    private void showButtons() {
        if (core == null)
            return;
        if (!mButtonsVisible) {
            mButtonsVisible = true;
            // Update page number text and slider
            int index = mDocView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
            mPageSlider.setProgress(index * mPageSliderRes);
            if (mTopBarMode == TopBarMode.Search) {
                mSearchText.requestFocus();
                showKeyboard();
            }
            mTopBarSwitcher.setVisibility(View.VISIBLE);
            mLowerButtons.setVisibility(View.VISIBLE);
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {
            mButtonsVisible = false;
            hideKeyboard();
            if (isFullscreen) {
                mTopBarSwitcher.setVisibility(View.INVISIBLE);
                mLowerButtons.setVisibility(View.INVISIBLE);
            } else {
                mLowerButtons.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void searchModeOn() {
        if (mTopBarMode != TopBarMode.Search) {
            mTopBarMode = TopBarMode.Search;
            //Focus on EditTextWidget
            mSearchText.requestFocus();
            showKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        }
    }

    private void searchModeOff() {
        if (mTopBarMode != TopBarMode.Main) {
            mTopBarMode = TopBarMode.Main;
            hideKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
            SearchTaskResult.set(null);
            // Make the ReaderView act on the change to mSearchTaskResult
            // via overridden onChildSetup method.
            mDocView.resetupChildren();
        }
    }

    private void updatePageNumView(int index) {
        if (core == null)
            return;
        mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", index + 1, core.countPages()));
        bookmarksButton.setImageResource(core.isBookmarkedAlready(index + 1) ? R.drawable.ic_bookmark_on : R.drawable.ic_bookmark_off);
    }

    private void makeButtonsView() {
        mFilenameView = view.findViewById(R.id.docNameText);
        mPageSlider = view.findViewById(R.id.pageSlider);
        mPageNumberView = view.findViewById(R.id.pageNumber);
        mSearchButton = view.findViewById(R.id.searchButton);
        moreFilesButton = view.findViewById(R.id.moreFilesButton);
        mOutlineButton = view.findViewById(R.id.outlineButton);
        mTopBarSwitcher = view.findViewById(R.id.switcher);
        mLowerButtons = view.findViewById(R.id.lowerButtons);
        mSearchBack = view.findViewById(R.id.searchBack);
        mSearchFwd = view.findViewById(R.id.searchForward);
        mSearchClose = view.findViewById(R.id.searchClose);
        mSearchText = view.findViewById(R.id.searchText);
        mLayoutButton = view.findViewById(R.id.layoutButton);
        bookmarksButton = view.findViewById(R.id.bookmarkButton);
        handleBookmarks();
        view.findViewById(R.id.moreOptions).setOnClickListener(this::showPoPup);
        view.findViewById(R.id.seamlessMode).setOnClickListener(this::goFullScreen);
        if (isFullscreen) {
            mTopBarSwitcher.setVisibility(View.INVISIBLE);
            mLowerButtons.setVisibility(View.INVISIBLE);
        }
    }

    private void handleBookmarks() {
        //if (core.countPages() < 3) return;
        bookmarksButton.setVisibility(View.VISIBLE);
        bookmarksButton.setOnClickListener(v -> {
            if (core.isBookmarkedAlready()) {
                core.removeBookmark();
                bookmarksButton.setImageResource(R.drawable.ic_bookmark_off);
            } else {
                core.addBookmark();
                bookmarksButton.setImageResource(R.drawable.ic_bookmark_on);
            }
            manageOutline();
        });
    }

    private void showPoPup(View v) {
        PopupMenu popup = new PopupMenu(activity, v);
        popup.getMenuInflater().inflate(R.menu.actions, popup.getMenu());
        popup.getMenu().findItem(R.id.print).setVisible((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) && core.isPDF());

        popup.getMenu().findItem(R.id.favorites).setTitle
                (core.isFavorite() ? R.string.unmark_favorite : R.string.mark_favorite);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.share:
                    SharingUtils.sharePdf(activity, path);
                    break;
                case R.id.print:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        printPdf(activity, path);
                    }
                    break;
                case R.id.fileInformation:
                    showDocInfo();
                    break;
                case R.id.demo:
                    showControls();
                    break;
                case R.id.favorites:
                    handleFavoriteClick();
                    break;
            }
            return true;
        });

        popup.show();
    }

    private void handleFavoriteClick() {
        if (core.isFavorite()) {
            core.removeFromFavorites();
        } else {
            core.addToFavorites();
        }
    }

    private void showControls() {
        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(getString(R.string.shortcuts_title));
        String printable = getString(R.string.shortcuts_description);
        alert.setMessage(printable);
        alert.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok), (dialog, which) -> {
        });
        alert.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void printPdf(FragmentActivity activity, String path) {
        PrintManager printManager;
        printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        try {
            PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(activity, path);
            printManager.print("Document", printAdapter, new PrintAttributes.Builder().build());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void showDocInfo() {
        Map<Integer, String> docinfo = core.getMetadataMap();
        StringBuilder printable = new StringBuilder();
        for (int name : docinfo.keySet()) {
            printable.append(getString(name)).append(" ").append(docinfo.get(name)).append("\n");
        }
        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(getString(R.string.pdf_info));
        alert.setMessage(printable);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.copy),
                (dialog, which) -> MiscUtils.copyTextToClipBoard(activity, printable.toString()));
        alert.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.share),
                (dialog, which) -> SharingUtils.shareText(activity, printable.toString()));
        alert.show();
    }

    private void goFullScreen(View v) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        if (isFullscreen) {
            isFullscreen = false;
            if (v != null) {
                ((ImageView) (v)).setImageResource(R.drawable.ic_fullscreen_exit_white_24dp);
                params.setMargins(0, px, 0, px);
                if (readerView != null)
                    readerView.setLayoutParams(params);
                exitFullScreen();
            }
        } else {
            isFullscreen = true;
            mButtonsVisible = true;
            hideButtons();
            if (v != null) {
                ((ImageView) (v)).setImageResource(R.drawable.ic_fullscreen_white_24dp);
                params.setMargins(0, 0, 0, 0);
                if (readerView != null)
                    readerView.setLayoutParams(params);
            }
            showFullScreen();
        }
    }

    private void showFullScreen() {
        final View decorView = activity.getWindow().getDecorView();
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> decorView.setSystemUiVisibility(flags));
    }

    void exitFullScreen() {
        final View decorView = activity.getWindow().getDecorView();
        final int clearFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        activity.getWindow().getDecorView().setSystemUiVisibility(clearFlags);
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> decorView.setSystemUiVisibility(clearFlags));
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(mSearchText, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

/*    @Override
    public boolean onSearchRequested() {
        if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOn();
        }
        return super.onSearchRequested();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOff();
        }
        return super.onPrepareOptionsMenu(menu);
    }*/

    private void search(int direction) {
        hideKeyboard();
        int displayPage = mDocView.getDisplayedViewIndex();
        SearchTaskResult r = SearchTaskResult.get();
        int searchPage = r != null ? r.pageNumber : -1;
        mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
    }

    private void goToPreviousFragment() {
        dismissPopup();
        activity.onBackPressed();
    }

    private void dismissPopup() {
        if (mDocView != null)
            mDocView.dismissPopup();
    }

    //returns true indicate to close the fragment
    public boolean onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
            return false;
        }
        if (shouldExitOnBackButton) {
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        setParents(View.GONE);
    }

    /* The core rendering instance */
    enum TopBarMode {Main, Search}

    public static class Item implements Serializable {
        public String title;
        public int page;

        public Item(String title, int page) {
            this.title = title;
            this.page = page;
        }

        @NonNull
        public String toString() {
            return title;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static class PdfDocumentAdapter extends PrintDocumentAdapter {

        Context context;
        String pathName;

        public PdfDocumentAdapter(Context ctxt, String pathName) {
            context = ctxt;
            this.pathName = pathName;
        }

        @Override
        public void onLayout(PrintAttributes printAttributes, PrintAttributes printAttributes1, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle bundle) {
            if (cancellationSignal.isCanceled()) {
                layoutResultCallback.onLayoutCancelled();
            } else {
                PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder(" file name");
                builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build();
                layoutResultCallback.onLayoutFinished(builder.build(),
                        !printAttributes1.equals(printAttributes));
            }
        }

        @Override
        public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
            InputStream in = null;
            OutputStream out = null;
            try {
                File file = new File(pathName);
                in = new FileInputStream(file);
                out = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

                byte[] buf = new byte[16384];
                int size;

                while ((size = in.read(buf)) >= 0
                        && !cancellationSignal.isCanceled()) {
                    out.write(buf, 0, size);
                }

                if (cancellationSignal.isCanceled()) {
                    writeResultCallback.onWriteCancelled();
                } else {
                    writeResultCallback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                }
            } catch (Exception e) {
                writeResultCallback.onWriteFailed(e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

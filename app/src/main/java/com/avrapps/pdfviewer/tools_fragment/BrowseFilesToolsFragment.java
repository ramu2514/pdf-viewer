package com.avrapps.pdfviewer.tools_fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.utils.DateTimeUtils;
import com.avrapps.pdfviewer.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BrowseFilesToolsFragment extends Fragment {

    public static final int REQUEST_CODE_DOC_NEW = 1212;
    MainActivity activity;
    FilesListRecycler recyclerAdapter;
    String rootFolderPath;
    File currentDir;
    View view;
    RecyclerView recyclerView;
    boolean isStorageListed = false;
    Toolbar toolbar;
    List<String> extensions;
    boolean multiSelect = false, passwordCheck = true;
    private boolean showHidden = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar = activity.findViewById(R.id.my_toolbar);
        toolbar.setTitle(R.string.select_pdf);
        if (multiSelect) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browsefiles_tools, container, false);
        activity = (MainActivity) getActivity();
        this.view = view;
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String formats = bundle.getString("formats", ".pdf");
            multiSelect = bundle.getBoolean("multiSelect", false);
            passwordCheck = bundle.getBoolean("passwordCheck", true);
            extensions = Arrays.asList(formats.split(","));
        }
        view.findViewById(R.id.pick_file).setOnClickListener(v -> pickAFile());
        SwitchCompat switchCompat = view.findViewById(R.id.hidden_files);
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showHidden = isChecked;
            refreshFiles();
        });
        recyclerView = view.findViewById(R.id.storages_view);
        openStorage();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_thumbnails, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void openStorage() {
        isStorageListed = true;
        ArrayList<StorageUtils.StorageDirectoryParcelable> storages = new StorageUtils(activity).getStorageList();
        view.findViewById(R.id.no_files).setVisibility(storages.size() == 0 ? View.VISIBLE : View.GONE);
        Log.d("BrowseFilesToolsFragmen", String.valueOf(storages));
        StorageListRecycler recyclerAdapter = new StorageListRecycler(activity, storages);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(recyclerAdapter);
        setFilterOptionsVisibility(View.GONE);
    }

    private void setFilterOptionsVisibility(int visibility) {
        int invertVisibility = (visibility == View.VISIBLE) ? View.GONE : View.VISIBLE;
        view.findViewById(R.id.pick_file).setVisibility(View.GONE);//todo:check
        view.findViewById(R.id.filterHeader).setVisibility(visibility);
    }

    private void openFiles(String rootFolderPath) {
        isStorageListed = false;
        setFilterOptionsVisibility(View.VISIBLE);
        currentDir = new File(rootFolderPath);
        File[] filesList = listOfFiles(currentDir);
        view.findViewById(R.id.no_files).setVisibility(filesList.length == 0 ? View.VISIBLE : View.GONE);
        recyclerAdapter = new FilesListRecycler(activity, filesList, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(recyclerAdapter);
    }

    private File[] listOfFiles(File currentDirectory) {

        List<File> dirs = new ArrayList<>();
        List<File> fs = new ArrayList<>();
        File[] files = currentDirectory.listFiles();
        if (files == null) return dirs.toArray(new File[0]);
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                if (showHidden || !file.getName().startsWith("."))
                    dirs.add(file);
            } else {
                String extension = "";
                int i = fileName.lastIndexOf('.');
                if (i > 0) {
                    extension = fileName.substring(i);
                }
                extension = extension.toLowerCase();
                if (extensions.contains(extension))
                    fs.add(file);
            }
        }
        Collections.sort(dirs, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
        Collections.sort(fs, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
        dirs.addAll(fs);
        return dirs.toArray(new File[0]);
    }

    public void onBackPressed() {
        if (isStorageListed) {
            activity.openLibraryFragment(null);
            return;
        }
        if (currentDir != null && currentDir.getAbsolutePath().equals(rootFolderPath)) {
            openStorage();
            return;
        }
        if (currentDir != null) currentDir = currentDir.getParentFile();
        refreshFiles();
    }

    private void refreshFiles() {
        if (currentDir == null) return;
        File[] newFiles = listOfFiles(currentDir);
        view.findViewById(R.id.no_files).setVisibility(newFiles.length == 0 ? View.VISIBLE : View.GONE);
        recyclerAdapter.setData(newFiles);
        recyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_DOC_NEW) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                //MiscUtils.openDoc(data.getData(), activity, new ArrayList<>());
                //todo:check logic
            }
        }
    }


    private void pickAFile() {
        Log.d("TAG", "Browse Documents");
        String[] mimeTypes = {"application/pdf", "application/vnd.ms-xpsdocument", "application/oxps", "application/x-cbz", "application/vnd.comicbook+zip", "application/epub+zip", "application/x-fictionbook"};

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        } else {
            StringBuilder mimeTypesStr = new StringBuilder();
            for (String mimeType : mimeTypes) {
                mimeTypesStr.append(mimeType).append("|");
            }
            intent.setType(mimeTypesStr.substring(0, mimeTypesStr.length() - 1));
        }
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_file)), REQUEST_CODE_DOC_NEW);
        } else {
            Toast.makeText(getActivity(), R.string.no_file_chosen, Toast.LENGTH_LONG).show();
        }
    }

    public class StorageListRecycler extends RecyclerView.Adapter<StorageListRecycler.ViewHolder> {

        private final LayoutInflater mInflater;
        List<StorageUtils.StorageDirectoryParcelable> data;

        StorageListRecycler(Context context, List<StorageUtils.StorageDirectoryParcelable> data) {
            if (toolbar != null) {
                toolbar.setTitle(R.string.title_select_storage);
            }
            this.mInflater = LayoutInflater.from(context);
            this.data = data;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.row_item_storage, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.storageName.setText(data.get(position).getName());
            holder.storagePath.setText(data.get(position).getPath());
            holder.storageIcon.setImageDrawable(ContextCompat.getDrawable(activity, data.get(position).getIcon()));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView storageName, storagePath;
            ImageView storageIcon;

            ViewHolder(View itemView) {
                super(itemView);
                storageName = itemView.findViewById(R.id.storage_name);
                storagePath = itemView.findViewById(R.id.storage_path);
                storageIcon = itemView.findViewById(R.id.storage_icon);
                View.OnClickListener listener = v -> {
                    int position = getAdapterPosition();
                    rootFolderPath = data.get(position).getPath();
                    openFiles(rootFolderPath);
                };
                itemView.setOnClickListener(listener);
            }
        }
    }

    public class FilesListRecycler extends RecyclerView.Adapter<FilesListRecycler.ViewHolder> {

        private final LayoutInflater mInflater;
        File[] data;
        private boolean checkboxVisible;
        private HashMap<String, String> selectedFiles = new HashMap<>();

        FilesListRecycler(Context context, File[] data, boolean selection) {
            this.mInflater = LayoutInflater.from(context);
            this.data = data;
            this.checkboxVisible = selection;
            toolbar.setTitle(R.string.title_select_files);
            TextView openAction = activity.findViewById(R.id.open_Action);
            openAction.setVisibility(multiSelect ? View.VISIBLE : View.GONE);
            openAction.setOnClickListener(v -> {
                if (selectedFiles.size() == 0) {
                    Toast.makeText(activity, R.string.select_atleast_one_file, Toast.LENGTH_LONG).show();
                } else {
                    activity.continueOperationsOnFileSelect(selectedFiles,-1);
                }
            });
        }

        public void setData(File[] data) {
            this.data = data;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.row_item_storage, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.storageName.setText(data[position].getName());
            holder.storagePath.setText(getString(R.string.modified_) + DateTimeUtils.getTimeAgo(data[position].lastModified(),activity));
            holder.checkBox.setVisibility(checkboxVisible && !data[position].isDirectory() ? View.VISIBLE : View.GONE);
            selectedFiles.keySet();
            if (selectedFiles.containsKey(data[position].getAbsolutePath()))
                holder.checkBox.setChecked(true);
            setIcon(holder.storageIcon, data[position]);
        }

        private void setIcon(ImageView storageIcon, File file) {
            if (file.isDirectory()) {
                storageIcon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_folder_black_24dp));
                return;
            }
            AsyncTask.execute(() -> {
                try {
                    Document pdfDocument = Document.openDocument(file.getAbsolutePath());
                    if (pdfDocument.needsPassword()) {
                        activity.runOnUiThread(() -> storageIcon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_lock_black_24dp)));
                    }
                    Page page = pdfDocument.loadPage(0);
                    Bitmap bitmap = AndroidDrawDevice.drawPageFitWidth(page, 100);
                    activity.runOnUiThread(() -> storageIcon.setImageBitmap(bitmap));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.length;
        }

        private void resolvePasswords(String datum, boolean shouldCallMain, CheckBox checkBox) {
            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
            alert.setMessage(R.string.entr_password_desc);
            alert.setTitle(R.string.enter_password_title);
            if (checkBox != null) {
                checkBox.setChecked(false);
            }

            final EditText edittext = new EditText(activity);
            alert.setView(edittext);

            alert.setCancelable(false);
            alert.setPositiveButton(R.string.done, (dialog, whichButton) -> {
                String password = edittext.getText().toString();
                if (PDFUtilities.isPasswordValid(datum,password)) {
                    dialog.cancel();
                    if (shouldCallMain) {
                        activity.continueOperationsOnFileSelect(datum, password, -1);
                    } else if (checkBox != null) {
                        selectedFiles.put(datum, password);
                        checkBox.setChecked(true);
                    }
                } else {
                    if (checkBox != null) {
                        checkBox.setChecked(false);
                    }
                    Toast.makeText(activity, R.string.incorrect_password,Toast.LENGTH_LONG).show();
                    edittext.setText("");
                }
            });

            alert.setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
                dialog.cancel();
            });
            alert.show();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView storageName, storagePath;
            ImageView storageIcon;
            CheckBox checkBox;

            ViewHolder(View itemView) {
                super(itemView);
                storageName = itemView.findViewById(R.id.storage_name);
                storagePath = itemView.findViewById(R.id.storage_path);
                storageIcon = itemView.findViewById(R.id.storage_icon);
                checkBox = itemView.findViewById(R.id.checkbox);
                if (multiSelect)
                    checkboxVisible = true;
                if (checkboxVisible) {
                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        int position = getAdapterPosition();
                        if (isChecked) {
                            String path = data[position].getAbsolutePath();
                            if (!selectedFiles.containsKey(path)) {
                                if (passwordCheck && PDFUtilities.isPasswordProtected(data[position].getAbsolutePath())) {
                                    resolvePasswords(data[position].getAbsolutePath(), false, checkBox);
                                } else {
                                    selectedFiles.put(data[position].getAbsolutePath(), "");
                                }
                            }
                        } else {
                            selectedFiles.remove(data[position].getAbsolutePath());
                        }
                        toolbar.setTitle(getString(R.string._selected,selectedFiles.size()));
                    });
                }
                View.OnClickListener listener = v -> {
                    int position = getAdapterPosition();
                    if (data[position].isDirectory()) {
                        currentDir = data[position];
                        refreshFiles();
                    } else {
                        if (!checkboxVisible) {
                            if (passwordCheck && PDFUtilities.isPasswordProtected(data[position].getAbsolutePath())) {
                                resolvePasswords(data[position].getAbsolutePath(), true, null);
                            } else {
                                activity.continueOperationsOnFileSelect(data[position].getAbsolutePath(), null, -1);
                            }
                        }
                    }
                };
                itemView.setOnClickListener(listener);

            }
        }
    }


}

package com.avrapps.pdfviewer.library_fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.avrapps.pdfviewer.results_fragment.FirebaseUtils;
import com.avrapps.pdfviewer.utils.DateTimeUtils;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowseFilesFragment extends Fragment {

    public static final int REQUEST_CODE_DOC_NEW = 1212;
    private static final List<String> SUPPORTED_LIST = Arrays.asList(".pdf", ".xps", ".oxps", ".cbz", ".epub", ".fb2", ".tif", ".tiff");
    MainActivity activity;
    List<String> allowedList = SUPPORTED_LIST;
    FilesListRecycler recyclerAdapter;
    String rootFolderPath;
    File currentDir;
    View view;
    RecyclerView recyclerView;
    boolean isStorageListed = false;
    Toolbar toolbar;
    private boolean allowAll = true;
    private boolean showHidden = false;
    private boolean enableSelection = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar = activity.findViewById(R.id.my_toolbar);
        toolbar.setTitle(R.string.title_select_storage);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browsefiles, container, false);
        activity = (MainActivity) getActivity();
        this.view = view;
        view.findViewById(R.id.pick_file).setOnClickListener(v -> pickAFile());
        SwitchCompat switchCompat = view.findViewById(R.id.hidden_files);
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showHidden = isChecked;
            refreshFiles();
        });

        view.findViewById(R.id.selectMultiple).setOnClickListener(this::initEnableSelection);
        Spinner spinner = view.findViewById(R.id.spinner_nav);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity, R.array.spinner_list_item_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                allowAll = false;
                switch (position) {
                    default:
                        allowedList = SUPPORTED_LIST;
                        allowAll = true;
                        break;
                    case 1:
                        allowedList = Collections.singletonList(".pdf");
                        break;
                    case 2:
                        allowedList = Collections.singletonList(".epub");
                        break;
                    case 3:
                        allowedList = Arrays.asList(".xps", ".oxps");
                        break;
                    case 4:
                        allowedList = Collections.singletonList(".fb2");
                        break;
                    case 5:
                        allowedList = Arrays.asList(".tif", ".tiff");
                        break;
                    case 6:
                        allowedList = Collections.singletonList(".cbz");
                        break;
                }
                refreshFiles();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
        recyclerView = view.findViewById(R.id.storages_view);

        openStorages();

        return view;
    }

    private void initEnableSelection(View v) {
        if (enableSelection) {
            recyclerAdapter.openSelected();
        } else {
            openFiles(currentDir.getAbsolutePath(), true);
            ((ImageView) v).setImageResource(R.drawable.ic_baseline_open_in_browser_black_24);
            enableSelection = true;
            Toast.makeText(activity,R.string.select_multiple_enabled,Toast.LENGTH_LONG).show();
        }
    }

    private void openStorages() {
        isStorageListed = true;
        ArrayList<StorageUtils.StorageDirectoryParcelable> storages = new StorageUtils(activity).getStorageList();
        view.findViewById(R.id.no_files).setVisibility(storages.size() == 0 ? View.VISIBLE : View.GONE);
        Log.d("BrowseFilesFragment", String.valueOf(storages));
        StorageListRecycler recyclerAdapter = new StorageListRecycler(activity, storages);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(recyclerAdapter);
        setFilterOptionsVisibility(View.GONE);
    }

    private void setFilterOptionsVisibility(int visibility) {
        int invertVisibility = (visibility == View.VISIBLE) ? View.GONE : View.VISIBLE;
        view.findViewById(R.id.pick_file).setVisibility(invertVisibility);
        view.findViewById(R.id.filterHeader).setVisibility(visibility);
    }

    private void openFiles(String rootFolderPath, boolean enableSelection) {
        isStorageListed = false;
        setFilterOptionsVisibility(View.VISIBLE);
        currentDir = new File(rootFolderPath);
        File[] filesList = listOfFiles(currentDir);
        view.findViewById(R.id.no_files).setVisibility(filesList.length == 0 ? View.VISIBLE : View.GONE);
        recyclerAdapter = new FilesListRecycler(activity, filesList, enableSelection);
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
                if (!allowAll && allowedList.contains(extension))
                    fs.add(file);
                if (allowAll && (allowedList.contains(extension) || Document.recognize(file.getAbsolutePath())))
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
            activity.finish();
            return;
        }
        if (currentDir != null && currentDir.getAbsolutePath().equals(rootFolderPath)) {
            openStorages();
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
                FirebaseUtils.analyticsFileOpen(activity,"FILE_OPEN_PICK_FILE", data.getData());
                MiscUtils.openDoc(data.getData(), activity, new ArrayList<>());
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
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_file)), REQUEST_CODE_DOC_NEW);
        } else {
            Toast.makeText(getActivity(), R.string.no_file_chooser, Toast.LENGTH_LONG).show();
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
        public StorageListRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.row_item_storage, parent, false);
            return new StorageListRecycler.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StorageListRecycler.ViewHolder holder, int position) {
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
                    openFiles(rootFolderPath, false);
                };
                itemView.setOnClickListener(listener);
            }
        }
    }

    public class FilesListRecycler extends RecyclerView.Adapter<FilesListRecycler.ViewHolder> {

        private final LayoutInflater mInflater;
        File[] data;
        private final boolean checkboxVisible;
        private final ArrayList<String> selectedFiles = new ArrayList<>();

        FilesListRecycler(Context context, File[] data, boolean selection) {
            this.mInflater = LayoutInflater.from(context);
            this.data = data;
            this.checkboxVisible = selection;
            toolbar.setTitle(R.string.title_select_files);
        }

        public void setData(File[] data) {
            this.data = data;
        }

        @Override
        @NonNull
        public FilesListRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.row_item_storage, parent, false);
            return new FilesListRecycler.ViewHolder(view);
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            if (holder.asyncTask != null)
                holder.asyncTask.cancel(true);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull FilesListRecycler.ViewHolder holder, int position) {
            holder.storageName.setText(data[position].getName());
            holder.storagePath.setText(getString(R.string.modified_) + DateTimeUtils.getTimeAgo(data[position].lastModified(), activity));
            holder.checkBox.setVisibility(checkboxVisible && !data[position].isDirectory() ? View.VISIBLE : View.GONE);
            if (selectedFiles.contains(data[position].getAbsolutePath()))
                holder.checkBox.setChecked(true);
            setIcon(holder, data[position]);
        }

        @SuppressWarnings("rawtypes")
        @SuppressLint("StaticFieldLeak")
        private void setIcon(FilesListRecycler.ViewHolder holder, File file) {
            if (file.isDirectory()) {
                holder.storageIcon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_folder_black_24dp));
                return;
            }
            holder.asyncTask = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    try {
                        Document doc = Document.openDocument(file.getAbsolutePath());
                        if (doc.needsPassword()) {
                            activity.runOnUiThread(() -> holder.storageIcon.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_lock_black_24dp)));
                            return null;
                        }
                        Page page = doc.loadPage(0);
                        Bitmap bitmap = AndroidDrawDevice.drawPageFitWidth(page, 80);
                        activity.runOnUiThread(() -> holder.storageIcon.setImageBitmap(bitmap));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return null;
                }
            };
            holder.asyncTask.execute();
        }

        @Override
        public int getItemCount() {
            return data.length;
        }

        public void openSelected() {
            if (selectedFiles.size() > 0) {
                Uri uri = Uri.fromFile(new File(selectedFiles.get(0)));
                MiscUtils.openDoc(uri, activity, selectedFiles);
                FirebaseUtils.analyticsFileOpen(activity,"FILE_OPEN_MANY_BROWSE", uri);
            } else {
                Toast.makeText(activity,R.string.select_at_least_one_file,Toast.LENGTH_LONG).show();
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView storageName, storagePath;
            ImageView storageIcon;
            CheckBox checkBox;
            AsyncTask asyncTask;

            ViewHolder(View itemView) {
                super(itemView);
                storageName = itemView.findViewById(R.id.storage_name);
                storagePath = itemView.findViewById(R.id.storage_path);
                storageIcon = itemView.findViewById(R.id.storage_icon);
                checkBox = itemView.findViewById(R.id.checkbox);
                if (checkboxVisible) {
                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        int position = getAdapterPosition();
                        if (isChecked) {
                            String path = data[position].getAbsolutePath();
                            if (!selectedFiles.contains(path)) {
                                selectedFiles.add(path);
                            }
                        } else {
                            selectedFiles.remove(data[position].getAbsolutePath());
                        }
                        toolbar.setTitle(getString(R.string._selected, selectedFiles.size()));
                    });
                }
                View.OnClickListener listener = v -> {
                    int position = getAdapterPosition();
                    if (data[position].isDirectory()) {
                        currentDir = data[position];
                        refreshFiles();
                    } else {
                        if (checkboxVisible) {
                            checkBox.setChecked(!checkBox.isChecked());
                        } else {
                            Uri uri = Uri.fromFile(data[position]);
                            MiscUtils.openDoc(uri, activity, selectedFiles);
                            FirebaseUtils.analyticsFileOpen(activity,"FILE_OPEN_OPEN_MANY_BROWSE",uri);
                        }
                    }
                };
                itemView.setOnClickListener(listener);
                itemView.setOnLongClickListener(view -> {
                    initEnableSelection(activity.findViewById(R.id.selectMultiple));
                    return true;
                });
            }
        }
    }

}

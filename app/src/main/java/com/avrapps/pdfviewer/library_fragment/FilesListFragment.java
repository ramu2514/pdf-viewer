package com.avrapps.pdfviewer.library_fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.data.LibraryFiles;
import com.avrapps.pdfviewer.utils.DateTimeUtils;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilesListFragment extends Fragment {
    public static final String ARG_OBJECT = "object";

    RecyclerView recyclerView;
    String extension;
    MainActivity activity;
    Toolbar toolbar;
    FilesListRecycler recyclerAdapter;
    private boolean enableSelection;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_object, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            extension = args.getString(ARG_OBJECT);
        }
        activity = (MainActivity) getActivity();
        toolbar = activity.findViewById(R.id.my_toolbar);
        lazyLoadFiles(view);
        addSearchListener(view);
        addMultiSelectListener(view);
    }

    private void addMultiSelectListener(View view) {
        view.findViewById(R.id.selectMultiple).setOnClickListener(v -> {
            if (enableSelection) {
                recyclerAdapter.openSelected();
                ((ImageView) v).setImageResource(R.drawable.ic_multiselect);
            } else {
                recyclerAdapter.enableMultiSelection();
                ((ImageView) v).setImageResource(R.drawable.ic_baseline_open_in_browser_24);
            }
            enableSelection = !enableSelection;
        });
    }

    private void addSearchListener(View view) {
        SearchView searchView = view.findViewById(R.id.fileSearch);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                recyclerAdapter.filterFiles(newText);
                return true;
            }
        });
    }

    AsyncTask asyncTask;

    @SuppressLint("StaticFieldLeak")
    private void lazyLoadFiles(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        ArrayList<File> filesList = new ArrayList<>();
        recyclerAdapter = new FilesListRecycler(activity, filesList, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(recyclerAdapter);
        recyclerAdapter.appendData(LibraryFiles.getFiles(extension));
        ArrayList<StorageUtils.StorageDirectoryParcelable> storages = new StorageUtils(activity).getStorageList();
        asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                for (StorageUtils.StorageDirectoryParcelable storage : storages) {
                    listOfFiles(new File(storage.getPath()), LibraryFiles.getFilePaths(extension));
                }
                return null;
            }
        };
        asyncTask.execute();
    }

    private void listOfFiles(File currentDirectory, List<String> preloadedFiles) {
        ArrayList<File> filesList = new ArrayList<>();
        File[] files = currentDirectory.listFiles();
        if (files != null) {
            Arrays.sort(files, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!file.getAbsolutePath().contains("PDFViewerLite/imports")) {
                        listOfFiles(file, preloadedFiles);
                    }
                } else {
                    if (!preloadedFiles.contains(file.getAbsolutePath())) {
                        String fileName = file.getName();
                        int i = fileName.lastIndexOf('.');
                        if (i > 0) {
                            String fileExt = fileName.substring(i);
                            if (fileExt.length() > 2 && extension.contains(fileExt.toLowerCase())) {
                                filesList.add(file);
                            }
                        }
                    }
                }
            }
            if (filesList.size() > 0) {
                LibraryFiles.insert(filesList);
                activity.runOnUiThread(() -> appendFiles(filesList));
            }
        }
    }

    private void appendFiles(ArrayList<File> filesList) {
        recyclerAdapter.appendData(filesList);
    }


    public class FilesListRecycler extends RecyclerView.Adapter<FilesListRecycler.ViewHolder> {

        private final LayoutInflater mInflater;
        ArrayList<File> data, original;
        Drawable pdfIcon, epubIcon, tifIcon, xpsIcon, cbzIcon, fb2Icon;
        private boolean checkboxVisible;
        private ArrayList<String> selectedFiles = new ArrayList<>();

        FilesListRecycler(Context context, ArrayList<File> data, boolean selection) {
            this.mInflater = LayoutInflater.from(context);
            this.data = data;
            this.checkboxVisible = selection;
            pdfIcon = ContextCompat.getDrawable(activity, R.drawable.pdf);
            tifIcon = ContextCompat.getDrawable(activity, R.drawable.tiff);
            epubIcon = ContextCompat.getDrawable(activity, R.drawable.epub);
            xpsIcon = ContextCompat.getDrawable(activity, R.drawable.xps);
            cbzIcon = ContextCompat.getDrawable(activity, R.drawable.cbz);
            fb2Icon = ContextCompat.getDrawable(activity, R.drawable.fb2);
        }

        public void appendData(ArrayList<File> data) {
            this.data.addAll(data);
            notifyDataSetChanged();
            original = (ArrayList<File>) this.data.clone();
        }

        public void filterFiles(String text) {
            ArrayList<File> temp = new ArrayList<>();
            if (original == null) return;
            for (File d : original) {
                if (d.getName().toLowerCase().contains(text.toLowerCase())) {
                    temp.add(d);
                }
            }
            data = temp;
            notifyDataSetChanged();
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.row_item_storage, parent, false);
            return new FilesListRecycler.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FilesListRecycler.ViewHolder holder, int position) {
            File file = data.get(position);
            holder.storageName.setText(file.getName());
            holder.storagePath.setText(file.getParentFile().getName() + "   .   " + DateTimeUtils.getTimeAgo(file.lastModified()));
            holder.checkBox.setVisibility(checkboxVisible && !file.isDirectory() ? View.VISIBLE : View.GONE);
            if (selectedFiles.contains(file.getAbsolutePath()))
                holder.checkBox.setChecked(true);
            setIcon(holder.storageIcon, file.getName().toLowerCase());
        }

        private void setIcon(ImageView storageIcon, String name) {
            Drawable icon = pdfIcon;
            if (name.contains("tif")) icon = tifIcon;
            else if (name.contains("epub")) icon = epubIcon;
            else if (name.contains("xps")) icon = xpsIcon;
            else if (name.contains("cbz")) icon = cbzIcon;
            else if (name.contains("fb2")) icon = fb2Icon;
            storageIcon.setImageDrawable(icon);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }


        public void openSelected() {
            if (selectedFiles.size() > 0) {
                if (asyncTask != null) asyncTask.cancel(true);
                MiscUtils.openDoc(Uri.fromFile(new File(selectedFiles.get(0))), activity, selectedFiles);
            }
        }

        public void enableMultiSelection() {
            checkboxVisible = true;
            notifyDataSetChanged();
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView storageName, storagePath;
            CheckBox checkBox;
            ImageView storageIcon;

            ViewHolder(View itemView) {
                super(itemView);
                storageName = itemView.findViewById(R.id.storage_name);
                storagePath = itemView.findViewById(R.id.storage_path);
                storageIcon = itemView.findViewById(R.id.storage_icon);
                checkBox = itemView.findViewById(R.id.checkbox);
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    int position = getAdapterPosition();
                    if (isChecked) {
                        String path = data.get(position).getAbsolutePath();
                        if (!selectedFiles.contains(path)) {
                            selectedFiles.add(path);
                        }
                    } else {
                        selectedFiles.remove(data.get(position).getAbsolutePath());
                    }
                    toolbar.setTitle(selectedFiles.size() + " Selected");
                });
                View.OnClickListener listener = v -> {
                    if (asyncTask != null) asyncTask.cancel(true);
                    int position = getAdapterPosition();
                    if (!checkboxVisible) {
                        MiscUtils.openDoc(Uri.fromFile(data.get(position)), activity, selectedFiles);
                    }
                };
                itemView.setOnClickListener(listener);

            }
        }
    }
}

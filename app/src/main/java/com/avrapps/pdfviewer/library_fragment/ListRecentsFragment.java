package com.avrapps.pdfviewer.library_fragment;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.library_fragment.data.LastOpenDocuments;
import com.avrapps.pdfviewer.results_fragment.FirebaseUtils;
import com.avrapps.pdfviewer.utils.DateTimeUtils;
import com.avrapps.pdfviewer.utils.MiscUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ListRecentsFragment extends Fragment {
    public static final String ARG_OBJECT = "object";
    private final String TAG = "DemoObjectFragment";

    RecyclerView recyclerView;
    TextView recentlyOpenedTitle;
    RecentlyOpenedDocumentsAdapter adapter;
    private boolean isRecentsTab = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            isRecentsTab = args.getInt(ARG_OBJECT) == 0;
        }
        recyclerView = view.findViewById(R.id.listView);
        recentlyOpenedTitle = view.findViewById(R.id.recentlyOpenedTitle);
        listRecentDocuments();
    }

    private void listRecentDocuments() {
        List<LastOpenDocuments> lastOpenDocuments;
        if (isRecentsTab) {
            lastOpenDocuments = new LastOpenDocuments().getLastOpenDocuments();
        } else {
            lastOpenDocuments = new LastOpenDocuments().getFavoriteDocuments();
        }
        Iterator<LastOpenDocuments> iter = lastOpenDocuments.iterator();
        while (iter.hasNext()) {
            LastOpenDocuments lastOpenDocument = iter.next();
            if (!new File(lastOpenDocument.getPathToDocument()).exists()) {
                lastOpenDocument.delete();
                iter.remove();
            }
        }
        if (lastOpenDocuments.size() == 0) {
            recentlyOpenedTitle.setText(isRecentsTab ? R.string.no_recent_files : R.string.no_faviorites);
            recentlyOpenedTitle.setVisibility(View.VISIBLE);
            return;
        }
        Log.d("LastOpenDocuments", String.valueOf(lastOpenDocuments));
        MainActivity activity = (MainActivity) getActivity();
        recentlyOpenedTitle.setVisibility(View.GONE);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean isListViewPreferred = preferences.getBoolean("isListViewPreferred", false);
        adapter = new RecentlyOpenedDocumentsAdapter(activity, lastOpenDocuments, isListViewPreferred);
        if (isListViewPreferred) {
            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        } else {
            final int numberOfColumns = getResources().getInteger(R.integer.grid_columns);
            recyclerView.setLayoutManager(new GridLayoutManager(activity, numberOfColumns));
        }
        recyclerView.setAdapter(adapter);
    }

    public void onRefresh() {
        if (adapter != null) {
            try {
                listRecentDocuments();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    //Refer https://stackoverflow.com/a/40587169/12643143 for click interface and code reference
    public class RecentlyOpenedDocumentsAdapter extends RecyclerView.Adapter<RecentlyOpenedDocumentsAdapter.ViewHolder> {

        private final LayoutInflater mInflater;
        boolean multiSelection = false;
        List<LastOpenDocuments> data;
        MainActivity activity;
        ArrayList<Integer> selectedFiles = new ArrayList<>();
        boolean isListViewPreferred;

        RecentlyOpenedDocumentsAdapter(MainActivity context, List<LastOpenDocuments> data, boolean isListViewPreferred) {
            this.mInflater = LayoutInflater.from(context);
            activity = context;
            this.data = data;
            this.isListViewPreferred = isListViewPreferred;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutResource = isListViewPreferred ? R.layout.row_item_list_file : R.layout.row_item_grid_file;
            View view = mInflater.inflate(layoutResource, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LastOpenDocuments lastOpenDocument = data.get(position);
            holder.checkbox.setVisibility(multiSelection ? View.VISIBLE : View.INVISIBLE);
            holder.checkbox.setChecked(false);
            String filename = new File(lastOpenDocument.getPathToDocument()).getName();
            int lastIndex = filename.lastIndexOf('.');
            if (lastIndex >= 0) {
                holder.fileName.setText(filename.substring(0, lastIndex));
                holder.fileExtension.setText(filename.substring(filename.lastIndexOf('.') + 1));
            } else {
                holder.fileName.setText(filename);
            }
            holder.lastOpenDate.setText(DateTimeUtils.getTimeAgo(new Date(lastOpenDocument.getLastOpenTime()).getTime(), activity));
            AsyncTask.execute(() -> {
                try {
                    Document doc = Document.openDocument(lastOpenDocument.getPathToDocument());
                    if (doc.needsPassword()) {
                        activity.runOnUiThread(() -> holder.thumbnail.setImageResource(R.drawable.ic_lock_black_24dp));
                        return;
                    }
                    Page page = doc.loadPage(0);
                    Bitmap bitmap = AndroidDrawDevice.drawPageFitWidth(page, 200);
                    activity.runOnUiThread(() -> holder.thumbnail.setImageBitmap(bitmap));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private void deleteSelected() {
            Collections.sort(selectedFiles, Collections.reverseOrder());
            for (Integer position : selectedFiles) {
                int intPosition = position;
                LastOpenDocuments doc = data.get(intPosition);
                if (doc.getPathToDocument().contains("PDFViewerLite/imports")) {
                    try {
                        new File(doc.getPathToDocument()).delete();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                doc.delete();
                data.remove(intPosition);
            }
            selectedFiles.clear();
        }

        private void openSelected() {
            ArrayList<String> filePaths = new ArrayList<>();
            for (Integer position : selectedFiles) {
                int intPosition = position;
                LastOpenDocuments doc = data.get(intPosition);
                filePaths.add(doc.getPathToDocument());
            }
            if (filePaths.size() > 0) {
                Uri uri = Uri.fromFile(new File(filePaths.get(0)));
                MiscUtils.openDoc(uri, activity, filePaths);
            }
            selectedFiles.clear();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView fileName, lastOpenDate, fileExtension;
            ImageView thumbnail;
            CheckBox checkbox;

            ViewHolder(View itemView) {
                super(itemView);
                checkbox = itemView.findViewById(R.id.checkbox);
                checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    int position = getAdapterPosition();
                    if (isChecked) {
                        selectedFiles.add(position);
                    } else {
                        selectedFiles.remove(Integer.valueOf(position));
                    }
                });
                fileName = itemView.findViewById(R.id.name);
                lastOpenDate = itemView.findViewById(R.id.lastOpenDate);
                fileExtension = itemView.findViewById(R.id.file_extension);
                thumbnail = itemView.findViewById(R.id.doc_thumbnail);
                View.OnClickListener listener = v -> {
                    checkbox.setChecked(!checkbox.isChecked());
                    if (!multiSelection) {
                        int position = getAdapterPosition();
                        Log.e(TAG, "position: " + position);
                        LastOpenDocuments dataModel = data.get(position);
                        File doc = new File(dataModel.getPathToDocument());
                        if (doc.exists()) {
                            Uri uri = Uri.fromFile(doc);
                            MiscUtils.openDoc(uri, activity, new ArrayList<>());
                            FirebaseUtils.analyticsFileOpen(activity, "FILE_OPEN_RECENT", uri);
                        } else {
                            Toast.makeText(getContext(), "Document no longer exist on device", Toast.LENGTH_LONG).show();
                        }
                    }
                };
                itemView.setOnClickListener(listener);
                itemView.setOnLongClickListener(v -> {
                    multiSelection = true;
                    notifyDataSetChanged();
                    activity.findViewById(R.id.fabContainer).setVisibility(View.VISIBLE);
                    activity.findViewById(R.id.fabDelete).setOnClickListener(view -> {
                        deleteSelected();
                        clearSelection();
                    });
                    activity.findViewById(R.id.fabCancel).setOnClickListener(view -> clearSelection());
                    activity.findViewById(R.id.fabOpen).setOnClickListener(view -> {
                        openSelected();
                        clearSelection();
                    });
                    return true;
                });
            }

            private void clearSelection() {
                multiSelection = false;
                activity.findViewById(R.id.fabContainer).setVisibility(View.GONE);
                notifyDataSetChanged();
            }
        }
    }

}

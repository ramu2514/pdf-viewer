package com.avrapps.pdfviewer.library_fragment;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.library_fragment.data.LastOpenDocuments;
import com.avrapps.pdfviewer.utils.DateTimeUtils;
import com.avrapps.pdfviewer.utils.MiscUtils;

import java.io.File;
import java.util.ArrayList;
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
        return inflater.inflate(R.layout.fragment_collection_object, container, false);
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
        adapter = new RecentlyOpenedDocumentsAdapter(activity, lastOpenDocuments);
        recyclerView.setLayoutManager(new GridLayoutManager(activity, 3));
        recyclerView.setAdapter(adapter);
    }

    //Refer https://stackoverflow.com/a/40587169/12643143 for click interface and code reference
    public class RecentlyOpenedDocumentsAdapter extends RecyclerView.Adapter<RecentlyOpenedDocumentsAdapter.ViewHolder> {

        private final LayoutInflater mInflater;
        List<LastOpenDocuments> data;
        MainActivity activity;

        RecentlyOpenedDocumentsAdapter(MainActivity context, List<LastOpenDocuments> data) {
            this.mInflater = LayoutInflater.from(context);
            activity = context;
            this.data = data;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.row_item_list_file, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LastOpenDocuments lastOpenDocument = data.get(position);
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

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView fileName, lastOpenDate, fileExtension;
            ImageView thumbnail;

            ViewHolder(View itemView) {
                super(itemView);
                fileName = itemView.findViewById(R.id.name);
                lastOpenDate = itemView.findViewById(R.id.lastOpenDate);
                fileExtension = itemView.findViewById(R.id.file_extension);
                thumbnail = itemView.findViewById(R.id.doc_thumbnail);
                View.OnClickListener listener = v -> {
                    int position = getAdapterPosition();
                    Log.e(TAG, "position: " + position);
                    LastOpenDocuments dataModel = data.get(position);
                    File doc = new File(dataModel.getPathToDocument());
                    if (doc.exists()) {
                        MiscUtils.openDoc(Uri.fromFile(doc), activity, new ArrayList<>());
                    } else {
                        Toast.makeText(getContext(), "Document no longer exist on device", Toast.LENGTH_LONG).show();
                    }
                };
                itemView.setOnClickListener(listener);
                itemView.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    Log.e(TAG, "position: " + position);
                    AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
                    if (isRecentsTab) {
                        dialog.setTitle(R.string.delete_recent_title);
                        dialog.setMessage(R.string.delete_recent);
                    } else {
                        dialog.setTitle(R.string.delete_faviorie_list);
                        dialog.setMessage(R.string.delete_favorite);
                    }
                    dialog.setPositiveButton(R.string.yes, (dialoginterface, i) -> {
                        data.get(position).delete();
                        data.remove(position);
                        notifyDataSetChanged();
                    });
                    dialog.setNegativeButton(R.string.dismiss, null);
                    dialog.setCancelable(false);
                    dialog.show();
                    return true;
                });
            }
        }
    }

}

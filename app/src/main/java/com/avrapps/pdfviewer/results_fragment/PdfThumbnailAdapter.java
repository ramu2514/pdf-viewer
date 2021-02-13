package com.avrapps.pdfviewer.results_fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.avrapps.pdfviewer.R;

import java.util.ArrayList;

public class PdfThumbnailAdapter extends RecyclerView.Adapter<PdfThumbnailAdapter.ViewHolder> {
    Document pdfDocument;
    Activity activity;
   ArrayList<Integer> pageNumbers= new ArrayList<>();

    public PdfThumbnailAdapter(String filePath, Activity activity) {
        pdfDocument = Document.openDocument(filePath);
        this.activity = activity;
    }

    public ArrayList<Integer> getPageNumbers() {
        return pageNumbers;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_thumb, null);
        return new ViewHolder(itemLayoutView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.txtViewTitle.setText(activity.getString(R.string.page_) + (position + 1));
        AsyncTask.execute(() -> {
           Page page = pdfDocument.loadPage(position);
            Bitmap bitmap = AndroidDrawDevice.drawPageFitWidth(page, 150);
            activity.runOnUiThread(() -> viewHolder.imgViewIcon.setImageBitmap(bitmap));
        });
        viewHolder.view.setOnClickListener((v)->{
            if (pageNumbers.contains(position+1)) {
                pageNumbers.remove(Integer.valueOf(position+1));
            } else {
                pageNumbers.add(position+1);
            }
            viewHolder.checkButton.setVisibility(pageNumbers.contains(position+1)?View.VISIBLE:View.GONE);
        });
    }

    @Override
    public int getItemCount() {
        return pdfDocument.countPages();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtViewTitle;
        public ImageView imgViewIcon;
        public ImageView checkButton;
        public View view;

        public ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            view = itemLayoutView.findViewById(R.id.thumb_view);
            checkButton = itemLayoutView.findViewById(R.id.img_camera_center);
            txtViewTitle = itemLayoutView.findViewById(R.id.item_title);
            imgViewIcon = itemLayoutView.findViewById(R.id.item_icon);
        }
    }
}

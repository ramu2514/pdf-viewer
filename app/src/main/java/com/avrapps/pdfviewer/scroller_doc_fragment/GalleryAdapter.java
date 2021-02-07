package com.avrapps.pdfviewer.scroller_doc_fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.avrapps.pdfviewer.R;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.MyViewHolder> {

    private final Document document;
    private final Context mContext;
    private final int widthPixels;
    RecyclerView recyclerView;
    String search;

    public GalleryAdapter(Context context, Document document, int widthPixels, RecyclerView recyclerView) {
        mContext = context;
        this.document = document;
        this.widthPixels = widthPixels;
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Page page = document.loadPage(position);
        Bitmap bitmap = AndroidDrawDevice.drawPageFitWidth(page, widthPixels);
        holder.thumbnail.setImageBitmap(bitmap);
        //Glide.with(mContext).load(bitmap).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.thumbnail);
    }

    @Override
    public int getItemCount() {
        return document.countPages();
    }

    public void setSearch(String s) {
        search = s;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;

        MyViewHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.image_pdf);
        }

    }
}

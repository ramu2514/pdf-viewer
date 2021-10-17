package com.avrapps.pdfviewer.viewer_fragment.models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Item implements Serializable {
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

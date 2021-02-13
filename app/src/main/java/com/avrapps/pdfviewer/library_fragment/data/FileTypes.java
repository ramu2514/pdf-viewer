package com.avrapps.pdfviewer.library_fragment.data;

public enum FileTypes {

    PDF(1),
    TIFF(2),
    EPUB(3),
    XPS(4),
    CBZ(5),
    FB2(6);
    public final int type;

    FileTypes(int i) {
        this.type = i;
    }

}

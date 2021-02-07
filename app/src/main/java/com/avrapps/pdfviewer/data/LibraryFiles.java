package com.avrapps.pdfviewer.data;

import android.util.Log;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.orm.util.NamingHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LibraryFiles extends SugarRecord {

    @Unique
    private String pathToDocument;

    private int type;

    public LibraryFiles(String absolutePath, int pdf) {
        this.pathToDocument = absolutePath;
        this.type = pdf;
    }

    public LibraryFiles() {
    }

    public static void insert(ArrayList<File> filesList) {
        for (File file : filesList) {
            String fileName = file.getName();
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                String fileExt = fileName.substring(i);
                if (fileExt.length() > 2) {
                    fileExt = fileExt.toLowerCase();
                    Log.e("DEBUGGER", file.getAbsolutePath() + getType(fileExt));
                    new LibraryFiles(file.getAbsolutePath(), getType(fileExt)).save();
                }
            }
        }
    }

    public static ArrayList<File> getFiles(String extension) {
        List<LibraryFiles> libraryFiles = Select.from(LibraryFiles.class)
                .where(Condition.prop(NamingHelper.toSQLNameDefault("type")).eq(getType(extension)))
                .list();
        ArrayList<File> files = new ArrayList<>();
        for (LibraryFiles file : libraryFiles) {
            files.add(new File(file.pathToDocument));
        }
        return files;
    }

    public static List<String> getFilePaths(String extension) {
        List<LibraryFiles> libraryFiles = Select.from(LibraryFiles.class)
                .where(Condition.prop(NamingHelper.toSQLNameDefault("type")).eq(getType(extension)))
                .list();
        List<String> files = new ArrayList<>();
        for (LibraryFiles file : libraryFiles) {
            files.add(file.pathToDocument);
        }
        return files;
    }

    private static int getType(String extension) {
        int type = -1;
        switch (extension) {
            case ".pdf":
                type = FileTypes.PDF.type;
                break;
            case ".epub":
                type = FileTypes.EPUB.type;
                break;
            case ".tif":
            case ".tiff":
                type = FileTypes.TIFF.type;
                break;
            case ".xps":
            case ".oxps":
                type = FileTypes.XPS.type;
                break;
            case ".cbz":
                type = FileTypes.CBZ.type;
                break;
            case ".fb2":
                type = FileTypes.FB2.type;
                break;
        }
        return type;
    }
}

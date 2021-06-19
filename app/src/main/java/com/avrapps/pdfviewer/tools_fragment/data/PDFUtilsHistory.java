package com.avrapps.pdfviewer.tools_fragment.data;

import com.orm.SugarRecord;

public class PDFUtilsHistory extends SugarRecord {
    private String pdfName;
    private String sourceFile;
    private String destinationFile;
    private int operation;
    private long updatedDate;

    public PDFUtilsHistory(){
    }
    public PDFUtilsHistory(String pdfName, String sourceFile, String destinationFile, int operation, long updatedDate){
        this.pdfName = pdfName;
        this.sourceFile = sourceFile;
        this.destinationFile = destinationFile;
        this.operation = operation;
        this.updatedDate=updatedDate;
    }

    public String getPdfName() {
        return pdfName;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getDestinationFile() {
        return destinationFile;
    }

    public int getOperation() {
        return operation;
    }

    public long getUpdatedDate() {
        return updatedDate;
    }

}
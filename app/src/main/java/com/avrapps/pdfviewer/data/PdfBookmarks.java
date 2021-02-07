package com.avrapps.pdfviewer.data;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.orm.util.NamingHelper;

import java.util.List;

public class PdfBookmarks extends SugarRecord {

    private int page;
    private String pathToDocument;
    @Unique
    private String uniqueKey;

    public PdfBookmarks() {
    }

    public PdfBookmarks(String filename, int page) {
        this.page = page;
        this.pathToDocument = filename;
        uniqueKey = filename + "@#@" + page;
    }

    public static List<PdfBookmarks> getBookmarks(String pathToDocument) {
        return Select.from(PdfBookmarks.class)
                .where(Condition.prop(NamingHelper.toSQLNameDefault("pathToDocument"))
                        .eq(pathToDocument))
                .orderBy("page asc")
                .list();
    }

    public static boolean isBookmarkedAlready(String filePath, int currentPage) {
        return Select.from(PdfBookmarks.class)
                .where(Condition.prop(NamingHelper.toSQLNameDefault("pathToDocument"))
                        .eq(filePath))
                .where(Condition.prop("page").eq(currentPage))
                .list().size() > 0;
    }

    public static void removeBookmark(String filePath, int currentPage) {
        List<PdfBookmarks> bookmarks = Select.from(PdfBookmarks.class)
                .where(Condition.prop(NamingHelper.toSQLNameDefault("pathToDocument"))
                        .eq(filePath))
                .where(Condition.prop("page").eq(currentPage))
                .list();
        for (PdfBookmarks bookmark : bookmarks) {
            bookmark.delete();
        }
    }

    public long getPage() {
        return page;
    }

    public String toString() {
        return "Page " + page;
    }
}

package com.avrapps.pdfviewer.library_fragment.data;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.orm.util.NamingHelper;

import java.io.File;
import java.util.Date;

public class LastOpenDocuments extends SugarRecord {

    private long lastOpenTime;
    @Unique
    private String pathToDocument;

    @Unique
    private String uniqueIdentifier;

    private boolean isFavorite;

    public LastOpenDocuments() {
    }

    public LastOpenDocuments(String pathToDocument, long lastOpenTime, boolean isFavorite) {
        this.lastOpenTime = lastOpenTime;
        this.pathToDocument = pathToDocument;
        this.isFavorite = isFavorite;
        File file = new File(pathToDocument);
        uniqueIdentifier = file.getName() + file.length();
    }

    public static boolean isFavorite(String filePath) {
        return Select.from(LastOpenDocuments.class)
                .where(Condition.prop(NamingHelper.toSQLNameDefault("pathToDocument")).eq(filePath))
                .where(Condition.prop(NamingHelper.toSQLNameDefault("isFavorite")).eq("1"))
                .list()
                .size() > 0;

    }

    public static void removeFromFavorites(String filePath) {
        new LastOpenDocuments(filePath, new Date().getTime(), false).save();
    }

    public static void addToFavorites(String filePath) {
        new LastOpenDocuments(filePath, new Date().getTime(), true).save();
    }

    public java.util.List<LastOpenDocuments> getLastOpenDocuments() {
        return Select.from(LastOpenDocuments.class)
                .orderBy(NamingHelper.toSQLNameDefault("lastOpenTime") + " desc")
                .limit("40")
                .list();
    }

    public long getLastOpenTime() {
        return lastOpenTime;
    }

    public String getPathToDocument() {
        return pathToDocument;
    }

    public java.util.List<LastOpenDocuments> getFavoriteDocuments() {
        return Select.from(LastOpenDocuments.class)
                .where(Condition.prop(NamingHelper.toSQLNameDefault("isFavorite")).eq("1"))
                .orderBy(NamingHelper.toSQLNameDefault("lastOpenTime") + " desc")
                .limit("10")
                .list();
    }

    @Override
    public String toString() {
        return "LastOpenDocuments{" +
                "lastOpenTime=" + lastOpenTime +
                ", pathToDocument='" + pathToDocument + '\'' +
                ", uniqueIdentifier='" + uniqueIdentifier + '\'' +
                ", isFavorite=" + isFavorite +
                '}';
    }
}

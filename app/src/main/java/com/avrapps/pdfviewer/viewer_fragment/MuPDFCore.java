package com.avrapps.pdfviewer.viewer_fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PDFDocument;
import com.artifex.mupdf.fitz.PDFPage;
import com.artifex.mupdf.fitz.PDFWidget;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.RectI;
import com.artifex.mupdf.fitz.StructuredText;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.avrapps.pdfviewer.BuildConfig;
import com.avrapps.pdfviewer.CallbackInterface;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.library_fragment.data.LastOpenDocuments;
import com.avrapps.pdfviewer.library_fragment.data.PdfBookmarks;
import com.avrapps.pdfviewer.settings_fragment.constants.AppConstants;
import com.avrapps.pdfviewer.utils.MessagingUtility;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.PreferenceUtil;
import com.avrapps.pdfviewer.viewer_fragment.models.Item;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MuPDFCore {
    private final int resolution;
    String filePath;
    DocumentFile pickedDir = null;
    private Document doc;
    private Outline[] outline;
    private int pageCount = -1;
    private int currentPage;
    private Page page;
    private float pageWidth;
    private float pageHeight;
    private DisplayList displayList;
    /* Default to "A Format" pocket book size. */
    private int layoutW = 760;
    private int layoutH = 1024;
    private int layoutEM = 5;
    private boolean isEditable = true;

    public MuPDFCore(String filename) {
        doc = Document.openDocument(filename);
        filePath = filename;
        doc.layout(layoutW, layoutH, layoutEM);
        pageCount = doc.countPages();
        resolution = 160;
        currentPage = -1;
    }

    public boolean isPDF() {
        return doc.isPDF();
    }

    public String getTitle() {
        int lastSlashPos = filePath.lastIndexOf('/');
        String mFileName = lastSlashPos == -1
                ? filePath
                : filePath.substring(lastSlashPos + 1);
        String title = doc.getMetaData(Document.META_INFO_TITLE);
        return (title != null && !title.isEmpty()) ? title : mFileName;
    }

    public Map<Integer, String> getMetadataMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(R.string.title, getTitle());
        map.put(R.string.author, doc.getMetaData(Document.META_INFO_AUTHOR));
        map.put(R.string.encryption, doc.getMetaData(Document.META_ENCRYPTION));
        map.put(R.string.formt, doc.getMetaData(Document.META_FORMAT));
        map.put(R.string.page_count, pageCount + "");
        map.put(R.string.file_Size, readableFileSize(new File(filePath).length()));
        return map;
    }

    private String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public int countPages() {
        return pageCount;
    }

    public synchronized StructuredText getText() {
        return page.toStructuredText();
    }

    public synchronized boolean isReflowable() {
        return doc.isReflowable();
    }

    public synchronized int layout(int oldPage, int w, int h, int em) {
        if (w != layoutW || h != layoutH || em != layoutEM) {
            System.out.println("LAYOUT: " + w + "," + h);
            layoutW = w * 2;
            layoutH = h * 2;
            layoutEM = em;
            long mark = doc.makeBookmark(doc.locationFromPageNumber(oldPage));
            doc.layout(layoutW, layoutH, layoutEM);
            currentPage = -1;
            pageCount = doc.countPages();
            outline = null;
            try {
                outline = doc.loadOutline();
            } catch (Exception ex) {
                /* ignore error */
            }
            return doc.pageNumberFromLocation(doc.findBookmark(mark));
        }
        return oldPage;
    }

    public synchronized void gotoPage(int pageNum) {
        /* TODO: page cache */
        if (pageNum > pageCount - 1)
            pageNum = pageCount - 1;
        else if (pageNum < 0)
            pageNum = 0;
        if (pageNum != currentPage) {
            currentPage = pageNum;
            if (page != null)
                page.destroy();
            page = null;
            if (displayList != null)
                displayList.destroy();
            displayList = null;
            if (doc == null) return;
            page = doc.loadPage(pageNum);
            Rect b = page.getBounds();
            pageWidth = b.x1 - b.x0;
            pageHeight = b.y1 - b.y0;
        }
    }

    public PDFWidget[] getWidgets(int page) {
        if (doc.isPDF()) {
            return ((PDFPage) doc.loadPage(page)).getWidgets();
        }
        return new PDFWidget[0];
    }

    public void clickWidget(PDFWidget widget) {
        Rect b = widget.getBounds();
        ((PDFPage) page).activateWidgetAt(b.x0 + (b.x1 - b.x0) / 2, b.y0 + (b.y1 - b.y0) / 2);
    }

    public PDFAnnotation[] getAnnotations(int page) {
        if (doc.isPDF()) {
            return ((PDFPage) doc.loadPage(page)).getAnnotations();
        }
        return new PDFAnnotation[0];
    }

    public synchronized void createAnnotation(Activity activity, int type, Quad[] qp, String content, CallbackInterface cb) {
        if (doc.isPDF()) {
            if (!isEditable) {
                Toast.makeText(activity, R.string.pdf_not_editable, Toast.LENGTH_LONG).show();
                return;
            }
            PDFAnnotation pdfAnnotation = ((PDFPage) page).createAnnotation(type);
            pdfAnnotation.setModificationDate(new Date());
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
            String author = sp.getString("prefAuthor", activity.getString(R.string.app_name));
            pdfAnnotation.setAuthor(author);
            pdfAnnotation.setQuadPoints(qp);
            if (content != null) {
                pdfAnnotation.setContents(content);
            }
            float[] color;//R+G=Yellow R-1,G-1,B-0
            switch (type) {
                case PDFAnnotation.TYPE_UNDERLINE:
                    color = new float[]{1.0f, 0.0f, 0.0f};
                    pdfAnnotation.setColor(color);
                    break;
                case PDFAnnotation.TYPE_HIGHLIGHT:
                    color = new float[]{1.0f, 1.0f, 0.0f};
                    pdfAnnotation.setColor(color);
                    break;
                case PDFAnnotation.TYPE_STRIKE_OUT:
                    color = new float[]{0.0f, 0.0f, 0.0f};
                    pdfAnnotation.setColor(color);
                    break;
                case PDFAnnotation.TYPE_TEXT:
                    pdfAnnotation.setIcon(getURLForResource(R.drawable.ic_baseline_comment_24));
                    break;
                default:
                    Log.e("PDFViewer", activity.getString(R.string.annotation_not_supported));
                    return;
            }
            savePage();
            saveDocument(activity, cb);
        }
    }

    private String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + resourceId).toString();
    }

    void savePage() {
        ((PDFPage) page).applyRedactions();
        ((PDFPage) page).update();
    }

    public void saveDocument(Activity context, CallbackInterface cb) {
        Toast.makeText(context, "Changes might not be applicable until restart", Toast.LENGTH_LONG).show();
        if (!isEditable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MessagingUtility.askSafPermission(context);
            return;
        }
        AsyncTask.execute(() -> {
            File tempFile = new File(filePath + ".temp.pdf");
            try {
                ((PDFDocument) doc).save(tempFile.getAbsolutePath(), "");
                tempFile.renameTo(new File(filePath));
            } catch (Exception ex) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        boolean isRemovable = PreferenceUtil.preferenceStringSetContains(context,
                                AppConstants.REMOVABLE_STORAGES, filePath);
                        if (isRemovable) {
                            tempFile = new File(context.getFilesDir(), "temp.pdf");
                            ((PDFDocument) doc).save(tempFile.getAbsolutePath(), "");
                            MiscUtils.copyFileUsingStorageFramework(context, pickedDir, tempFile.getAbsolutePath(), filePath);
                        }
                    } catch (Exception ex1) {
                        MiscUtils.sendBugEmail(context, ex1, ex);
                    }
                }
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                context.runOnUiThread(() ->cb.onButtonClick(null));
            }
        });
    }

    public void addComment(PDFAnnotation annotation, String replyComment, Context context) {
        annotation.setContents(replyComment);
        annotation.setModificationDate(new Date());
        savePage();
    }

    public void setAnnotationColor(PDFAnnotation pdfAnnotation, float[] color) {
        pdfAnnotation.setColor(color);
        pdfAnnotation.setModificationDate(new Date());
        savePage();
    }

    public void deleteAnnotation(PDFAnnotation pdfAnnotation) {
        ((PDFPage) page).deleteAnnotation(pdfAnnotation);
        savePage();
    }

    public synchronized PointF getPageSize(int pageNum) {
        gotoPage(pageNum);
        return new PointF(pageWidth, pageHeight);
    }

    public synchronized void onDestroy() {
        if (displayList != null)
            displayList.destroy();
        displayList = null;
        if (page != null)
            page.destroy();
        page = null;
        if (doc != null)
            doc.destroy();
        doc = null;
    }

    public synchronized void drawPage(Bitmap bm, int pageNum,
                                      int pageW, int pageH,
                                      int patchX, int patchY,
                                      int patchW, int patchH,
                                      Cookie cookie) {
        gotoPage(pageNum);

        if (page == null) return;
        if (displayList == null)
            displayList = page.toDisplayList();

        float zoom = resolution / 72;
        Matrix ctm = new Matrix(zoom, zoom);
        RectI bbox = new RectI(page.getBounds().transform(ctm));
        float xscale = (float) pageW / (float) (bbox.x1 - bbox.x0);
        float yscale = (float) pageH / (float) (bbox.y1 - bbox.y0);
        ctm.scale(xscale, yscale);

        AndroidDrawDevice dev = new AndroidDrawDevice(bm, patchX, patchY);
        displayList.run(dev, ctm, cookie);
        if (isPDF()) {
            page.runPageAnnots(dev, ctm, cookie);
        }
        dev.close();
        dev.destroy();
    }

    public synchronized void updatePage(Bitmap bm, int pageNum,
                                        int pageW, int pageH,
                                        int patchX, int patchY,
                                        int patchW, int patchH,
                                        Cookie cookie) {
        drawPage(bm, pageNum, pageW, pageH, patchX, patchY, patchW, patchH, cookie);
    }

    public synchronized Link[] getPageLinks(int pageNum) {
        if (page == null) return new Link[0];
        gotoPage(pageNum);
        return page.getLinks();
    }

    public synchronized int resolveLink(Link link) {
        return doc.pageNumberFromLocation(doc.resolveLink(link));
    }

    public synchronized Quad[] searchPage(int pageNum, String text) {
        gotoPage(pageNum);
        return page.search(text);
    }

    public synchronized boolean hasOutline() {
        if (outline == null) {
            try {
                outline = doc.loadOutline();
            } catch (Exception ex) {
                /* ignore error */
            }
        }
        return outline != null;
    }

    private void flattenOutlineNodes(ArrayList<Item> result, Outline[] list, String indent) {
        if (list == null) {
            return;
        }
        for (Outline node : list) {
            if (node.title != null) {
                int page = doc.pageNumberFromLocation(doc.resolveLink(node));
                result.add(new Item(indent + node.title, page));
            }
            if (node.down != null)
                flattenOutlineNodes(result, node.down, indent + "    ");
        }
    }

    public synchronized ArrayList<Item> getOutline() {
        ArrayList<Item> result = new ArrayList<Item>();
        flattenOutlineNodes(result, outline, "");
        return result;
    }

    public synchronized boolean needsPassword() {
        return doc.needsPassword();
    }

    public synchronized boolean authenticatePassword(String password) {
        return doc.authenticatePassword(password);
    }

    public void addBookmark(int page) {
        PdfBookmarks pdfBookmarks = new PdfBookmarks(filePath, page);
        pdfBookmarks.save();
    }

    public List<PdfBookmarks> getBookmarks() {
        return PdfBookmarks.getBookmarks(filePath);
    }

    public boolean hasBookmarks() {
        return PdfBookmarks.getBookmarks(filePath).size() > 0;
    }

    public boolean isBookmarkedAlready(int page) {
        return PdfBookmarks.isBookmarkedAlready(filePath, page);
    }

    public void removeBookmark(int page) {
        PdfBookmarks.removeBookmark(filePath, page);
    }

    public boolean isFavorite() {
        return LastOpenDocuments.isFavorite(filePath);
    }

    public void removeFromFavorites() {
        LastOpenDocuments.removeFromFavorites(filePath);
    }

    public void addToFavorites() {
        LastOpenDocuments.addToFavorites(filePath);
    }

    public void setPickedDir(DocumentFile pickedDir) {
        this.pickedDir = pickedDir;
        isEditable = pickedDir != null;
    }

    public void setInternalFile() {
        isEditable = true;
    }

    public boolean isImage() {
        return "Image".equals(doc.getMetaData(Document.META_FORMAT));
    }
}

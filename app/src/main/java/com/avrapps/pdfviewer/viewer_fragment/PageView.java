package com.avrapps.pdfviewer.viewer_fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PDFWidget;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.StructuredText;
import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.settings_fragment.constants.AppConstants;
import com.avrapps.pdfviewer.utils.LogUtils;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.SharingUtils;

import java.util.ArrayList;
import java.util.List;

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends androidx.appcompat.widget.AppCompatImageView {

    public OpaqueImageView(Context context) {
        super(context);
    }

    @Override
    public boolean isOpaque() {
        return true;
    }
}

public class PageView extends ViewGroup {
    public static final String LOG = "PageView";
    private static final int HIGHLIGHT_COLOR = 0x80cc6600;
    private static final int LINK_COLOR = 0xFF0066cc;
    private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
    private static final int PROGRESS_DIALOG_DELAY = 200;
    protected final Context mContext;
    private final MuPDFCore mCore;
    private final Point mParentSize;
    private final Matrix mEntireMat;
    private final Handler mHandler = new Handler();
    protected int mPageNumber;
    protected Point mSize;   // Size of page at minimum zoom
    protected float mSourceScale;
    protected Link[] mLinks;
    protected Quad[] wordsSelected;
    private ImageView mEntire; // Image rendered at minimum zoom
    private Bitmap mEntireBm;
    private AsyncTask<Void, Void, Link[]> mGetLinkInfo;
    private CancellableAsyncTask<Void, Void> mDrawEntire;
    private Point mPatchViewSize; // View size on the basis of which the patch was created
    private Rect mPatchArea;
    private ImageView mPatch;
    private int mTheme = 0;
    private Bitmap mPatchBm;
    private CancellableAsyncTask<Void, Void> mDrawPatch;
    private Quad[] mSearchBoxes;
    private View mSearchView;
    private boolean mIsBlank;
    private boolean mHighlightLinks;
    private ProgressBar mBusyIndicator;
    boolean darkMode;

    public PageView(Context c, MuPDFCore core, Point parentSize, Bitmap sharedHqBm) {
        super(c);
        mContext = c;
        mCore = core;
        mParentSize = parentSize;
        setBackgroundColor(BACKGROUND_COLOR);
        mEntireBm = Bitmap.createBitmap(parentSize.x, parentSize.y, Config.ARGB_8888);
        mPatchBm = sharedHqBm;
        mEntireMat = new Matrix();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        darkMode = sp.getBoolean(AppConstants.DARK_THEME, false);
        int pageTheme = sp.getInt(AppConstants.PAGE_THEME, 0);
        mTheme = AppConstants.PAGE_THEME_IDS.get(pageTheme);
    }

    private void reinit() {
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancel();
            mDrawEntire = null;
        }

        if (mDrawPatch != null) {
            mDrawPatch.cancel();
            mDrawPatch = null;
        }

        if (mGetLinkInfo != null) {
            mGetLinkInfo.cancel(true);
            mGetLinkInfo = null;
        }

        mIsBlank = true;
        mPageNumber = 0;

        if (mSize == null)
            mSize = mParentSize;

        if (mEntire != null) {
            mEntire.setImageBitmap(null);
            mEntire.invalidate();
        }

        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }

        mPatchViewSize = null;
        mPatchArea = null;

        mSearchBoxes = null;
        mLinks = null;
    }

    public void releaseResources() {
        reinit();

        if (mBusyIndicator != null) {
            removeView(mBusyIndicator);
            mBusyIndicator = null;
        }
    }

    public static final int cursorWidth = 50;
    String textSelected = "";
    int startCursorPosX = -1, startCursorPosY = -1;
    PopupWindow startCursor;
    int endCursorPosX = -1, endCursorPosY = -1;
    PopupWindow endCursor;
    boolean isSelectionPopupVisible = false;
    PopupWindow selectionPopup;
    View popupView;
    boolean isPDF = true;
    int popupHeight = 200;
    OnSelectionChangeListener listener;

    public void annotate(int type, String content) {
        mCore.createAnnotation((MainActivity) getContext(), type, wordsSelected, content, returnValue -> {
            invalidate();
            update();
        });
    }

    public void selectText(MotionEvent e1) {
        float x0 = e1.getX(), y0 = e1.getY(), x1 = e1.getX(), y1 = e1.getY();
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        selectText(x0, y0, x1, y1);
        if (wordsSelected.length == 0) {
            Toast.makeText(getContext(), R.string.no_text_selected, Toast.LENGTH_LONG).show();
            return;
        }
        int lineHeight = (int) (wordsSelected[0].ll_x - wordsSelected[0].ul_x) + cursorWidth;
        initPopup();
        showPopUpWindow((int) x1 + lineHeight, (int) y1);
        manageCursors(wordsSelected[0].ll_x * scale + getLeft(),
                wordsSelected[0].ll_y * scale + getTop() + e1.getRawY() - e1.getY(),
                wordsSelected[wordsSelected.length - 1].lr_x * scale + getLeft(),
                wordsSelected[wordsSelected.length - 1].ll_y * scale + getTop() + e1.getRawY() - e1.getY(),
                lineHeight,
                e1.getRawX() - e1.getX(),
                e1.getRawY() - e1.getY()
        );
    }

    void selectText(float x0, float y0, float x1, float y1) {
        Log.d(LOG, "selectText invoked");
        Log.d(getLeft() + "", getTop() + "");
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX0 = (x0 - getLeft()) / scale;
        float docRelY0 = (y0 - getTop()) / scale;
        float docRelX1 = (x1 - getLeft()) / scale;
        float docRelY1 = (y1 - getTop()) / scale;

        mSearchView.invalidate();

        StructuredText result = mCore.getText();
        com.artifex.mupdf.fitz.Point a = new com.artifex.mupdf.fitz.Point(docRelX0, docRelY0);
        com.artifex.mupdf.fitz.Point b = new com.artifex.mupdf.fitz.Point(docRelX1, docRelY1);
        Log.d(LOG, "Point A:" + a.toString() + "\tPoint B:" + b.toString());
        result.snapSelection(a, b, StructuredText.SELECT_WORDS);
        wordsSelected = result.highlight(a, b);
        setSearchBoxes(wordsSelected);
        textSelected = result.copy(a, b);
        if (textSelected.isEmpty()) {
            Toast.makeText(mContext, R.string.nothing_selected, Toast.LENGTH_LONG).show();
            exitPopUp();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void manageCursors(float x1, float y1, float x2, float y2, int lineHeight, float xAdjustment, float yAdjustment) {
        if (startCursorPosX < 0) {
            ImageView imageView = new ImageView(mContext);
            imageView.setImageResource(R.drawable.ic_cursor_left);
            imageView.setMaxWidth(cursorWidth);
            startCursorPosX = (int) x1 - cursorWidth;
            startCursorPosY = (int) y1;
            startCursor = new PopupWindow(imageView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            imageView.setOnTouchListener(new OnTouchListener() {
                private int dx = 0;
                private int dy = 0;

                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isSelectionPopupVisible = false;
                            selectionPopup.dismiss();
                            dx = (int) (startCursorPosX - motionEvent.getRawX());
                            dy = (int) (startCursorPosY - motionEvent.getRawY());
                            break;
                        case MotionEvent.ACTION_MOVE:
                            startCursorPosX = (int) (motionEvent.getRawX() + dx);
                            startCursorPosY = (int) (motionEvent.getRawY() + dy);
                            if (startCursorPosY > endCursorPosY) {
                                startCursorPosY = (int) endCursorPosY;
                            }
                            if (endCursorPosY == startCursorPosY && startCursorPosX > endCursorPosX) {
                                startCursorPosX = (int) endCursorPosX;
                            }
                            startCursor.update(startCursorPosX, startCursorPosY, -1, -1);
                            selectText(startCursorPosX - xAdjustment + cursorWidth, startCursorPosY - yAdjustment, endCursorPosX - xAdjustment, endCursorPosY - yAdjustment);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            //startCursor.update((int) (wordsSelected[0].ll_x*scale+getLeft()), (int)(wordsSelected[0].ll_y*scale+getTop()), -1, -1);
                            selectionPopup.showAtLocation(PageView.this, Gravity.TOP | Gravity.START, startCursorPosX, startCursorPosY - lineHeight - popupHeight);
                            isSelectionPopupVisible = true;
                            break;
                    }
                    return true;
                }
            });
        }
        startCursor.showAtLocation(this, Gravity.TOP | Gravity.START, startCursorPosX, startCursorPosY);

        if (endCursorPosX < 0) {
            ImageView imageView = new ImageView(mContext);
            imageView.setImageResource(R.drawable.ic_cursor_right);
            imageView.setMaxWidth(cursorWidth);
            endCursorPosX = (int) x2;
            endCursorPosY = (int) y2;
            endCursor = new PopupWindow(imageView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            imageView.setOnTouchListener(new OnTouchListener() {
                private int dx = 0;
                private int dy = 0;

                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isSelectionPopupVisible = false;
                            selectionPopup.dismiss();
                            dx = (int) (endCursorPosX - motionEvent.getRawX());
                            dy = (int) (endCursorPosY - motionEvent.getRawY());
                            break;
                        case MotionEvent.ACTION_MOVE:
                            endCursorPosX = (int) (motionEvent.getRawX() + dx);
                            endCursorPosY = (int) (motionEvent.getRawY() + dy);
                            if (endCursorPosY < startCursorPosY) {
                                endCursorPosY = (int) startCursorPosY;
                            }
                            if (endCursorPosY == startCursorPosY && endCursorPosX < startCursorPosX) {
                                endCursorPosX = (int) startCursorPosX;
                            }
                            endCursor.update(endCursorPosX, endCursorPosY, -1, -1);
                            selectText(startCursorPosX - xAdjustment + cursorWidth, startCursorPosY - yAdjustment, endCursorPosX - xAdjustment, endCursorPosY - yAdjustment);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            selectionPopup.showAtLocation(PageView.this, Gravity.TOP | Gravity.START, startCursorPosX, startCursorPosY - lineHeight - popupHeight);
                            isSelectionPopupVisible = true;
                            //endCursor.update((int) (wordsSelected[wordsSelected.length-1].lr_x*scale+getLeft()), (int)(wordsSelected[wordsSelected.length-1].lr_y*scale+getTop()), -1, -1);
                            break;
                    }
                    return true;
                }
            });
        }
        endCursor.showAtLocation(this, Gravity.TOP | Gravity.START, endCursorPosX, endCursorPosY);
    }

    void initPopup() {
        resetPopUps();
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupView = layoutInflater.inflate(R.layout.text_select_popup, null);
    }

    void showPopUpWindow(int x, int y) {
        if (selectionPopup != null) {
            selectionPopup.dismiss();
        } else {
            selectionPopup = new PopupWindow(popupView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            popupView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            popupHeight = popupView.getMeasuredHeight();
            selectionPopup.setOnDismissListener(() -> {
                if (isSelectionPopupVisible) {
                    if (selectionPopup != null) selectionPopup.dismiss();
                    if (startCursor != null) startCursor.dismiss();
                    if (endCursor != null) endCursor.dismiss();
                    exitPopUp();
                }
                isSelectionPopupVisible = false;
            });
            //selectionPopup.setBackgroundDrawable(new BitmapDrawable());
            //selectionPopup.setOutsideTouchable(true);
            //selectionPopup.setFocusable(true);
        }
        Integer[] views = new Integer[]{R.id.copyText, R.id.shareText, R.id.translateText,
                R.id.exitPopup, R.id.highlight, R.id.underline, R.id.strike};
        Integer[] hidable_views = new Integer[]{R.id.copyText, R.id.shareText, R.id.translateText,
                R.id.exitPopup, R.id.highlight, R.id.underline, R.id.strike};
        for (int view : views) {
            popupView.findViewById(view).setOnClickListener(this::action);
        }
        if (!isPDF) {
            for (int view : hidable_views) {
                popupView.findViewById(view).setVisibility(GONE);
            }
        }
        selectionPopup.showAtLocation(this, Gravity.TOP | Gravity.START, x, y - cursorWidth);
        isSelectionPopupVisible = true;
    }

    private void action(View view) {
        switch (view.getId()) {
            case R.id.translateText:
                String url = "https://translate.google.com/#view=home&op=translate&sl=auto&tl=en&text=" + textSelected;
                SharingUtils.openUrl(getContext(), url, R.string.error_no_browser);
                break;
            case R.id.copyText:
                MiscUtils.copyTextToClipBoard(getContext(), textSelected);
                break;
            case R.id.highlight:
                annotate(PDFAnnotation.TYPE_HIGHLIGHT, null);
                break;
            case R.id.underline:
                annotate(PDFAnnotation.TYPE_UNDERLINE, null);
                break;
            case R.id.strike:
                annotate(PDFAnnotation.TYPE_STRIKE_OUT, null);
                break;
            case R.id.comment:
                Context context = getContext();
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle(R.string.comment_title);
                alert.setMessage(R.string.comment_description);
                EditText input = new EditText(context);
                alert.setView(input);
                alert.setPositiveButton(R.string.add_note, (dialog, whichButton) -> annotate(PDFAnnotation.TYPE_TEXT, null));
                alert.show();
                break;
            case R.id.shareText:
                SharingUtils.shareText(getContext(), textSelected);
                break;
        }
        exitPopUp();
    }

    public void exitPopUp() {
        setSearchBoxes(null);
        resetPopUps();
        if (listener != null) {
            listener.onSelectionDismiss();
        }
    }

    private void resetPopUps() {
        if (startCursor != null) startCursor.dismiss();
        if (endCursor != null) endCursor.dismiss();
        isSelectionPopupVisible = false;
        if (selectionPopup != null) selectionPopup.dismiss();
        startCursorPosX = -1;
        startCursorPosY = -1;
        endCursorPosX = -1;
        endCursorPosY = -1;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.listener = listener;
    }

    public interface OnSelectionChangeListener {
        void onSelectionDismiss();
    }


    public void releaseBitmaps() {
        reinit();

        // recycle bitmaps before releasing them.

        if (mEntireBm != null)
            mEntireBm.recycle();
        mEntireBm = null;

        if (mPatchBm != null)
            mPatchBm.recycle();
        mPatchBm = null;
    }

    public void blank(int page) {
        reinit();
        mPageNumber = page;

        if (mBusyIndicator == null) {
            mBusyIndicator = new ProgressBar(mContext);
            mBusyIndicator.setIndeterminate(true);
            addView(mBusyIndicator);
        }

        setBackgroundColor(BACKGROUND_COLOR);
    }

    public void setPage(int page, PointF size) {
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancel();
            mDrawEntire = null;
        }

        mIsBlank = false;
        // Highlights may be missing because mIsBlank was true on last draw
        if (mSearchView != null)
            mSearchView.invalidate();

        mPageNumber = page;
        if (mEntire == null) {
            mEntire = new OpaqueImageView(mContext);
            //mEntire.setColorFilter(Color.RED, PorterDuff.Mode.DST_OVER);
            mEntire.setScaleType(ImageView.ScaleType.MATRIX);
            addView(mEntire);
        }

        // Calculate scaled size that fits within the screen limits
        // This is the size at minimum zoom
        mSourceScale = Math.min(mParentSize.x / size.x, mParentSize.y / size.y);
        Point newSize = new Point((int) (size.x * mSourceScale), (int) (size.y * mSourceScale));
        mSize = newSize;

        mEntire.setImageBitmap(null);
        mEntire.invalidate();

        // Get the link info in the background
        mGetLinkInfo = new AsyncTask<Void, Void, Link[]>() {
            protected Link[] doInBackground(Void... v) {
                return getLinkInfo();
            }

            protected void onPostExecute(Link[] v) {
                mLinks = v;
                if (mSearchView != null)
                    mSearchView.invalidate();
            }
        };
        mGetLinkInfo.execute();

        // Render the page in the background
        mDrawEntire = new CancellableAsyncTask<Void, Void>(getDrawPageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            @Override
            public void onPreExecute() {
                setBackgroundColor(BACKGROUND_COLOR);
                mEntire.setImageBitmap(null);
                mEntire.invalidate();

                if (mBusyIndicator == null) {
                    mBusyIndicator = new ProgressBar(mContext);
                    mBusyIndicator.setIndeterminate(true);
                    addView(mBusyIndicator);
                    mBusyIndicator.setVisibility(INVISIBLE);
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (mBusyIndicator != null)
                                mBusyIndicator.setVisibility(VISIBLE);
                        }
                    }, PROGRESS_DIALOG_DELAY);
                }
            }

            @Override
            public void onPostExecute(Void result) {
                removeView(mBusyIndicator);
                mBusyIndicator = null;
                mEntire.setImageBitmap(getBitmapForTheme(mEntireBm));
                mEntire.invalidate();
                setBackgroundColor(Color.TRANSPARENT);

            }
        };

        mDrawEntire.execute();

        if (mSearchView == null) {
            mSearchView = new View(mContext) {
                @Override
                protected void onDraw(final Canvas canvas) {
                    super.onDraw(canvas);
                    // Work out current total scale factor
                    // from source to view
                    final float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
                    final Paint paint = new Paint();

                    if (!mIsBlank && mSearchBoxes != null) {
                        paint.setColor(HIGHLIGHT_COLOR);
                        for (Quad q : mSearchBoxes) {
                            Path path = new Path();
                            path.moveTo(q.ul_x * scale, q.ul_y * scale);
                            path.lineTo(q.ll_x * scale, q.ll_y * scale);
                            path.lineTo(q.lr_x * scale, q.lr_y * scale);
                            path.lineTo(q.ur_x * scale, q.ur_y * scale);
                            path.close();
                            canvas.drawPath(path, paint);
                        }
                    }

                    if (!mIsBlank && mLinks != null && mHighlightLinks) {
                        paint.setColor(LINK_COLOR);
                        paint.setStrokeWidth(3);
                        for (Link link : mLinks) {
                            canvas.drawLine(link.bounds.x0 * scale, link.bounds.y1 * scale,
                                    link.bounds.x1 * scale, link.bounds.y1 * scale,
                                    paint);
                        }
                    }
                }
            };

            addView(mSearchView);
        }
        requestLayout();
    }

    public void setSearchBoxes(Quad[] searchBoxes) {
        mSearchBoxes = searchBoxes;
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    public void setLinkHighlighting(boolean f) {
        mHighlightLinks = f;
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int x, y;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            x = mSize.x;
        } else {
            x = MeasureSpec.getSize(widthMeasureSpec);
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            y = mSize.y;
        } else {
            y = MeasureSpec.getSize(heightMeasureSpec);
        }

        setMeasuredDimension(x, y);

        if (mBusyIndicator != null) {
            int limit = Math.min(mParentSize.x, mParentSize.y) / 2;
            mBusyIndicator.measure(MeasureSpec.AT_MOST | limit, MeasureSpec.AT_MOST | limit);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;

        if (mEntire != null) {
            if (mEntire.getWidth() != w || mEntire.getHeight() != h) {
                mEntireMat.setScale(w / (float) mSize.x, h / (float) mSize.y);
                mEntire.setImageMatrix(mEntireMat);
                mEntire.invalidate();
            }
            mEntire.layout(0, 0, w, h);
        }

        if (mSearchView != null) {
            mSearchView.layout(0, 0, w, h);
        }

        if (mPatchViewSize != null) {
            if (mPatchViewSize.x != w || mPatchViewSize.y != h) {
                // Zoomed since patch was created
                mPatchViewSize = null;
                mPatchArea = null;
                if (mPatch != null) {
                    mPatch.setImageBitmap(null);
                    mPatch.invalidate();
                }
            } else {
                mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
            }
        }

        if (mBusyIndicator != null) {
            int bw = mBusyIndicator.getMeasuredWidth();
            int bh = mBusyIndicator.getMeasuredHeight();

            mBusyIndicator.layout((w - bw) / 2, (h - bh) / 2, (w + bw) / 2, (h + bh) / 2);
        }
    }

    public void updateHq(boolean update) {
        Rect viewArea = new Rect(getLeft(), getTop(), getRight(), getBottom());
        if (viewArea.width() == mSize.x || viewArea.height() == mSize.y) {
            // If the viewArea's size matches the unzoomed size, there is no need for an hq patch
            if (mPatch != null) {
                mPatch.setImageBitmap(null);
                mPatch.invalidate();
            }
        } else {
            final Point patchViewSize = new Point(viewArea.width(), viewArea.height());
            final Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

            // Intersect and test that there is an intersection
            if (!patchArea.intersect(viewArea))
                return;

            // Offset patch area to be relative to the view top left
            patchArea.offset(-viewArea.left, -viewArea.top);

            boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);

            // If being asked for the same area as last time and not because of an update then nothing to do
            if (area_unchanged && !update)
                return;

            boolean completeRedraw = !(area_unchanged && update);

            // Stop the drawing of previous patch if still going
            if (mDrawPatch != null) {
                mDrawPatch.cancel();
                mDrawPatch = null;
            }

            // Create and add the image view if not already done
            if (mPatch == null) {
                mPatch = new OpaqueImageView(mContext);
                mPatch.setScaleType(ImageView.ScaleType.MATRIX);
                addView(mPatch);
                mSearchView.bringToFront();
            }

            CancellableTaskDefinition<Void, Void> task;

            if (completeRedraw)
                task = getDrawPageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
                        patchArea.left, patchArea.top,
                        patchArea.width(), patchArea.height());
            else
                task = getUpdatePageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
                        patchArea.left, patchArea.top,
                        patchArea.width(), patchArea.height());

            mDrawPatch = new CancellableAsyncTask<Void, Void>(task) {

                public void onPostExecute(Void result) {
                    mPatchViewSize = patchViewSize;
                    mPatchArea = patchArea;
                    mPatch.setImageBitmap(getBitmapForTheme(mPatchBm));
                    mPatch.invalidate();
                    //requestLayout();
                    // Calling requestLayout here doesn't lead to a later call to layout. No idea
                    // why, but apparently others have run into the problem.
                    mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
                }
            };

            mDrawPatch.execute();
        }
    }

    private Bitmap getBitmapForTheme(Bitmap bitmap) {
        if (bitmap == null) return null;
        switch (mTheme) {
            case R.id.page_theme_dark:
                setBackgroundColor(Color.BLACK);
                return invert(bitmap);
            case R.id.page_theme_blue:
                int c = Color.parseColor("#D3E3FA");
                setBackgroundColor(c);
                return replaceColor(bitmap, c);
            case R.id.page_theme_pink:
                int c2 = Color.parseColor("#f9eae0");
                setBackgroundColor(c2);
                return replaceColor(bitmap, c2);
            default:
                setBackgroundColor(Color.BLACK);
                return bitmap;
        }
    }

    private Bitmap replaceColor(Bitmap src, int dest) {
        Bitmap myBitmap = src.copy(src.getConfig(), true);
        // src.recycle();
        int[] allpixels = new int[myBitmap.getHeight() * myBitmap.getWidth()];
        myBitmap.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        for (int i = 0; i < allpixels.length; i++) {
            if (allpixels[i] == Color.WHITE) {
                allpixels[i] = dest;
            }
        }
        myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        return myBitmap;
    }

    private Bitmap invert(Bitmap src) {
        int height = src.getHeight();
        int width = src.getWidth();

        Bitmap bitmap = Bitmap.createBitmap(width, height, src.getConfig());
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        ColorMatrix matrixGrayscale = new ColorMatrix();
        //matrixGrayscale.setSaturation(0);

        ColorMatrix matrixInvert = new ColorMatrix();
        matrixInvert.set(new float[]
                {
                        -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                });
        matrixInvert.preConcat(matrixGrayscale);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrixInvert);
        paint.setColorFilter(filter);

        canvas.drawBitmap(src, 0, 0, paint);
        // src.recycle();
        return bitmap;
    }

    public void update() {
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancel();
            mDrawEntire = null;
        }

        if (mDrawPatch != null) {
            mDrawPatch.cancel();
            mDrawPatch = null;
        }

        // Render the page in the background
        mDrawEntire = new CancellableAsyncTask<Void, Void>(getUpdatePageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            public void onPostExecute(Void result) {
                mEntire.setImageBitmap(mEntireBm);
                mEntire.invalidate();
            }
        };

        mDrawEntire.execute();

        updateHq(true);
    }

    public void removeHq() {
        // Stop the drawing of the patch if still going
        if (mDrawPatch != null) {
            mDrawPatch.cancel();
            mDrawPatch = null;
        }

        // And get rid of it
        mPatchViewSize = null;
        mPatchArea = null;
        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }
    }

    public int getPage() {
        return mPageNumber;
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    public int hitLink(Link link) {
        if (link.isExternal()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.uri));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); // API>=21: FLAG_ACTIVITY_NEW_DOCUMENT
                mContext.startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(mContext, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return 0;
        } else {
            return mCore.resolveLink(link);
        }
    }

    public int hitLink(float x, float y) {
        // Since link highlighting was implemented, the super class
        // PageView has had sufficient information to be able to
        // perform this method directly. Making that change would
        // make MuPDFCore.hitLinkPage superfluous.
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        if (mLinks != null)
            for (Link l : mLinks)
                if (l.bounds.contains(docRelX, docRelY))
                    return hitLink(l);
        return 0;
    }

    protected CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY,
                                                                    final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        return new MuPDFCancellableTaskDefinition<Void, Void>() {
            @Override
            public Void doInBackground(Cookie cookie, Void... params) {
                mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };

    }

    protected CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY,
                                                                      final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        return new MuPDFCancellableTaskDefinition<Void, Void>() {
            @Override
            public Void doInBackground(Cookie cookie, Void... params) {
                mCore.updatePage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };
    }

    protected Link[] getLinkInfo() {
        return mCore.getPageLinks(mPageNumber);
    }

    public List<PDFAnnotation> getAnnotationsAtLocation(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;
        List<PDFAnnotation> pdfAnnotationsAtTouch = new ArrayList<>();
        PDFAnnotation[] pdfAnnotations = mCore.getAnnotations(getPage());
        if (pdfAnnotations != null) {
            for (PDFAnnotation pdfAnnotation : pdfAnnotations) {
                if (pdfAnnotation.getBounds().contains(docRelX, docRelY)) {
                    pdfAnnotationsAtTouch.add(pdfAnnotation);
                }
            }
        }
        return pdfAnnotationsAtTouch;
    }

    public List<PDFWidget> getPDFWidgets(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;
        List<PDFWidget> pdfWidgetList = new ArrayList<>();
        PDFWidget[] pdfWidgets = mCore.getWidgets(getPage());
        if (pdfWidgets != null) {
            for (PDFWidget pdfWidget : pdfWidgets) {
                if (pdfWidget.getBounds().contains(docRelX, docRelY)) {
                    pdfWidgetList.add(pdfWidget);
                }
            }
        }
        LogUtils.logPDFWidgets(pdfWidgetList);
        return pdfWidgetList;
    }
}

package com.avrapps.pdfviewer.viewer_fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.avrapps.pdfviewer.R;

public class ColorDialog {
    final AlertDialog dialog;
    final OnColorChangedListener listener;
    final View viewHue;
    final ColorDialogSquare viewSatVal;
    final ImageView viewCursor;
    final ImageView viewAlphaCursor;
    final View viewOldColor;
    final View viewNewColor;
    final View viewAlphaOverlay;
    final ImageView viewTarget;
    final ImageView viewAlphaCheckered;
    final ViewGroup viewContainer;
    final float[] currentColorHsv = new float[3];
    private final boolean supportsAlpha;
    int alpha;

    /**
     * Create an ColorDialog.
     *
     * @param context  activity context
     * @param color    current color
     * @param listener an OnColorChangedListener, allowing you to get back error or OK
     */
    public ColorDialog(final Context context, int color, OnColorChangedListener listener) {
        this(context, color, false, listener);
    }

    /**
     * Create an ColorDialog.
     *
     * @param context       activity context
     * @param color         current color
     * @param supportsAlpha whether alpha/transparency controls are enabled
     * @param listener      an OnColorChangedListener, allowing you to get back error or OK
     */
    @SuppressLint("ClickableViewAccessibility")
    public ColorDialog(final Context context, int color, boolean supportsAlpha, OnColorChangedListener listener) {
        this.supportsAlpha = supportsAlpha;
        this.listener = listener;

        if (!supportsAlpha) { // remove alpha if not supported
            color = color | 0xff000000;
        }

        Color.colorToHSV(color, currentColorHsv);
        alpha = Color.alpha(color);

        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        viewHue = view.findViewById(R.id.ambilwarna_viewHue);
        viewSatVal = view.findViewById(R.id.ambilwarna_viewSatBri);
        viewCursor = view.findViewById(R.id.ambilwarna_cursor);
        viewOldColor = view.findViewById(R.id.ambilwarna_oldColor);
        viewNewColor = view.findViewById(R.id.ambilwarna_newColor);
        viewTarget = view.findViewById(R.id.ambilwarna_target);
        viewContainer = view.findViewById(R.id.ambilwarna_viewContainer);
        viewAlphaOverlay = view.findViewById(R.id.ambilwarna_overlay);
        viewAlphaCursor = view.findViewById(R.id.ambilwarna_alphaCursor);
        viewAlphaCheckered = view.findViewById(R.id.ambilwarna_alphaCheckered);

        { // hide/show alpha
            viewAlphaOverlay.setVisibility(supportsAlpha ? View.VISIBLE : View.GONE);
            viewAlphaCursor.setVisibility(supportsAlpha ? View.VISIBLE : View.GONE);
            viewAlphaCheckered.setVisibility(supportsAlpha ? View.VISIBLE : View.GONE);
        }

        viewSatVal.setHue(getHue());
        viewOldColor.setBackgroundColor(color);
        viewNewColor.setBackgroundColor(color);

        viewHue.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE
                    || event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_UP) {

                float y = event.getY();
                if (y < 0.f) y = 0.f;
                if (y > viewHue.getMeasuredHeight()) {
                    y = viewHue.getMeasuredHeight() - 0.001f; // to avoid jumping the cursor from bottom to top.
                }
                float hue = 360.f - 360.f / viewHue.getMeasuredHeight() * y;
                if (hue == 360.f) hue = 0.f;
                setHue(hue);

                // update view
                viewSatVal.setHue(getHue());
                moveCursor();
                viewNewColor.setBackgroundColor(getColor());
                updateAlphaView();
                return true;
            }
            return false;
        });

        if (supportsAlpha) viewAlphaCheckered.setOnTouchListener((v, event) -> {
            if ((event.getAction() == MotionEvent.ACTION_MOVE)
                    || (event.getAction() == MotionEvent.ACTION_DOWN)
                    || (event.getAction() == MotionEvent.ACTION_UP)) {

                float y = event.getY();
                if (y < 0.f) {
                    y = 0.f;
                }
                if (y > viewAlphaCheckered.getMeasuredHeight()) {
                    y = viewAlphaCheckered.getMeasuredHeight() - 0.001f; // to avoid jumping the cursor from bottom to top.
                }
                final int a = Math.round(255.f - ((255.f / viewAlphaCheckered.getMeasuredHeight()) * y));
                ColorDialog.this.setAlpha(a);

                // update view
                moveAlphaCursor();
                int col = ColorDialog.this.getColor();
                int c = a << 24 | col & 0x00ffffff;
                viewNewColor.setBackgroundColor(c);
                return true;
            }
            return false;
        });
        viewSatVal.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE
                    || event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_UP) {

                float x = event.getX(); // touch event are in dp units.
                float y = event.getY();

                if (x < 0.f) x = 0.f;
                if (x > viewSatVal.getMeasuredWidth()) x = viewSatVal.getMeasuredWidth();
                if (y < 0.f) y = 0.f;
                if (y > viewSatVal.getMeasuredHeight()) y = viewSatVal.getMeasuredHeight();

                setSat(1.f / viewSatVal.getMeasuredWidth() * x);
                setVal(1.f - (1.f / viewSatVal.getMeasuredHeight() * y));

                // update view
                moveTarget();
                viewNewColor.setBackgroundColor(getColor());

                return true;
            }
            return false;
        });

        // if back button is used, call back our listener.
        dialog = new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (ColorDialog.this.listener != null) {
                        ColorDialog.this.listener.onOk(ColorDialog.this, getColor());
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if (ColorDialog.this.listener != null) {
                        ColorDialog.this.listener.onCancel(ColorDialog.this);
                    }
                })
                .setOnCancelListener(paramDialogInterface -> {
                    if (ColorDialog.this.listener != null) {
                        ColorDialog.this.listener.onCancel(ColorDialog.this);
                    }

                })
                .create();
        // kill all padding from the dialog window
        dialog.setView(view, 0, 0, 0, 0);

        // move cursor & target on first draw
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                moveCursor();
                if (ColorDialog.this.supportsAlpha) moveAlphaCursor();
                moveTarget();
                if (ColorDialog.this.supportsAlpha) updateAlphaView();
                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    protected void moveCursor() {
        float y = viewHue.getMeasuredHeight() - (getHue() * viewHue.getMeasuredHeight() / 360.f);
        if (y == viewHue.getMeasuredHeight()) y = 0.f;
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewCursor.getLayoutParams();
        layoutParams.leftMargin = (int) (viewHue.getLeft() - Math.floor(viewCursor.getMeasuredWidth() / 2) - viewContainer.getPaddingLeft());
        layoutParams.topMargin = (int) (viewHue.getTop() + y - Math.floor(viewCursor.getMeasuredHeight() / 2) - viewContainer.getPaddingTop());
        viewCursor.setLayoutParams(layoutParams);
    }

    protected void moveTarget() {
        float x = getSat() * viewSatVal.getMeasuredWidth();
        float y = (1.f - getVal()) * viewSatVal.getMeasuredHeight();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewTarget.getLayoutParams();
        layoutParams.leftMargin = (int) (viewSatVal.getLeft() + x - Math.floor(viewTarget.getMeasuredWidth() / 2) - viewContainer.getPaddingLeft());
        layoutParams.topMargin = (int) (viewSatVal.getTop() + y - Math.floor(viewTarget.getMeasuredHeight() / 2) - viewContainer.getPaddingTop());
        viewTarget.setLayoutParams(layoutParams);
    }

    protected void moveAlphaCursor() {
        final int measuredHeight = this.viewAlphaCheckered.getMeasuredHeight();
        float y = measuredHeight - ((this.getAlpha() * measuredHeight) / 255.f);
        final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.viewAlphaCursor.getLayoutParams();
        layoutParams.leftMargin = (int) (this.viewAlphaCheckered.getLeft() - Math.floor(this.viewAlphaCursor.getMeasuredWidth() / 2) - this.viewContainer.getPaddingLeft());
        layoutParams.topMargin = (int) ((this.viewAlphaCheckered.getTop() + y) - Math.floor(this.viewAlphaCursor.getMeasuredHeight() / 2) - this.viewContainer.getPaddingTop());

        this.viewAlphaCursor.setLayoutParams(layoutParams);
    }

    private int getColor() {
        final int argb = Color.HSVToColor(currentColorHsv);
        return alpha << 24 | (argb & 0x00ffffff);
    }

    private float getHue() {
        return currentColorHsv[0];
    }

    private void setHue(float hue) {
        currentColorHsv[0] = hue;
    }

    private float getAlpha() {
        return this.alpha;
    }

    private void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    private float getSat() {
        return currentColorHsv[1];
    }

    private void setSat(float sat) {
        currentColorHsv[1] = sat;
    }

    private float getVal() {
        return currentColorHsv[2];
    }

    private void setVal(float val) {
        currentColorHsv[2] = val;
    }

    public void show() {
        dialog.show();
    }

    public AlertDialog getDialog() {
        return dialog;
    }

    private void updateAlphaView() {
        final GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                Color.HSVToColor(currentColorHsv), 0x0
        });
        viewAlphaOverlay.setBackgroundDrawable(gd);
    }

    public interface OnColorChangedListener {
        void onCancel(ColorDialog dialog);

        void onOk(ColorDialog dialog, int color);
    }

}
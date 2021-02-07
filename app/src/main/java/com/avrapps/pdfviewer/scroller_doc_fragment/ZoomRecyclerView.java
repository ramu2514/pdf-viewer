package com.avrapps.pdfviewer.scroller_doc_fragment;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.MotionEvent.INVALID_POINTER_ID;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.avrapps.pdfviewer.R;


@SuppressLint("ClickableViewAccessibility")
public class ZoomRecyclerView extends RecyclerView {

    // constant
    private static final int DEFAULT_SCALE_DURATION = 300;
    private static final float DEFAULT_SCALE_FACTOR = 1.f;
    private static final float DEFAULT_MAX_SCALE_FACTOR = 2.0f;
    private static final float DEFAULT_MIN_SCALE_FACTOR = 0.5f;
    private static final String PROPERTY_SCALE = "scale";
    private static final String PROPERTY_TRANX = "tranX";
    private static final String PROPERTY_TRANY = "tranY";
    private static final float INVALID_TOUCH_POSITION = -1;

    // touch detector
    ScaleGestureDetector mScaleDetector;
    GestureDetectorCompat mGestureDetector;

    // draw param
    float mViewWidth;       // 宽度
    float mViewHeight;      // 高度
    float mTranX;           // x偏移量
    float mTranY;           // y偏移量
    float mScaleFactor;     // 缩放系数

    // touch param
    int mActivePointerId = INVALID_POINTER_ID;  // 有效的手指id
    float mLastTouchX;      // 上一次触摸位置 X
    float mLastTouchY;      // 上一次触摸位置 Y

    // control param
    boolean isScaling = false;    // 是否正在缩放
    boolean isEnableScale = false;// 是否支持缩放

    // zoom param
    ValueAnimator mScaleAnimator; //缩放动画
    float mScaleCenterX;    // 缩放中心 X
    float mScaleCenterY;    // 缩放中心 Y
    float mMaxTranX;        // 当前缩放系数下最大的X偏移量
    float mMaxTranY;        // 当前缩放系数下最大的Y偏移量

    // config param
    float mMaxScaleFactor;      // 最大缩放系数
    float mMinScaleFactor;      // 最小缩放系数
    float mDefaultScaleFactor;  // 默认缩放系数 双击缩小后的缩放系数 暂不支持小于1
    int mScaleDuration;         // 缩放时间 ms

    public ZoomRecyclerView(Context context) {
        super(context);
        init(null);
    }

    public ZoomRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ZoomRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attr) {
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        mGestureDetector = new GestureDetectorCompat(getContext(), new GestureListener());

        if (attr != null) {
            TypedArray a = getContext()
                    .obtainStyledAttributes(attr, R.styleable.ZoomRecyclerView, 0, 0);
            mMinScaleFactor =
                    a.getFloat(R.styleable.ZoomRecyclerView_min_scale, DEFAULT_MIN_SCALE_FACTOR);
            mMaxScaleFactor =
                    a.getFloat(R.styleable.ZoomRecyclerView_max_scale, DEFAULT_MAX_SCALE_FACTOR);
            mDefaultScaleFactor = a
                    .getFloat(R.styleable.ZoomRecyclerView_default_scale, DEFAULT_SCALE_FACTOR);
            mScaleFactor = mDefaultScaleFactor;
            mScaleDuration = a.getInteger(R.styleable.ZoomRecyclerView_zoom_duration,
                    DEFAULT_SCALE_DURATION);
            a.recycle();
        } else {
            //init param with default
            mMaxScaleFactor = DEFAULT_MAX_SCALE_FACTOR;
            mMinScaleFactor = DEFAULT_MIN_SCALE_FACTOR;
            mDefaultScaleFactor = DEFAULT_SCALE_FACTOR;
            mScaleFactor = mDefaultScaleFactor;
            mScaleDuration = DEFAULT_SCALE_DURATION;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
        mViewHeight = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {

        if (!isEnableScale) {
            return super.onTouchEvent(ev);
        }

        boolean retVal = mScaleDetector.onTouchEvent(ev);
        retVal = mGestureDetector.onTouchEvent(ev) || retVal;

        int action = ev.getActionMasked();

        switch (action) {
            case ACTION_DOWN: {
                final int pointerIndex = ev.getActionIndex();
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);
                // Remember where we started (for dragging)
                mLastTouchX = x;
                mLastTouchY = y;
                // Save the ID of this pointer (for dragging)
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case ACTION_MOVE: {
                try {
                    // Find the index of the active pointer and fetch its position
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);

                    final float x = ev.getX(pointerIndex);
                    final float y = ev.getY(pointerIndex);

                    if (!isScaling && mScaleFactor > 1) { // 缩放时不做处理
                        // Calculate the distance moved
                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;

                        setTranslateXY(mTranX + dx, mTranY + dy);
                        correctTranslateXY();
                    }

                    invalidate();
                    // Remember this touch position for the next move event
                    mLastTouchX = x;
                    mLastTouchY = y;
                } catch (Exception e) {
                    final float x = ev.getX();
                    final float y = ev.getY();

                    if (!isScaling && mScaleFactor > 1 && mLastTouchX != INVALID_TOUCH_POSITION) { // 缩放时不做处理
                        // Calculate the distance moved
                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;

                        setTranslateXY(mTranX + dx, mTranY + dy);
                        correctTranslateXY();
                    }

                    invalidate();
                    // Remember this touch position for the next move event
                    mLastTouchX = x;
                    mLastTouchY = y;
                }
                break;
            }
            case ACTION_UP:
            case ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER_ID;
                mLastTouchX = INVALID_TOUCH_POSITION;
                mLastTouchY = INVALID_TOUCH_POSITION;
                break;
            case ACTION_POINTER_UP: {
                final int pointerIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }

        return super.onTouchEvent(ev) || retVal;
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {

        canvas.save();
        canvas.translate(mTranX, mTranY);
        canvas.scale(mScaleFactor, mScaleFactor);

        // 所有子view都会缩放和平移
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private void setTranslateXY(float tranX, float tranY) {
        mTranX = tranX;
        mTranY = tranY;
    }

    //当scale 大于 1 时修正action move的位置
    private void correctTranslateXY() {
        float[] correctXY = correctTranslateXY(mTranX, mTranY);
        mTranX = correctXY[0];
        mTranY = correctXY[1];
    }

    private float[] correctTranslateXY(float x, float y) {
        if (mScaleFactor <= 1) {
            return new float[]{x, y};
        }

        if (x > 0.0f) {
            x = 0.0f;
        } else if (x < mMaxTranX) {
            x = mMaxTranX;
        }

        if (y > 0.0f) {
            y = 0.0f;
        } else if (y < mMaxTranY) {
            y = mMaxTranY;
        }
        return new float[]{x, y};
    }

    private void zoom(float startVal, float endVal) {
        if (mScaleAnimator == null) {
            newZoomAnimation();
        }

        if (mScaleAnimator.isRunning()) {
            return;
        }

        //set Value
        mMaxTranX = mViewWidth - (mViewWidth * endVal);
        mMaxTranY = mViewHeight - (mViewHeight * endVal);

        float startTranX = mTranX;
        float startTranY = mTranY;
        float endTranX = mTranX - (endVal - startVal) * mScaleCenterX;
        float endTranY = mTranY - (endVal - startVal) * mScaleCenterY;
        float[] correct = correctTranslateXY(endTranX, endTranY);
        endTranX = correct[0];
        endTranY = correct[1];

        PropertyValuesHolder scaleHolder = PropertyValuesHolder
                .ofFloat(PROPERTY_SCALE, startVal, endVal);
        PropertyValuesHolder tranXHolder = PropertyValuesHolder
                .ofFloat(PROPERTY_TRANX, startTranX, endTranX);
        PropertyValuesHolder tranYHolder = PropertyValuesHolder
                .ofFloat(PROPERTY_TRANY, startTranY, endTranY);
        mScaleAnimator.setValues(scaleHolder, tranXHolder, tranYHolder);
        mScaleAnimator.setDuration(mScaleDuration);
        mScaleAnimator.start();
    }

    private void newZoomAnimation() {
        mScaleAnimator = new ValueAnimator();
        mScaleAnimator.setInterpolator(new DecelerateInterpolator());
        mScaleAnimator.addUpdateListener(animation -> {
            //update scaleFactor & tranX & tranY
            mScaleFactor = (float) animation.getAnimatedValue(PROPERTY_SCALE);
            setTranslateXY(
                    (float) animation.getAnimatedValue(PROPERTY_TRANX),
                    (float) animation.getAnimatedValue(PROPERTY_TRANY)
            );
            invalidate();
        });

        // set listener to update scale flag
        mScaleAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                isScaling = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isScaling = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isScaling = false;
            }

        });

    }

    // handle scale event
    private class ScaleListener implements OnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            final float mLastScale = mScaleFactor;
            mScaleFactor *= detector.getScaleFactor();
            //修正scaleFactor
            mScaleFactor = Math.max(mMinScaleFactor, Math.min(mScaleFactor, mMaxScaleFactor));

            mMaxTranX = mViewWidth - (mViewWidth * mScaleFactor);
            mMaxTranY = mViewHeight - (mViewHeight * mScaleFactor);

            mScaleCenterX = detector.getFocusX();
            mScaleCenterY = detector.getFocusY();

            float offsetX = mScaleCenterX * (mLastScale - mScaleFactor);
            float offsetY = mScaleCenterY * (mLastScale - mScaleFactor);

            setTranslateXY(mTranX + offsetX, mTranY + offsetY);

            isScaling = true;
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mScaleFactor <= mDefaultScaleFactor) {
                mScaleCenterX = -mTranX / (mScaleFactor - 1);
                mScaleCenterY = -mTranY / (mScaleFactor - 1);
                mScaleCenterX = Float.isNaN(mScaleCenterX) ? 0 : mScaleCenterX;
                mScaleCenterY = Float.isNaN(mScaleCenterY) ? 0 : mScaleCenterY;
                zoom(mScaleFactor, mDefaultScaleFactor);
            }
            isScaling = false;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float startFactor = mScaleFactor;
            float endFactor;

            if (mScaleFactor == mDefaultScaleFactor) {
                mScaleCenterX = e.getX();
                mScaleCenterY = e.getY();
                endFactor = mMaxScaleFactor;
            } else {
                mScaleCenterX = mScaleFactor == 1 ? e.getX() : -mTranX / (mScaleFactor - 1);
                mScaleCenterY = mScaleFactor == 1 ? e.getY() : -mTranY / (mScaleFactor - 1);
                endFactor = mDefaultScaleFactor;
            }
            zoom(startFactor, endFactor);
            return super.onDoubleTap(e);
        }
    }

    // public method
    public void setEnableScale(boolean enable) {
        if (isEnableScale == enable) {
            return;
        }
        this.isEnableScale = enable;
        // 禁用了 恢复比例1
        if (!isEnableScale && mScaleFactor != 1) {
            zoom(mScaleFactor, 1);
        }
    }

    public boolean isEnableScale() {
        return isEnableScale;
    }

}

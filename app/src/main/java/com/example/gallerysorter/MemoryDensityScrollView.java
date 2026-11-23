package com.example.gallerysorter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.view.ScaleGestureDetector;
import android.widget.ScrollView;

import java.util.function.BooleanSupplier;

/** Keeps list taps and single-finger scrolling on the ordinary ScrollView path. */
final class MemoryDensityScrollView extends ScrollView {
    private final BooleanSupplier wide;
    private final ScaleGestureDetector detector;
    private boolean pinching;
    private Runnable render;
    private TextView indicator;
    private float focusX, focusY;
    private int pendingColumns;
    private final Runnable applyDensity = this::applyDensity;
    private final Runnable hideIndicator = this::removeIndicator;

    MemoryDensityScrollView(Context context, MemoryListDensity density, BooleanSupplier wide) {
        super(context);
        this.wide = wide;
        detector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector gesture) {
                density.beginPinch();
                return true;
            }

            @Override public boolean onScale(ScaleGestureDetector gesture) {
                if (density.scaleBy(gesture.getScaleFactor(), wide.getAsBoolean())) {
                    focusX = gesture.getFocusX();
                    focusY = gesture.getFocusY();
                    pendingColumns = density.columns(wide.getAsBoolean());
                    removeCallbacks(applyDensity);
                    postOnAnimation(applyDensity);
                }
                return true;
            }
        });
        detector.setQuickScaleEnabled(false);
        detector.setStylusScaleEnabled(false);
    }

    void setOnDensityChanged(Runnable render) {
        this.render = render;
    }

    private void applyDensity() {
        if (!isAttachedToWindow() || render == null) return;
        showIndicator(pendingColumns);
        MemoryBrowserScrollController.aroundGridChange(this, focusX, focusY, render);
    }

    private void showIndicator(int columns) {
        removeCallbacks(hideIndicator);
        removeIndicator();
        indicator = CleanupUiStyle.text(getContext(), columns + "열", 13, false, CleanupUiStyle.PURPLE);
        indicator.setGravity(Gravity.CENTER);
        int pad = CleanupUiStyle.dp(getContext(), 14);
        indicator.setPadding(pad, pad / 2, pad, pad / 2);
        GradientDrawable chip = MemorySelectionStyle.surface(getContext(), 0xfff3edfc, 16);
        chip.setStroke(CleanupUiStyle.dp(getContext(), 1), 0xffded3f4);
        indicator.setBackground(chip);
        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        indicator.measure(unspecified, unspecified);
        int left = (getWidth() - indicator.getMeasuredWidth()) / 2;
        int top = Math.min(Math.max(pad, (int) focusY - pad * 4),
                Math.max(pad, getHeight() - indicator.getMeasuredHeight() - pad));
        indicator.layout(left, top, left + indicator.getMeasuredWidth(), top + indicator.getMeasuredHeight());
        getOverlay().add(indicator);
        announceForAccessibility(indicator.getText());
        postDelayed(hideIndicator, 750);
    }

    private void removeIndicator() {
        if (indicator == null) return;
        getOverlay().remove(indicator);
        indicator = null;
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(applyDensity);
        removeCallbacks(hideIndicator);
        removeIndicator();
        super.onDetachedFromWindow();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) pinching = false;
        if (!pinching && render == null) return super.dispatchTouchEvent(event);
        if (!pinching && action == MotionEvent.ACTION_POINTER_DOWN) {
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            try {
                super.dispatchTouchEvent(cancel);
            } finally {
                cancel.recycle();
            }
            pinching = true;
        }
        detector.onTouchEvent(event);
        if (!pinching) return super.dispatchTouchEvent(event);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) pinching = false;
        return true;
    }
}

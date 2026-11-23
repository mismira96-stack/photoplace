package com.example.gallerysorter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * Matrix-backed ImageView supporting 1.0x - 4.0x pinch zoom, 2.5x animated double-tap zoom,
 * and edge-clamped panning when zoomed in.
 */
final class MemoryZoomableImageView extends ImageView {
    private final Matrix baseMatrix = new Matrix();
    private final Matrix suppMatrix = new Matrix();
    private final Matrix drawMatrix = new Matrix();
    private final RectF displayRect = new RectF();
    private final float[] matrixValues = new float[9];
    private final ScaleGestureDetector scaleDetector;
    private ValueAnimator zoomAnimator;
    private boolean zoomEnabled = true;

    MemoryZoomableImageView(Context context) {
        super(context);
        super.setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!zoomEnabled || getDrawable() == null) {
                    return false;
                }
                float scaleFactor = detector.getScaleFactor();
                if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor)) {
                    return false;
                }
                applyScaleFactor(scaleFactor, detector.getFocusX(), detector.getFocusY());
                return true;
            }
        });
    }

    void setZoomEnabled(boolean enabled) {
        this.zoomEnabled = enabled;
        if (!enabled) {
            resetZoom();
        }
    }

    boolean isZoomed() {
        return MemoryViewerGesturePolicy.isZoomed(currentScale());
    }

    float currentScale() {
        suppMatrix.getValues(matrixValues);
        float sx = matrixValues[Matrix.MSCALE_X];
        float skewY = matrixValues[Matrix.MSKEW_Y];
        float scale = (float) Math.sqrt(sx * sx + skewY * skewY);
        return scale <= 0.0f ? 1.0f : scale;
    }

    void onScaleEvent(MotionEvent event) {
        if (zoomEnabled) {
            scaleDetector.onTouchEvent(event);
        }
    }

    boolean isPinching() {
        return scaleDetector.isInProgress();
    }

    void toggleDoubleTapZoom(float focusX, float focusY) {
        if (!zoomEnabled || getDrawable() == null) {
            return;
        }
        cancelZoomAnimator();
        final float startScale = currentScale();
        final float targetScale = MemoryViewerGesturePolicy.nextDoubleTapScale(startScale);
        final float px = getWidth() > 0 ? focusX : getWidth() / 2.0f;
        final float py = getHeight() > 0 ? focusY : getHeight() / 2.0f;
        zoomAnimator = ValueAnimator.ofFloat(startScale, targetScale);
        zoomAnimator.setDuration(165L);
        zoomAnimator.setInterpolator(new DecelerateInterpolator());
        zoomAnimator.addUpdateListener(animation -> {
            float desired = (Float) animation.getAnimatedValue();
            float current = currentScale();
            if (current > 0.0f) {
                applyScaleFactor(desired / current, px, py);
            }
        });
        zoomAnimator.start();
    }

    void panBy(float dx, float dy) {
        if (!isZoomed() || getDrawable() == null) {
            return;
        }
        suppMatrix.postTranslate(dx, dy);
        clampAndApplyMatrix();
    }

    void resetZoom() {
        cancelZoomAnimator();
        suppMatrix.reset();
        clampAndApplyMatrix();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        updateBaseMatrix();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        updateBaseMatrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateBaseMatrix();
    }

    private void applyScaleFactor(float factor, float focusX, float focusY) {
        float current = currentScale();
        float target = MemoryViewerGesturePolicy.clampScale(current * factor);
        float effectiveFactor = target / current;
        suppMatrix.postScale(effectiveFactor, effectiveFactor, focusX, focusY);
        clampAndApplyMatrix();
    }

    private void updateBaseMatrix() {
        cancelZoomAnimator();
        suppMatrix.reset();
        baseMatrix.reset();
        Drawable d = getDrawable();
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (d == null || viewWidth <= 0 || viewHeight <= 0) {
            setImageMatrix(baseMatrix);
            return;
        }
        int drawableWidth = d.getIntrinsicWidth();
        int drawableHeight = d.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            setImageMatrix(baseMatrix);
            return;
        }
        RectF src = new RectF(0, 0, drawableWidth, drawableHeight);
        RectF dst = new RectF(0, 0, viewWidth, viewHeight);
        baseMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
        clampAndApplyMatrix();
    }

    private void clampAndApplyMatrix() {
        Drawable d = getDrawable();
        if (d == null) {
            return;
        }
        drawMatrix.set(baseMatrix);
        drawMatrix.postConcat(suppMatrix);
        displayRect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        drawMatrix.mapRect(displayRect);

        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (viewWidth <= 0 || viewHeight <= 0) {
            setImageMatrix(drawMatrix);
            return;
        }

        float deltaX = 0.0f;
        float deltaY = 0.0f;
        float height = displayRect.height();
        float width = displayRect.width();

        if (height <= viewHeight) {
            deltaY = (viewHeight - height) / 2.0f - displayRect.top;
        } else if (displayRect.top > 0) {
            deltaY = -displayRect.top;
        } else if (displayRect.bottom < viewHeight) {
            deltaY = viewHeight - displayRect.bottom;
        }

        if (width <= viewWidth) {
            deltaX = (viewWidth - width) / 2.0f - displayRect.left;
        } else if (displayRect.left > 0) {
            deltaX = -displayRect.left;
        } else if (displayRect.right < viewWidth) {
            deltaX = viewWidth - displayRect.right;
        }

        if (deltaX != 0.0f || deltaY != 0.0f) {
            suppMatrix.postTranslate(deltaX, deltaY);
            drawMatrix.set(baseMatrix);
            drawMatrix.postConcat(suppMatrix);
        }
        setImageMatrix(drawMatrix);
    }

    private void cancelZoomAnimator() {
        if (zoomAnimator != null) {
            zoomAnimator.cancel();
            zoomAnimator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelZoomAnimator();
        super.onDetachedFromWindow();
    }
}

package com.example.gallerysorter;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Crisp, anti-aliased vector drawables for media controls and badges.
 * Prevents system emoji font substitution (▶, ⏸, 🔊, 🔇) and legacy Android 2.x icons.
 */
final class MediaVectorIconDrawable extends Drawable {
    static final int KIND_PLAY = 1;
    static final int KIND_PAUSE = 2;
    static final int KIND_VOLUME_ON = 3;
    static final int KIND_VOLUME_OFF = 4;
    static final int KIND_PHOTO = 5;
    static final int KIND_VIDEO = 6;
    static final int KIND_TRASH = 7;
    static final int KIND_SEARCH = 8;
    static final int KIND_SHARE = 9;
    static final int KIND_MORE = 10;
    static final int KIND_PLACE = 11;
    static final int KIND_SAVE = 12;
    private static final int KIND_ADD_PLACE = 13;
    private static final int KIND_ADD_COLLECTION = 14;
    private static final int KIND_FOLDER_PLUS = 15;
    private static final int KIND_CLOSE = 16;

    private final int kind;
    private final int sizePx;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private MediaVectorIconDrawable(int kind, int color, int sizePx) {
        this.kind = kind;
        this.sizePx = Math.max(8, sizePx);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setColor(color);
    }

    static MediaVectorIconDrawable play(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_PLAY, color, sizePx);
    }

    static MediaVectorIconDrawable pause(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_PAUSE, color, sizePx);
    }

    static MediaVectorIconDrawable volumeOn(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_VOLUME_ON, color, sizePx);
    }

    static MediaVectorIconDrawable volumeOff(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_VOLUME_OFF, color, sizePx);
    }

    static MediaVectorIconDrawable photo(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_PHOTO, color, sizePx);
    }

    static MediaVectorIconDrawable video(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_VIDEO, color, sizePx);
    }

    static MediaVectorIconDrawable trash(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_TRASH, color, sizePx);
    }

    static MediaVectorIconDrawable search(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_SEARCH, color, sizePx);
    }

    static MediaVectorIconDrawable close(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_CLOSE, color, sizePx);
    }

    static MediaVectorIconDrawable share(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_SHARE, color, sizePx);
    }

    static MediaVectorIconDrawable more(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_MORE, color, sizePx);
    }

    static MediaVectorIconDrawable place(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_PLACE, color, sizePx);
    }
    static MediaVectorIconDrawable save(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_SAVE, color, sizePx);
    }

    static MediaVectorIconDrawable discoveryAdd(int color, int sizePx, boolean collection) {
        return new MediaVectorIconDrawable(collection ? KIND_ADD_COLLECTION : KIND_ADD_PLACE, color, sizePx);
    }

    static MediaVectorIconDrawable folderPlus(int color, int sizePx) {
        return new MediaVectorIconDrawable(KIND_FOLDER_PLUS, color, sizePx);
    }

    @Override
    public int getIntrinsicWidth() {
        return sizePx;
    }

    @Override
    public int getIntrinsicHeight() {
        return sizePx;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        float w = b.width();
        float h = b.height();
        float cx = b.exactCenterX();
        float cy = b.exactCenterY();
        float s = Math.min(w, h);
        strokePaint.setStrokeWidth(Math.max(1.5f, s * 0.095f));

        switch (kind) {
            case KIND_PLAY:
                drawPlay(canvas, cx, cy, s);
                break;
            case KIND_PAUSE:
                drawPause(canvas, cx, cy, s);
                break;
            case KIND_VOLUME_ON:
                drawSpeaker(canvas, cx, cy, s, false);
                break;
            case KIND_VOLUME_OFF:
                drawSpeaker(canvas, cx, cy, s, true);
                break;
            case KIND_PHOTO:
                drawPhoto(canvas, cx, cy, s);
                break;
            case KIND_VIDEO:
                drawVideoBadge(canvas, cx, cy, s);
                break;
            case KIND_TRASH:
                drawTrash(canvas, cx, cy, s);
                break;
            case KIND_SEARCH:
                drawSearch(canvas, cx, cy, s);
                break;
            case KIND_CLOSE:
                float half = s * .24f;
                canvas.drawLine(cx - half, cy - half, cx + half, cy + half, strokePaint);
                canvas.drawLine(cx + half, cy - half, cx - half, cy + half, strokePaint);
                break;
            case KIND_SHARE:
                drawShare(canvas, cx, cy, s);
                break;
            case KIND_MORE:
                drawMore(canvas, cx, cy, s);
                break;
            case KIND_PLACE:
                Path pin = new Path();
                pin.moveTo(cx, cy + s * .42f);
                pin.cubicTo(cx - s * .6f, cy - s * .15f, cx - s * .32f, cy - s * .43f, cx, cy - s * .43f);
                pin.cubicTo(cx + s * .32f, cy - s * .43f, cx + s * .6f, cy - s * .15f, cx, cy + s * .42f);
                canvas.drawPath(pin, strokePaint);
                canvas.drawCircle(cx, cy - s * .12f, s * .105f, strokePaint);
                break;
            case KIND_ADD_PLACE:
            case KIND_ADD_COLLECTION:
                drawDiscoveryAdd(canvas, cx, cy, s, kind == KIND_ADD_COLLECTION);
                break;
            case KIND_FOLDER_PLUS:
                drawFolderPlus(canvas, cx, cy, s);
                break;
            case KIND_SAVE:
                canvas.drawLine(cx, cy - s * .4f, cx, cy + s * .12f, strokePaint);
                canvas.drawLine(cx - s * .17f, cy - s * .05f, cx, cy + s * .12f, strokePaint);
                canvas.drawLine(cx + s * .17f, cy - s * .05f, cx, cy + s * .12f, strokePaint);
                Path tray = new Path();
                tray.moveTo(cx - s * .34f, cy + s * .12f);
                tray.lineTo(cx - s * .34f, cy + s * .4f);
                tray.lineTo(cx + s * .34f, cy + s * .4f);
                tray.lineTo(cx + s * .34f, cy + s * .12f);
                canvas.drawPath(tray, strokePaint);
                break;
            default:
                break;
        }
    }

    private void drawPlay(Canvas canvas, float cx, float cy, float s) {
        float halfH = s * 0.34f;
        float left = cx - s * 0.24f;
        float right = cx + s * 0.34f;
        Path path = new Path();
        path.moveTo(left, cy - halfH);
        path.lineTo(right, cy);
        path.lineTo(left, cy + halfH);
        path.close();
        Paint roundedFill = new Paint(fillPaint);
        roundedFill.setStrokeJoin(Paint.Join.ROUND);
        roundedFill.setStrokeCap(Paint.Cap.ROUND);
        roundedFill.setStyle(Paint.Style.FILL_AND_STROKE);
        roundedFill.setStrokeWidth(Math.max(1.2f, s * 0.08f));
        canvas.drawPath(path, roundedFill);
    }

    private void drawPause(Canvas canvas, float cx, float cy, float s) {
        float barW = Math.max(2.0f, s * 0.17f);
        float halfH = s * 0.33f;
        float gap = s * 0.14f;
        float r = barW * 0.45f;
        RectF leftBar = new RectF(cx - gap - barW, cy - halfH, cx - gap, cy + halfH);
        RectF rightBar = new RectF(cx + gap, cy - halfH, cx + gap + barW, cy + halfH);
        canvas.drawRoundRect(leftBar, r, r, fillPaint);
        canvas.drawRoundRect(rightBar, r, r, fillPaint);
    }

    private void drawSpeaker(Canvas canvas, float cx, float cy, float s, boolean muted) {
        float left = cx - s * 0.36f;
        float bodyRight = cx - s * 0.18f;
        float coneRight = cx + s * 0.04f;
        float bodyHalfH = s * 0.13f;
        float coneHalfH = s * 0.31f;

        Path cone = new Path();
        cone.moveTo(left, cy - bodyHalfH);
        cone.lineTo(bodyRight, cy - bodyHalfH);
        cone.lineTo(coneRight, cy - coneHalfH);
        cone.lineTo(coneRight, cy + coneHalfH);
        cone.lineTo(bodyRight, cy + bodyHalfH);
        cone.lineTo(left, cy + bodyHalfH);
        cone.close();

        Paint conePaint = new Paint(fillPaint);
        conePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        conePaint.setStrokeJoin(Paint.Join.ROUND);
        conePaint.setStrokeWidth(Math.max(1.0f, s * 0.05f));
        canvas.drawPath(cone, conePaint);

        if (!muted) {
            RectF innerArc = new RectF(cx - s * 0.08f, cy - s * 0.18f, cx + s * 0.20f, cy + s * 0.18f);
            RectF outerArc = new RectF(cx - s * 0.18f, cy - s * 0.32f, cx + s * 0.34f, cy + s * 0.32f);
            canvas.drawArc(innerArc, -42f, 84f, false, strokePaint);
            canvas.drawArc(outerArc, -42f, 84f, false, strokePaint);
        } else {
            float x1 = cx + s * 0.12f;
            float x2 = cx + s * 0.34f;
            float y1 = cy - s * 0.11f;
            float y2 = cy + s * 0.11f;
            canvas.drawLine(x1, y1, x2, y2, strokePaint);
            canvas.drawLine(x2, y1, x1, y2, strokePaint);
        }
    }

    private void drawDiscoveryAdd(Canvas canvas, float cx, float cy, float s, boolean collection) {
        float u = s / 28f;
        strokePaint.setStrokeWidth(1.5f * u);
        if (collection) {
            Path back = new Path();
            back.moveTo(cx - 4 * u, cy - 8 * u);
            back.lineTo(cx + 7 * u, cy - 8 * u);
            back.quadTo(cx + 9 * u, cy - 8 * u, cx + 9 * u, cy - 6 * u);
            back.lineTo(cx + 9 * u, cy - 2 * u);
            canvas.drawPath(back, strokePaint);
            canvas.drawRoundRect(cx - 10 * u, cy - 5 * u, cx + 6 * u, cy + 7 * u,
                    2 * u, 2 * u, strokePaint);
            canvas.drawCircle(cx - 6 * u, cy - u, u, strokePaint);
            Path hills = new Path();
            hills.moveTo(cx - 9 * u, cy + 5 * u);
            hills.lineTo(cx - 3 * u, cy + u);
            hills.lineTo(cx + 5 * u, cy + 5 * u);
            canvas.drawPath(hills, strokePaint);
        } else {
            float x = cx - 3 * u;
            Path pin = new Path();
            pin.moveTo(x, cy + 8 * u);
            pin.cubicTo(x - 2 * u, cy + 5 * u, x - 6 * u, cy + u, x - 6 * u, cy - 2 * u);
            pin.cubicTo(x - 6 * u, cy - 10 * u, x + 6 * u, cy - 10 * u, x + 6 * u, cy - 2 * u);
            pin.cubicTo(x + 6 * u, cy + u, x + 2 * u, cy + 5 * u, x, cy + 8 * u);
            pin.close();
            canvas.drawPath(pin, strokePaint);
            canvas.drawCircle(x, cy - 2 * u, 1.7f * u, strokePaint);
        }
        drawPlus(canvas, cx + 10 * u, cy + 6 * u, 3 * u);
    }

    private void drawFolderPlus(Canvas canvas, float cx, float cy, float s) {
        Path folder = new Path();
        folder.moveTo(cx - s * 0.42f, cy + s * 0.30f);
        folder.lineTo(cx - s * 0.42f, cy - s * 0.30f);
        folder.lineTo(cx - s * 0.11f, cy - s * 0.30f);
        folder.lineTo(cx, cy - s * 0.19f);
        folder.lineTo(cx + s * 0.42f, cy - s * 0.19f);
        folder.lineTo(cx + s * 0.42f, cy + s * 0.30f);
        folder.close();
        canvas.drawPath(folder, strokePaint);
        drawPlus(canvas, cx, cy + s * 0.05f, s * 0.13f);
    }

    private void drawPlus(Canvas canvas, float x, float y, float half) {
        canvas.drawLine(x - half, y, x + half, y, strokePaint);
        canvas.drawLine(x, y - half, x, y + half, strokePaint);
    }

    private void drawPhoto(Canvas canvas, float cx, float cy, float s) {
        float half = s * 0.38f;
        float r = s * 0.14f;
        RectF frame = new RectF(cx - half, cy - half, cx + half, cy + half);
        canvas.drawRoundRect(frame, r, r, strokePaint);
        canvas.drawCircle(cx - half * 0.38f, cy - half * 0.36f, Math.max(1.2f, s * 0.075f), fillPaint);
        Path hills = new Path();
        hills.moveTo(cx - half * 0.78f, cy + half * 0.65f);
        hills.lineTo(cx - half * 0.20f, cy + half * 0.05f);
        hills.lineTo(cx + half * 0.18f, cy + half * 0.42f);
        hills.lineTo(cx + half * 0.46f, cy + half * 0.16f);
        hills.lineTo(cx + half * 0.80f, cy + half * 0.65f);
        canvas.drawPath(hills, strokePaint);
    }

    private void drawVideoBadge(Canvas canvas, float cx, float cy, float s) {
        float half = s * 0.38f;
        float r = s * 0.16f;
        RectF frame = new RectF(cx - half, cy - half, cx + half, cy + half);
        canvas.drawRoundRect(frame, r, r, strokePaint);
        drawPlay(canvas, cx + s * 0.02f, cy, s * 0.56f);
    }

    private void drawTrash(Canvas canvas, float cx, float cy, float s) {
        float u = s / 28f;
        strokePaint.setStrokeWidth(1.5f * u);
        float lidY = cy - 6f * u;
        float lidHalfW = 8f * u;
        canvas.drawLine(cx - lidHalfW, lidY, cx + lidHalfW, lidY, strokePaint);
        RectF handle = new RectF(cx - 3f * u, cy - 9f * u, cx + 3f * u, lidY);
        canvas.drawRoundRect(handle, 1.2f * u, 1.2f * u, strokePaint);
        RectF bin = new RectF(cx - 6f * u, lidY, cx + 6f * u, cy + 9f * u);
        canvas.drawRoundRect(bin, 2f * u, 2f * u, strokePaint);
        canvas.drawLine(cx - 2.2f * u, lidY + 3.5f * u, cx - 2.2f * u, cy + 6f * u, strokePaint);
        canvas.drawLine(cx + 2.2f * u, lidY + 3.5f * u, cx + 2.2f * u, cy + 6f * u, strokePaint);
    }

    private void drawSearch(Canvas canvas, float cx, float cy, float s) {
        float r = s * 0.25f;
        float lensCx = cx - s * 0.06f;
        float lensCy = cy - s * 0.06f;
        canvas.drawCircle(lensCx, lensCy, r, strokePaint);
        float startX = lensCx + r * 0.71f;
        float startY = lensCy + r * 0.71f;
        float endX = cx + s * 0.34f;
        float endY = cy + s * 0.34f;
        canvas.drawLine(startX, startY, endX, endY, strokePaint);
    }

    private void drawShare(Canvas canvas, float cx, float cy, float s) {
        float u = s / 28f;
        strokePaint.setStrokeWidth(1.5f * u);
        // 2D line share icon (upward arrow + open tray)
        float arrowTop = cy - 9f * u;
        float arrowBottom = cy + 2.5f * u;
        float wingW = 4.5f * u;
        float wingY = arrowTop + 4.5f * u;
        canvas.drawLine(cx, arrowBottom, cx, arrowTop, strokePaint);
        canvas.drawLine(cx - wingW, wingY, cx, arrowTop, strokePaint);
        canvas.drawLine(cx + wingW, wingY, cx, arrowTop, strokePaint);

        Path tray = new Path();
        float halfW = 7.5f * u;
        float trayTop = cy - 1f * u;
        float trayBottom = cy + 8f * u;
        tray.moveTo(cx - halfW, trayTop);
        tray.lineTo(cx - halfW, trayBottom);
        tray.lineTo(cx + halfW, trayBottom);
        tray.lineTo(cx + halfW, trayTop);
        canvas.drawPath(tray, strokePaint);
    }

    private void drawMore(Canvas canvas, float cx, float cy, float s) {
        float u = s / 28f;
        float r = 1.35f * u;
        float gap = 6f * u;
        canvas.drawCircle(cx, cy - gap, r, fillPaint);
        canvas.drawCircle(cx, cy, r, fillPaint);
        canvas.drawCircle(cx, cy + gap, r, fillPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        strokePaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        strokePaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

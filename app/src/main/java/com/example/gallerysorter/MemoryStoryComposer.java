package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composes a 9:16 (1080x1920) Instagram Story bitmap for either:
 * 1) Single-photo minimal frame (preserving original photo aspect ratio)
 * 2) Multi-photo vertical Photo Strip ("사진띠", 2~4 photos + dedicated footer caption zone)
 *
 * Reuses QuickBeauty's typography system (Sans / NanumPenScript / GowunBatangBold + textScale).
 */
final class MemoryStoryComposer {
    static final int STORY_WIDTH = 1080;
    static final int STORY_HEIGHT = 1920;
    static final int PREVIEW_WIDTH = 360;
    static final int PREVIEW_HEIGHT = 640;
    static final int MAX_STRIP_PHOTOS = 4;

    static final class Box {
        final float left;
        final float top;
        final float right;
        final float bottom;

        Box(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        float width() {
            return right - left;
        }

        float height() {
            return bottom - top;
        }
    }

    private MemoryStoryComposer() {
    }

    static Typeface resolveTypeface(Context context, MemoryStoryConfig.FontKind fontKind) {
        if (context == null || fontKind == null) {
            return Typeface.create("sans-serif", Typeface.BOLD);
        }
        try {
            if (fontKind == MemoryStoryConfig.FontKind.HAND) {
                if (Build.VERSION.SDK_INT >= 26) {
                    Typeface tf = context.getResources().getFont(R.font.nanum_pen_script);
                    if (tf != null) {
                        return tf;
                    }
                }
            } else if (fontKind == MemoryStoryConfig.FontKind.SERIF) {
                if (Build.VERSION.SDK_INT >= 26) {
                    Typeface tf = context.getResources().getFont(R.font.gowun_batang_bold);
                    if (tf != null) {
                        return tf;
                    }
                }
                return Typeface.create(Typeface.SERIF, Typeface.BOLD);
            }
        } catch (Throwable ignored) {
        }
        return Typeface.create("sans-serif", Typeface.BOLD);
    }

    /**
     * Computes the aspect-ratio-preserving photo box for SINGLE_FRAME mode in 1080x1920 space.
     */
    static Box computeSinglePhotoBox(int photoWidth, int photoHeight, MemoryStoryConfig config) {
        int safeW = Math.max(1, photoWidth);
        int safeH = Math.max(1, photoHeight);
        boolean hasText = config != null && config.hasVisibleText();
        float footer = singleFooterHeight(config);
        float maxW = 964f;
        float maxH = STORY_HEIGHT - 160f - 24f - footer;
        float fit = Math.min(maxW / safeW, maxH / safeH);
        float drawW = safeW * fit;
        float drawH = safeH * fit;
        float cardTop = (STORY_HEIGHT - drawH - 24f - footer) * .5f;
        return new Box((STORY_WIDTH - drawW) * .5f, cardTop + 24f,
                (STORY_WIDTH + drawW) * .5f, cardTop + 24f + drawH);
    }

    static float singleFooterHeight(MemoryStoryConfig config) {
        if (config == null || !config.hasVisibleText()) return 24f;
        boolean caption = !config.captionText.isEmpty(), date = !config.metaText.isEmpty();
        float captionSize = config.fontKind == MemoryStoryConfig.FontKind.HAND ? 68f : 52f;
        float metaSize = Math.min(40f, Math.max(20f, 31f * (.75f + .25f * config.textScale)));
        return 16f + 24f + (caption ? captionSize * config.textScale * 1.3f : 0)
                + (date ? metaSize * 1.3f : 0) + (caption && date ? 12f : 0);
    }

    static Box computeSingleCardBox(int width, int height, MemoryStoryConfig config) {
        Box photo = computeSinglePhotoBox(width, height, config);
        return new Box(photo.left - 24f, photo.top - 24f,
                photo.right + 24f, photo.bottom + singleFooterHeight(config));
    }

    static int singleBackgroundColor(MemoryStoryConfig config) {
        return config.themeTone == MemoryStoryConfig.ThemeTone.DARK ? 0xFF14161D : 0xFFE9E5EE;
    }

    static final class StripLayout {
        final Box card; final List<Box> slots;
        StripLayout(Box card,List<Box> slots){this.card=card;this.slots=Collections.unmodifiableList(slots);}
    }

    /** Photo strip: every photo fills the same inner width, stacked in selection order. */
    static StripLayout computeStripLayout(int[] widths,int[] heights,MemoryStoryConfig config) {
        if (MemoryStoryRowLayout.usesRow(config, widths.length)) {
            return MemoryStoryRowLayout.compute(widths, heights, config);
        }
        int count=Math.min(4,Math.min(widths.length,heights.length));
        if(count<2)throw new IllegalArgumentException("Strip requires 2–4 photos");
        float[] ratios=new float[count];
        for(int i=0;i<count;i++)ratios[i]=Math.max(1,widths[i])/(float)Math.max(1,heights[i]);
        float padding=24f,gap=24f,footer=singleFooterHeight(config);
        int rows=count;
        float factor=0;
        for(float ratio:ratios)factor+=1f/ratio;
        float available=1760f-padding-footer-gap*(rows-1);
        float width=Math.min(964f,available/factor);
        float photoHeight=width*factor+gap*(rows-1);
        float left=(STORY_WIDTH-width-padding*2)/2,top=(STORY_HEIGHT-photoHeight-padding-footer)/2;
        Box card=new Box(left,top,left+width+padding*2,top+photoHeight+padding+footer);
        List<Box> slots=new ArrayList<>();float y=top+padding;
        for(int i=0;i<count;i++) {
            float h=width/ratios[i];slots.add(new Box(left+padding,y,left+padding+width,y+h));y+=h+gap;
        }
        return new StripLayout(card,slots);
    }
    static StripLayout stripLayout(List<Bitmap> photos,MemoryStoryConfig config) {
        int count=Math.min(4,photos.size());int[] widths=new int[count],heights=new int[count];
        for(int i=0;i<count;i++){widths[i]=photos.get(i).getWidth();heights[i]=photos.get(i).getHeight();}
        return computeStripLayout(widths,heights,config);
    }
    static Box computeStripOuterBox(int count,MemoryStoryConfig config) {
        int n=Math.max(2,Math.min(4,count));int[] w=new int[n],h=new int[n];java.util.Arrays.fill(w,3);java.util.Arrays.fill(h,4);
        return computeStripLayout(w,h,config).card;
    }
    static List<Box> computeStripPhotoSlots(int count,MemoryStoryConfig config) {
        int n=Math.max(2,Math.min(4,count));int[] w=new int[n],h=new int[n];java.util.Arrays.fill(w,3);java.util.Arrays.fill(h,4);
        return computeStripLayout(w,h,config).slots;
    }

    static void draw(Context context,Canvas canvas,List<Bitmap> photos,MemoryStoryConfig config){
        if(config.layoutMode!=MemoryStoryConfig.LayoutMode.SINGLE_FRAME&&photos.size()>=2)drawPhotoStrip(context,canvas,photos,config);
        else drawSingleFrame(context,canvas,photos.isEmpty()?null:photos.get(0),config);
    }
    static Bitmap render(Context context, List<Bitmap> photos, MemoryStoryConfig config, int targetWidth) {
        int outW = Math.max(180, targetWidth);
        int outH = outW * 16 / 9;
        Bitmap output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.scale(outW / (float) STORY_WIDTH, outH / (float) STORY_HEIGHT);

        MemoryStoryConfig safeConfig = config == null
                ? new MemoryStoryConfig(MemoryStoryConfig.LayoutMode.SINGLE_FRAME, false, "", "",
                MemoryStoryConfig.FontKind.SANS, MemoryStoryConfig.ThemeTone.CREAM, 1.0f)
                : config;

        List<Bitmap> validPhotos = new ArrayList<>();
        if (photos != null) {
            for (Bitmap b : photos) {
                if (b != null && !b.isRecycled() && b.getWidth() > 0 && b.getHeight() > 0) {
                    validPhotos.add(b);
                }
            }
        }

        if (safeConfig.layoutMode != MemoryStoryConfig.LayoutMode.SINGLE_FRAME && validPhotos.size() >= 2) {
            drawPhotoStrip(context, canvas, validPhotos, safeConfig);
        } else {
            Bitmap single = validPhotos.isEmpty() ? null : validPhotos.get(0);
            drawSingleFrame(context, canvas, single, safeConfig);
        }
        return output;
    }

    /** Card-only transparent PNG for Instagram's sticker layer; never used as a View canvas. */
    static Bitmap renderCard(Context context, List<Bitmap> photos, MemoryStoryConfig config, int targetWidth) {
        return renderCard(context, photos, config, targetWidth, false);
    }

    static Bitmap renderCard(Context context, List<Bitmap> photos, MemoryStoryConfig config, int targetWidth,
                             boolean photoFrames) {
        if (photos == null || photos.isEmpty() || photos.get(0) == null || photos.get(0).isRecycled()) {
            throw new IllegalArgumentException("사진을 불러오지 못했어요.");
        }
        Bitmap photo = photos.get(0);
        MemoryStoryConfig single = config.withThemeTone(MemoryStoryConfig.ThemeTone.CREAM);
        boolean strip = single.layoutMode != MemoryStoryConfig.LayoutMode.SINGLE_FRAME && photos.size() >= 2;
        Box card = strip ? stripLayout(photos, single).card
                : computeSingleCardBox(photo.getWidth(), photo.getHeight(), single);
        float scale = Math.max(180, targetWidth) / (float) STORY_WIDTH;
        Bitmap result = Bitmap.createBitmap(Math.max(1, Math.round(card.width() * scale)),
                Math.max(1, Math.round(card.height() * scale)), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.scale(scale, scale);
        canvas.translate(-card.left, -card.top);
        if (strip) drawStripCard(context, canvas, photos, single, photoFrames);
        else drawSingleCard(context, canvas, photo, single);
        return result;
    }

    private static void drawSingleFrame(Context context,
                                        Canvas canvas,
                                        Bitmap photo,
                                        MemoryStoryConfig config) {
        // Paint every pixel; CLEAR on a hardware View can expose the viewer behind this dialog.
        canvas.drawColor(singleBackgroundColor(config));
        drawSingleCard(context, canvas, photo, config);
    }

    private static void drawSingleCard(Context context, Canvas canvas, Bitmap photo, MemoryStoryConfig config) {
        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        Box card = computeSingleCardBox(photo == null ? 1080 : photo.getWidth(),
                photo == null ? 1080 : photo.getHeight(), config);
        Paint paper = new Paint(Paint.ANTI_ALIAS_FLAG);
        paper.setColor(config.themeTone == MemoryStoryConfig.ThemeTone.DARK ? 0xFF252832 : Color.WHITE);
        RectF cardRect = new RectF(card.left, card.top, card.right, card.bottom);
        canvas.drawRoundRect(cardRect, 18f, 18f, paper);
        Box photoBox;
        if (photo != null) {
            photoBox = computeSinglePhotoBox(photo.getWidth(), photo.getHeight(), config);
            RectF dst = new RectF(photoBox.left, photoBox.top, photoBox.right, photoBox.bottom);
            Path clip = new Path();
            clip.addRoundRect(dst, 24f, 24f, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            Box positioned=MemoryStoryPhotoEdits.imageBox(photo.getWidth(),photo.getHeight(),photoBox,config.photoEdits.get(0));
            canvas.drawBitmap(photo,null,new RectF(positioned.left,positioned.top,positioned.right,positioned.bottom),bitmapPaint);
            canvas.restore();

        } else {
            photoBox = computeSinglePhotoBox(1080, 1080, config);
        }

        if (config.hasVisibleText()) {
            float footerTop = photoBox.bottom + 16f;
            Box footerBox = new Box(card.left + 24f, footerTop, card.right - 24f, card.bottom - 24f);
            drawFooterTextBlock(context, canvas, footerBox, config, false);
        }
    }

    private static void drawPhotoStrip(Context context,
                                       Canvas canvas,
                                       List<Bitmap> photos,
                                       MemoryStoryConfig config) {
        canvas.drawColor(singleBackgroundColor(config));
        drawStripCard(context, canvas, photos, config);
    }

    private static void drawStripCard(Context context, Canvas canvas, List<Bitmap> photos,
                                      MemoryStoryConfig config) {
        drawStripCard(context, canvas, photos, config, false);
    }

    private static void drawStripCard(Context context, Canvas canvas, List<Bitmap> photos,
                                      MemoryStoryConfig config, boolean photoFrames) {
        int count = Math.min(MAX_STRIP_PHOTOS, photos.size());
        StripLayout layout=stripLayout(photos,config);
        Box outer=layout.card;
        List<Box> slots=layout.slots;

        // Strip paper body
        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(Color.WHITE);
        RectF cardRect = new RectF(outer.left, outer.top, outer.right, outer.bottom);
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint);

        Paint cardBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBorder.setStyle(Paint.Style.STROKE);
        cardBorder.setStrokeWidth(2.5f);
        cardBorder.setColor(config.themeTone.borderColor);
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorder);

        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        for (int i = 0; i < count; i++) {
            Bitmap bmp = photos.get(i);
            Box slot = slots.get(i);
            canvas.save();
            canvas.rotate(MemoryStoryRowLayout.angle(config, i), (slot.left + slot.right) / 2,
                    (slot.top + slot.bottom) / 2);
            if (config.layoutMode == MemoryStoryConfig.LayoutMode.SCRAPBOOK) {
                float frameWidth = photoFrames ? 12f : 0f;
                Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
                frame.setColor(Color.WHITE);
                // Layered translucent outlines work on both hardware previews and bitmap canvases.
                for (int shadow = 6; shadow > 0; shadow--) {
                    frame.setColor(Color.argb(5, 30, 25, 40));
                    canvas.drawRoundRect(new RectF(slot.left - frameWidth - shadow, slot.top - frameWidth + 2 - shadow,
                            slot.right + frameWidth + shadow, slot.bottom + frameWidth + 2 + shadow), 5, 5, frame);
                }
                frame.setColor(Color.WHITE);
                if (photoFrames) canvas.drawRoundRect(new RectF(slot.left - 12, slot.top - 12,
                        slot.right + 12, slot.bottom + 12), 4, 4, frame);
            }
            drawCenterCropRounded(canvas, bmp, slot, 16f, bitmapPaint, config.themeTone.borderColor, config.photoEdits.get(i));
            canvas.restore();
        }

        if (config.hasVisibleText()) {
            Box footerBox = footerBox(outer, slots, config);
            if (MemoryStoryRowLayout.usesRow(config, count)) {
                drawRowFooter(context, canvas, footerBox, config, MemoryStoryRowLayout.footerScale(slots));
            } else {
                drawFooterTextBlock(context, canvas, footerBox, config, false);
            }
        }
    }

    static Box footerBox(Box card, List<Box> slots, MemoryStoryConfig config) {
        float top = MemoryStoryRowLayout.usesRow(config, slots.size())
                ? card.bottom - MemoryStoryRowLayout.footerHeight(config, MemoryStoryRowLayout.footerScale(slots)) - 8f
                : slots.get(slots.size() - 1).bottom + 16f;
        return new Box(card.left + 24f, top, card.right - 24f, card.bottom - 24f);
    }

    private static void drawCenterCropRounded(Canvas canvas,
                                              Bitmap bmp,
                                              Box slot,
                                              float radius,
                                              Paint bitmapPaint,
                                              int borderColor,MemoryStoryPhotoEdits.Transform transform) {
        if (bmp == null || bmp.isRecycled()) {
            return;
        }
        Box positioned=MemoryStoryPhotoEdits.imageBox(bmp.getWidth(),bmp.getHeight(),slot,transform);
        RectF clipRect = new RectF(slot.left, slot.top, slot.right, slot.bottom);
        Path clip = new Path();
        clip.addRoundRect(clipRect, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawBitmap(bmp, null, new RectF(positioned.left,positioned.top,positioned.right,positioned.bottom), bitmapPaint);
        canvas.restore();

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1.5f);
        border.setColor(borderColor);
        canvas.drawRoundRect(clipRect, radius, radius, border);
    }

    private static void drawFooterTextBlock(Context context,
                                            Canvas canvas,
                                            Box footerBox,
                                            MemoryStoryConfig config,
                                            boolean insideStrip) {
        String caption = config.captionText;
        String meta = config.metaText;
        boolean hasCaption = !caption.isEmpty();
        boolean hasMeta = !meta.isEmpty();
        if (!hasCaption && !hasMeta) {
            return;
        }
        if (MemoryStoryRowLayout.usesRow(config, 2)) {
            drawRowFooter(context, canvas, footerBox, config, 1f);
            return;
        }

        Typeface mainTypeface = resolveTypeface(context, config.fontKind);
        float baseCaptionSize;
        if (config.fontKind == MemoryStoryConfig.FontKind.HAND) {
            baseCaptionSize = insideStrip ? 60f : 68f;
        } else if (config.fontKind == MemoryStoryConfig.FontKind.SERIF) {
            baseCaptionSize = insideStrip ? 46f : 52f;
        } else {
            baseCaptionSize = insideStrip ? 46f : 52f;
        }
        float scaledCaptionSize = baseCaptionSize * config.textScale;

        Paint captionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        captionPaint.setTypeface(mainTypeface);
        captionPaint.setColor(config.themeTone.primaryTextColor);
        captionPaint.setTextAlign(insideStrip ? Paint.Align.CENTER : Paint.Align.RIGHT);
        captionPaint.setTextSize(scaledCaptionSize);

        float maxWidth = Math.max(120f, footerBox.width() - 16f);
        if (hasCaption) {
            while (captionPaint.getTextSize() > 22f && captionPaint.measureText(caption) > maxWidth) {
                captionPaint.setTextSize(captionPaint.getTextSize() - 1.5f);
            }
        }

        Paint metaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        metaPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        metaPaint.setColor(config.themeTone.secondaryTextColor);
        metaPaint.setTextAlign(insideStrip ? Paint.Align.CENTER : Paint.Align.RIGHT);
        float baseMetaSize = insideStrip ? 27f : 31f;
        metaPaint.setTextSize(Math.min(40f, Math.max(20f, baseMetaSize * (0.75f + 0.25f * config.textScale))));
        if (hasMeta) {
            while (metaPaint.getTextSize() > 18f && metaPaint.measureText(meta) > maxWidth) {
                metaPaint.setTextSize(metaPaint.getTextSize() - 1.0f);
            }
        }

        float cx = insideStrip ? (footerBox.left + footerBox.right) * 0.5f : footerBox.right - 8f;
        float cy = (footerBox.top + footerBox.bottom) * 0.5f;

        if (hasCaption && hasMeta) {
            float capH = captionPaint.descent() - captionPaint.ascent();
            float metaH = metaPaint.descent() - metaPaint.ascent();
            float gap = insideStrip ? 14f : 18f;
            float totalH = capH + gap + metaH;
            float startTop = cy - totalH * 0.5f;
            float capBaseline = startTop - captionPaint.ascent();
            float metaBaseline = startTop + capH + gap - metaPaint.ascent();
            canvas.drawText(caption, cx, capBaseline, captionPaint);
            canvas.drawText(meta, cx, metaBaseline, metaPaint);
        } else if (hasCaption) {
            float capH = captionPaint.descent() - captionPaint.ascent();
            float capBaseline = cy - capH * 0.5f - captionPaint.ascent();
            canvas.drawText(caption, cx, capBaseline, captionPaint);
        } else {
            float metaH = metaPaint.descent() - metaPaint.ascent();
            float metaBaseline = cy - metaH * 0.5f - metaPaint.ascent();
            canvas.drawText(meta, cx, metaBaseline, metaPaint);
        }
    }

    private static void drawRowFooter(Context context, Canvas canvas, Box box, MemoryStoryConfig config, float scale) {
        android.text.TextPaint caption = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
        caption.setTypeface(resolveTypeface(context, config.fontKind));
        caption.setColor(config.themeTone.primaryTextColor);
        caption.setTextSize(MemoryStoryRowLayout.captionTextSize(config, scale));
        caption.setTextAlign(Paint.Align.RIGHT);
        android.text.TextPaint date = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
        date.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        date.setColor(config.themeTone.secondaryTextColor);
        date.setTextSize(MemoryStoryRowLayout.dateTextSize(scale));
        date.setTextAlign(Paint.Align.RIGHT);
        float available = Math.max(1, box.width() - 16);
        String meta = !config.captionText.isEmpty() && !config.metaText.isEmpty() ? "· " + config.metaText : config.metaText;
        String dateText = android.text.TextUtils.ellipsize(meta, date,
                config.captionText.isEmpty() ? available : available * .45f,
                android.text.TextUtils.TruncateAt.END).toString();
        float dateWidth = date.measureText(dateText);
        float gap = !dateText.isEmpty() && !config.captionText.isEmpty() ? 20f : 0;
        float captionWidth = Math.max(1, available - dateWidth - gap);
        float measured = caption.measureText(config.captionText);
        if (measured > captionWidth) caption.setTextSize(Math.max(22f, caption.getTextSize() * captionWidth / measured));
        String title = android.text.TextUtils.ellipsize(config.captionText, caption, captionWidth,
                android.text.TextUtils.TruncateAt.END).toString();
        Paint baselinePaint = title.isEmpty() ? date : caption;
        float baseline = (box.top + box.bottom - baselinePaint.ascent() - baselinePaint.descent()) / 2;
        float right = box.right - 8;
        canvas.drawText(dateText, right, baseline, date);
        canvas.drawText(title, right - dateWidth - gap, baseline, caption);
    }
}

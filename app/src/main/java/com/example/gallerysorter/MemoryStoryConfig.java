package com.example.gallerysorter;

/**
 * Pure value configuration for Instagram Story 9:16 composition (Single Frame & Photo Strip).
 */
final class MemoryStoryConfig {
    enum LayoutMode {
        SINGLE_FRAME,
        PHOTO_STRIP,
        PORTRAIT_ROW,
        SCRAPBOOK
    }

    enum FontKind {
        SANS("기본"),
        HAND("손글씨"),
        SERIF("바탕체");

        final String label;

        FontKind(String label) {
            this.label = label;
        }
    }

    enum ThemeTone {
        CREAM("크림", 0xFFFAF8F5, 0xFF1E222B, 0xFF6E7585, 0xFFEAE6DF),
        DARK("다크", 0xFF14161D, 0xFFF5F6FA, 0xFFA5AEBE, 0xFF262A36);

        final String label;
        final int canvasColor;
        final int primaryTextColor;
        final int secondaryTextColor;
        final int borderColor;

        ThemeTone(String label, int canvasColor, int primaryTextColor, int secondaryTextColor, int borderColor) {
            this.label = label;
            this.canvasColor = canvasColor;
            this.primaryTextColor = primaryTextColor;
            this.secondaryTextColor = secondaryTextColor;
            this.borderColor = borderColor;
        }
    }

    final LayoutMode layoutMode;
    final boolean includeContext;
    final String captionText;
    final String metaText;
    final FontKind fontKind;
    final ThemeTone themeTone;
    final float textScale;
    final MemoryStoryPhotoEdits photoEdits;

    MemoryStoryConfig(LayoutMode layoutMode,
                      boolean includeContext,
                      String captionText,
                      String metaText,
                      FontKind fontKind,
                      ThemeTone themeTone,
                      float textScale) {
        this(layoutMode,includeContext,captionText,metaText,fontKind,themeTone,textScale,new MemoryStoryPhotoEdits());
    }
    private MemoryStoryConfig(LayoutMode layoutMode,boolean includeContext,String captionText,String metaText,
                              FontKind fontKind,ThemeTone themeTone,float textScale,MemoryStoryPhotoEdits edits) {
        this.photoEdits=edits;
        this.layoutMode = layoutMode == null ? LayoutMode.SINGLE_FRAME : layoutMode;
        this.includeContext = includeContext;
        this.captionText = captionText == null ? "" : captionText.trim();
        this.metaText = metaText == null ? "" : metaText.trim();
        this.fontKind = fontKind == null ? FontKind.SANS : fontKind;
        this.themeTone = themeTone == null ? ThemeTone.CREAM : themeTone;
        this.textScale = clampScale(textScale);
    }

    static float clampScale(float scale) {
        if (Float.isNaN(scale) || Float.isInfinite(scale)) {
            return 1.0f;
        }
        return Math.max(0.60f, Math.min(1.60f, scale));
    }

    boolean hasVisibleText() {
        return includeContext && (!captionText.isEmpty() || !metaText.isEmpty());
    }

    MemoryStoryConfig withLayoutMode(LayoutMode nextMode) {
        return new MemoryStoryConfig(nextMode, includeContext, captionText, metaText, fontKind, themeTone, textScale, photoEdits);
    }

    MemoryStoryConfig withIncludeContext(boolean nextInclude) {
        return new MemoryStoryConfig(layoutMode, nextInclude, captionText, metaText, fontKind, themeTone, textScale, photoEdits);
    }

    MemoryStoryConfig withCaptionText(String nextCaption) {
        return new MemoryStoryConfig(layoutMode, includeContext, nextCaption, metaText, fontKind, themeTone, textScale, photoEdits);
    }

    MemoryStoryConfig withMetaText(String nextMeta) {
        return new MemoryStoryConfig(layoutMode, includeContext, captionText, nextMeta, fontKind, themeTone, textScale, photoEdits);
    }

    MemoryStoryConfig withFontKind(FontKind nextFont) {
        return new MemoryStoryConfig(layoutMode, includeContext, captionText, metaText, nextFont, themeTone, textScale, photoEdits);
    }

    MemoryStoryConfig withThemeTone(ThemeTone nextTheme) {
        return new MemoryStoryConfig(layoutMode, includeContext, captionText, metaText, fontKind, nextTheme, textScale, photoEdits);
    }

    MemoryStoryConfig withTextScale(float nextScale) {
        return new MemoryStoryConfig(layoutMode, includeContext, captionText, metaText, fontKind, themeTone, nextScale, photoEdits);
    }

    MemoryStoryConfig withPhotoOrder(java.util.List<String> keys){return copyEdits(photoEdits.ordered(keys));}
    MemoryStoryConfig withPhotoEdit(int index,float zoom,float x,float y){return copyEdits(photoEdits.with(index,zoom,x,y));}
    private MemoryStoryConfig copyEdits(MemoryStoryPhotoEdits edits){return new MemoryStoryConfig(layoutMode,includeContext,captionText,metaText,fontKind,themeTone,textScale,edits);}

    static String formatPlaceAndDate(String placeText, String dateText) {
        String p = placeText == null ? "" : placeText.trim();
        String d = normalizeDateText(dateText);
        if (!p.isEmpty() && !d.isEmpty()) {
            return p + " · " + d;
        }
        return !p.isEmpty() ? p : d;
    }

    static String normalizeDateText(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        // Convert "2026-11-08" or "2026년 11월 8일" style into clean "2026.11.08" if applicable
        String digits = trimmed.replaceAll("[^0-9]", " ").trim();
        String[] parts = digits.split("\\s+");
        if (parts.length >= 3 && parts[0].length() == 4) {
            try {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                if (y >= 1970 && y <= 2100 && m >= 1 && m <= 12 && day >= 1 && day <= 31) {
                    return String.format(java.util.Locale.US, "%04d.%02d.%02d", y, m, day);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return trimmed;
    }
}

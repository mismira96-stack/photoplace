package com.example.gallerysorter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text labels preserve media units; legacy totals never invent a photo/video split. */
final class MemoryCardCountPresentation {
    private static final Pattern PHOTOS = Pattern.compile("사진 (\\d+)장(?: · 동영상 (\\d+)개)?");
    private static final Pattern VIDEOS = Pattern.compile("동영상 (\\d+)개");
    private static final Pattern TOTAL = Pattern.compile("사진·동영상 (\\d+)개");
    private static final String TOTAL_LABEL_PREFIX = "사진·동영상 합계 ";
    final String photos, videos, fallback, description;

    private MemoryCardCountPresentation(String photos, String videos, String fallback, String description) {
        this.photos = photos; this.videos = videos; this.fallback = fallback; this.description = description;
    }

    static MemoryCardCountPresentation from(String text) {
        String source = text == null ? "" : text.trim();
        Matcher photos = PHOTOS.matcher(source), videos = VIDEOS.matcher(source), total = TOTAL.matcher(source);
        try {
            if (photos.matches()) return new MemoryCardCountPresentation(number(photos.group(1)),
                    photos.group(2) == null ? "" : number(photos.group(2)), "", source);
            if (videos.matches()) return new MemoryCardCountPresentation("", number(videos.group(1)), "", source);
            if (total.matches()) return new MemoryCardCountPresentation("", "",
                    TOTAL_LABEL_PREFIX + number(total.group(1)) + "개", source);
        } catch (NumberFormatException outOfRange) { /* Unknown text must never invent a photo/video split. */ }
        return new MemoryCardCountPresentation("", "", source, source);
    }

    String displayText() {
        if (photos.isEmpty() && videos.isEmpty()) return fallback;
        String text = photos.isEmpty() ? "" : "사진 " + photos + "장";
        if (!videos.isEmpty()) text += (text.isEmpty() ? "" : " · ") + "동영상 " + videos + "개";
        return text;
    }

    String photoLabel() { return photos.isEmpty() ? "" : photos + "장"; }
    String videoLabel() { return videos.isEmpty() ? "" : videos + "개"; }

    String compactLabel() {
        if (!photos.isEmpty()) return photoLabel();
        if (!videos.isEmpty()) return videoLabel();
        return fallback.startsWith(TOTAL_LABEL_PREFIX) ? fallback.substring(TOTAL_LABEL_PREFIX.length()) : fallback;
    }

    private static String number(String value) { return String.format(Locale.KOREA, "%,d", Long.parseLong(value)); }
}

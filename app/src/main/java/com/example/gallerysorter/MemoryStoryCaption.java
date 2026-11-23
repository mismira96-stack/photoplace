package com.example.gallerysorter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Display-only Story labels. Never rewrites capture times, EXIF or saved notes. */
final class MemoryStoryCaption {
    private MemoryStoryCaption() { }
    static String place(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.isEmpty() || value.endsWith("에서") ? value : value + "에서";
    }
    static String date(String raw) {
        String value = raw == null ? "" : raw.trim();
        Matcher match = Pattern.compile("^(?:\\d{4}\\s*(?:년|[.\\-/])\\s*)?(\\d{1,2})\\s*(?:월|[.\\-/])\\s*(\\d{1,2})\\s*일?\\.?$").matcher(value);
        if (!match.matches()) return value;
        int month = Integer.parseInt(match.group(1)), day = Integer.parseInt(match.group(2));
        return month >= 1 && month <= 12 && day >= 1 && day <= 31
                ? month + "월 " + day + "일" : value;
    }
}

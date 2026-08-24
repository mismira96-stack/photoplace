package com.example.gallerysorter;

import android.net.Uri;

final class MediaAnalysisSignature {
    private MediaAnalysisSignature() {
    }

    static String build(Uri uri, String displayName, long modifiedSeconds, long addedSeconds, long mediaTakenMillis, boolean video, String sourceFolderKey) {
        return build(uri == null ? "" : uri.toString(), displayName, modifiedSeconds, addedSeconds, mediaTakenMillis, video, sourceFolderKey);
    }

    static String build(String uri, String displayName, long modifiedSeconds, long addedSeconds, long mediaTakenMillis, boolean video, String sourceFolderKey) {
        return (video ? "v|" : "i|")
                + safe(uri)
                + "|" + safe(displayName)
                + "|" + modifiedSeconds
                + "|" + addedSeconds
                + "|" + mediaTakenMillis
                + "|" + safe(sourceFolderKey);
    }

    static String buildForNoLocation(Uri uri, String displayName, long modifiedSeconds, long addedSeconds, long mediaTakenMillis, boolean video) {
        return build(uri, displayName, modifiedSeconds, addedSeconds, mediaTakenMillis, video, "");
    }

    static String buildForNoLocation(String uri, String displayName, long modifiedSeconds, long addedSeconds, long mediaTakenMillis, boolean video) {
        return build(uri, displayName, modifiedSeconds, addedSeconds, mediaTakenMillis, video, "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

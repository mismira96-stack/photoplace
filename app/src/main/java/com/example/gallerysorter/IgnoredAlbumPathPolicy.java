package com.example.gallerysorter;

import java.util.Locale;

/**
 * Pure policy deciding whether a relative_path should be excluded from destination place album candidates.
 */
final class IgnoredAlbumPathPolicy {
    static final String CAMERA_PATH = "DCIM/Camera/";

    private IgnoredAlbumPathPolicy() {
    }

    static boolean isIgnoredAlbumPath(String relativePath) {
        if (relativePath == null) {
            return true;
        }
        String lowerCase = relativePath.toLowerCase(Locale.US);
        return CAMERA_PATH.equals(relativePath)
                || lowerCase.contains("/quickbeauty/")
                || lowerCase.contains("/screenshots/")
                || lowerCase.contains("/screenshot/")
                || lowerCase.contains("/kakaotalk/")
                || lowerCase.contains("/instagram/")
                || lowerCase.contains("/photogrid/")
                || lowerCase.contains("/download/")
                || relativePath.startsWith("Pictures/사진정리");
    }
}

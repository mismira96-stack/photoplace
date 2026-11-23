package com.example.gallerysorter;

import java.util.function.Consumer;

/** Explicit viewer video action; swiping must never launch another app. */
final class MemoryViewerVideoAction {
    private MemoryViewerVideoAction() { }

    static boolean isTap(float dx, float dy, float slop) {
        return Math.abs(dx) <= slop && Math.abs(dy) <= slop;
    }

    static boolean open(MemoryPhotoItem item, Consumer<MemoryPhotoItem> external) {
        if (item == null || item.mediaKind != MediaKind.VIDEO || item.sourceUri.isEmpty()) return false;
        external.accept(item);
        return true;
    }
}

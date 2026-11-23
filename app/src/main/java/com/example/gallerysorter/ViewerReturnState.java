package com.example.gallerysorter;

/** One in-process screen, reusable only while its source revision is unchanged. */
final class ViewerReturnState<T> {
    private T screen;
    private String revision;

    void remember(T value, String sourceRevision) {
        clear();
        if (sourceRevision != null && !sourceRevision.isEmpty()) {
            screen = value;
            revision = sourceRevision;
        }
    }

    boolean hasScreen() { return screen != null; }

    T take(String currentRevision) {
        T result = revision != null && revision.equals(currentRevision) ? screen : null;
        clear();
        return result;
    }

    void clear() {
        screen = null;
        revision = null;
    }
}

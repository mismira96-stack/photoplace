package com.example.gallerysorter;

import java.util.function.Consumer;

/** Keep the displayed photo until its replacement is ready; never leave it as a failed result. */
final class ViewerImageTransition<T> {
    private final boolean hasDisplayedImage;
    private final T preview;
    private final Consumer<T> show;
    private final Runnable placeholder;

    ViewerImageTransition(boolean hasDisplayedImage, T preview, Consumer<T> show, Runnable placeholder) {
        this.hasDisplayedImage = hasDisplayedImage;
        this.preview = preview;
        this.show = show;
        this.placeholder = placeholder;
    }

    void pending() {
        if (!hasDisplayedImage) failed();
    }

    void ready(T image) {
        if (image != null) show.accept(image); else failed();
    }

    void failed() {
        if (preview != null) show.accept(preview); else placeholder.run();
    }
}

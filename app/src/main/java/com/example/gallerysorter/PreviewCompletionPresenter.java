package com.example.gallerysorter;

final class PreviewCompletionPresenter {
    private PreviewCompletionPresenter() { }

    static void present(int newPlaceCount, Runnable newPlaces, Runnable existingRecords) {
        if (newPlaceCount > 0) newPlaces.run();
        else existingRecords.run();
    }
}

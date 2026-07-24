package com.example.gallerysorter;

interface MediaScanProgressListener {
    void onItemScanned(boolean video, int displayCurrent, int displayTotal,
                       int progressCurrent, int progressTotal, PhotoItem item);

    void onScanError(boolean video, Exception error);
}

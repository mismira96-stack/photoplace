package com.example.gallerysorter;

interface SortJobProgressListener {
    void onItem(int current, int total, PhotoItem item);
}

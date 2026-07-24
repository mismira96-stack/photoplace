package com.example.gallerysorter;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

final class SortJobResult {
    final List<Uri> sortedUris = new ArrayList<>();
    final List<Uri> copiedOriginalUris = new ArrayList<>();
    final StringBuilder log = new StringBuilder();
    int copiedCount;
    int skippedCount;
    int failedCount;
    boolean canceled;
}

package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MemoryGroup {
    final String countryName;
    final String title;
    final List<MemoryItem> items;
    final int itemCount;
    final String startDate;
    final String endDate;
    final String thumbnailUri;

    MemoryGroup(String countryName, String title, List<MemoryItem> items) {
        this.countryName = countryName == null ? "" : countryName;
        this.title = title == null ? this.countryName : title;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        int count = 0;
        String firstDate = null;
        String lastDate = null;
        String representativeThumbnail = null;
        for (MemoryItem item : items) {
            count += Math.max(0, item.itemCount);
            if (item.startDate != null && !item.startDate.isEmpty() && (firstDate == null || item.startDate.compareTo(firstDate) < 0)) {
                firstDate = item.startDate;
            }
            if (item.endDate != null && !item.endDate.isEmpty() && (lastDate == null || item.endDate.compareTo(lastDate) > 0)) {
                lastDate = item.endDate;
            }
            if ((representativeThumbnail == null || representativeThumbnail.isEmpty()) && item.thumbnailUri != null && !item.thumbnailUri.isEmpty()) {
                representativeThumbnail = item.thumbnailUri;
            }
        }
        this.itemCount = count;
        this.startDate = firstDate;
        this.endDate = lastDate;
        this.thumbnailUri = representativeThumbnail;
    }
}

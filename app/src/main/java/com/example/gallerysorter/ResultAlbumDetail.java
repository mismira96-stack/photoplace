package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.List;

final class ResultAlbumDetail {
    final String groupKey;
    final DateRange dateRange;
    final List<PhotoItem> previewItems;
    int totalCount;
    int videoCount;

    ResultAlbumDetail(String groupKey) {
        this.groupKey = groupKey;
        this.dateRange = new DateRange();
        this.previewItems = new ArrayList<>();
    }
}

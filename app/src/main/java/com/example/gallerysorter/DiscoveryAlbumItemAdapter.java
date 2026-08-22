package com.example.gallerysorter;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Android boundary that converts a reviewed discovery plan into SortWorker items. */
final class DiscoveryAlbumItemAdapter {
    private DiscoveryAlbumItemAdapter() {
    }

    static List<PhotoItem> toPhotoItems(List<DiscoveryAlbumOrganizer.PreparedItem> preparedItems) {
        if (preparedItems == null || preparedItems.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<PhotoItem> items = new ArrayList<>();
        for (DiscoveryAlbumOrganizer.PreparedItem item : preparedItems) {
            if (item == null || item.sourceUri == null || item.sourceUri.trim().isEmpty()) {
                continue;
            }
            items.add(new PhotoItem(
                    Uri.parse(item.sourceUri),
                    item.displayName,
                    item.mimeType,
                    item.takenAtMillis > 0L ? new Date(item.takenAtMillis) : null,
                    item.locationKey,
                    false,
                    item.targetExists,
                    item.duplicateInTarget,
                    item.targetRelativePath,
                    item.video,
                    item.countryCode,
                    item.countryName,
                    item.adminArea,
                    item.addressLine));
        }
        return items;
    }
}

package com.example.gallerysorter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Counts displayable projection entries; album-only summaries have no media-kind split. */
final class MemoryMediaCounts {
    final int photos;
    final int videos;

    private MemoryMediaCounts(int photos, int videos) {
        this.photos = photos;
        this.videos = videos;
    }

    static MemoryMediaCounts fromRefs(List<DiscoveryPhotoRef> refs) {
        int photos = 0, videos = 0;
        Set<String> seen = new HashSet<>();
        if (refs != null) for (DiscoveryPhotoRef ref : refs) {
            if (ref == null || ref.stale || ref.sourceUri == null
                    || ref.sourceUri.trim().isEmpty() || !seen.add(ref.sourceUri)) continue;
            if (ref.mediaKind == MediaKind.VIDEO) videos++; else photos++;
        }
        return new MemoryMediaCounts(photos, videos);
    }

    static MemoryMediaCounts fromSections(List<MemoryPhotoSection> sections) {
        int photos = 0, videos = 0;
        if (sections != null) for (MemoryPhotoSection section : sections) {
            if (section == null) continue;
            for (MemoryPhotoItem item : section.photos) {
                if (item == null || item.sourceUri.isEmpty()) continue;
                if (item.mediaKind == MediaKind.VIDEO) videos++; else photos++;
            }
        }
        return new MemoryMediaCounts(photos, videos);
    }

    String text() {
        if (videos == 0) return "사진 " + photos + "장";
        if (photos == 0) return "동영상 " + videos + "개";
        return "사진 " + photos + "장 · 동영상 " + videos + "개";
    }

    static String forRecord(MemoryRecord record) {
        if (record.sourceType == MemorySourceType.DISCOVERED_ONLY && record.discoveryGroup != null) {
            return fromRefs(record.discoveryGroup.photoRefs).text();
        }
        if (record.sourceType == MemorySourceType.MIXED
                && (record.organizedAlbum == null || record.organizedAlbum.mediaSplitKnown)) {
            return new MemoryMediaCounts(record.photoCount, record.videoCount).text();
        }
        if (record.organizedAlbum != null && record.organizedAlbum.mediaSplitKnown) {
            return new MemoryMediaCounts(record.organizedAlbum.photoCount, record.organizedAlbum.videoCount).text();
        }
        // Legacy album history does not retain the photo/video split.
        return "사진·동영상 " + record.itemCount + "개";
    }
}

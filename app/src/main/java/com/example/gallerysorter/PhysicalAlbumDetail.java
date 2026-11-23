package com.example.gallerysorter;

import java.util.Collections;

/** Exact physical-folder projection into the common detail; never appends discovery media. */
final class PhysicalAlbumDetail {
    static MemoryBrowserDetail build(StoredAlbumSummary album, MemoryRepository unified,
                                      MemoryMediaResolver.GalleryReader reader) {
        return buildFromRecords(album, unified.memories(), reader);
    }

    static MemoryBrowserDetail buildFromRecords(StoredAlbumSummary album, java.util.List<MemoryRecord> records,
                                                 MemoryMediaResolver.GalleryReader reader) {
        MemoryRepository physical = new MemoryRepository(null, Collections.singletonList(album));
        if (physical.memories().isEmpty()) return null;
        MemoryRecord source = physical.memories().get(0);
        String key = source.memoryKey;
        for (MemoryRecord record : records) {
            if (record.organizedAlbum != null
                    && normalized(record.organizedAlbum.relativePath).equals(normalized(album.relativePath))) {
                key = record.memoryKey;
                break;
            }
        }
        // Reuse the known identity for notes/personal places, but only this folder's media.
        MemoryRecord record = new MemoryRecord(key, source.placeKey, source.title,
                source.canonicalPlaceName, album.albumName, source.countryCode, source.countryName,
                source.adminArea, source.addressLine, source.itemCount, source.photoCount, source.videoCount,
                source.startDateMillis, source.endDateMillis, source.coverUri, MemorySourceType.ORGANIZED_ALBUM,
                null, source.organizedAlbum, 0, source.availableCount, true, true, false, false);
        MemoryMediaResolution media = MemoryMediaResolver.resolve(record, null, reader);
        MemoryBrowserDetail detail = MemoryBrowserDetail.from(MemoryBrowserItem.from(record), record, media.refs);
        return media.source == MemoryMediaSourcePolicy.Source.UNAVAILABLE ? detail.withUnavailableCount() : detail;
    }

    private static String normalized(String path) {
        if (path == null) return "";
        return path.trim().replace('\\', '/').replaceAll("/+$", "");
    }
    private PhysicalAlbumDetail() { }
}

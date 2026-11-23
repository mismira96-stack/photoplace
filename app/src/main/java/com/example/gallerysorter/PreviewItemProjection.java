package com.example.gallerysorter;

import android.net.Uri;
import java.util.List;
import java.util.Locale;

/** Preserves Preview targeting while using the run-local target lookup cache. */
final class PreviewItemProjection {
    static PhotoItem build(Uri uri, String name, String mime, LocationResult location,
                           boolean video, List<AlbumFolder> albums,
                           DiscoveryAlbumOrganizer.AlbumLookup lookup) throws Exception {
        boolean noLocation = LocationLookupResult.LOCATION_NONE.equals(location.folderKey);
        AlbumFolder match = noLocation ? null : matchingAlbum(location.folderKey, albums);
        String target = noLocation ? "" : match == null
                ? "Pictures/" + location.folderKey + "에서/" : match.relativePath;
        boolean duplicate = !noLocation && lookup.find(target, name, video).duplicateExists;
        return new PhotoItem(uri, name, mime, location.takenAt, location.folderKey, noLocation,
                match != null, duplicate, target, video, location.countryCode, location.countryName,
                location.adminArea, location.addressLine);
    }

    static AlbumFolder matchingAlbum(String place, List<AlbumFolder> albums) {
        String key = place == null ? "" : place.replaceAll("\\s+", "").toLowerCase(Locale.KOREA);
        AlbumFolder best = null;
        int score = -1;
        for (AlbumFolder album : albums) {
            if (!album.matchName.contains(key)) continue;
            int next = album.matchName.equals(key + "에서") || album.matchName.equals(key) ? 100 : 10;
            if (album.matchName.startsWith(key)) next += 20;
            if (next > score) { best = album; score = next; }
        }
        return best;
    }
}

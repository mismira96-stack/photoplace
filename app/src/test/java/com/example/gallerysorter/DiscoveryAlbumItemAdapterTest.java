package com.example.gallerysorter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscoveryAlbumItemAdapterTest {
    @Test
    public void excludesVideosFromDiscoveryWorkerInputWhenVideosAreDisabled() {
        DiscoveryAlbumOrganizer.PreparedItem photo = preparedItem(MediaKind.PHOTO, "content://photo/1");
        DiscoveryAlbumOrganizer.PreparedItem video = preparedItem(MediaKind.VIDEO, "content://video/1");

        assertTrue(DiscoveryAlbumItemAdapter.shouldInclude(photo, false));
        assertFalse(DiscoveryAlbumItemAdapter.shouldInclude(video, false));
        assertTrue(DiscoveryAlbumItemAdapter.shouldInclude(video, true));
    }

    @Test
    public void excludesPreparedItemsWithoutUsableSourceUri() {
        assertFalse(DiscoveryAlbumItemAdapter.shouldInclude(
                preparedItem(MediaKind.PHOTO, "  "), true));
        assertFalse(DiscoveryAlbumItemAdapter.shouldInclude(null, true));
    }

    private static DiscoveryAlbumOrganizer.PreparedItem preparedItem(MediaKind kind, String uri) {
        DiscoveryPhotoRef ref = new DiscoveryPhotoRef(
                uri, 1L, kind, kind == MediaKind.VIDEO ? "video/mp4" : "image/jpeg",
                "item", 1L, "place", "Place", "", "", "", "", "", 1L, 1L, false);
        return new DiscoveryAlbumOrganizer.PreparedItem(
                ref, "place", "Pictures/Place/",
                new DiscoveryAlbumOrganizer.Match(true, false));
    }
}

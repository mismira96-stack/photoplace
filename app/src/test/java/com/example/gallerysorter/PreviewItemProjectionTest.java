package com.example.gallerysorter;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class PreviewItemProjectionTest {
    @Test public void exactAlbumWinsOverPartialNameAndPreservesDuplicateFlag() throws Exception {
        AlbumFolder partial = new AlbumFolder("Pictures/성남여행/", "성남여행", "성남여행");
        AlbumFolder exact = new AlbumFolder("Pictures/성남에서/", "성남에서", "성남에서");
        PhotoItem item = PreviewItemProjection.build(null, "old.jpg", "image/jpeg",
                new LocationResult(null, "성남", "KR", "대한민국", "", ""), false,
                Arrays.asList(partial, exact), new DiscoveryAlbumOrganizer.AlbumLookup() {
                    public String resolveTargetRelativePath(String place, String path) { return path; }
                    public DiscoveryAlbumOrganizer.Match find(String path, String name, boolean video) {
                        assertEquals(exact.relativePath, path);
                        return new DiscoveryAlbumOrganizer.Match(true, true);
                    }
                });
        assertTrue(item.targetExists);
        assertTrue(item.duplicateInTarget);
        assertEquals(exact.relativePath, item.targetRelativePath);
    }

    @Test public void noLocationRemainsInPreviewWithoutQueryingTarget() throws Exception {
        PhotoItem item = PreviewItemProjection.build(null, "video.mp4", "video/mp4",
                new LocationResult(null, LocationLookupResult.LOCATION_NONE, "", "", ""), true,
                Collections.emptyList(), null);
        assertTrue(item.noLocation);
        assertTrue(item.video);
        assertFalse(item.duplicateInTarget);
        assertEquals("", item.targetRelativePath);
    }
}

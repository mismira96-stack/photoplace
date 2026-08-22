package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DiscoveryAlbumOrganizerTest {
    @Test
    public void prepareBuildsExistingSortWorkerInputAndKeepsDuplicatesSkippable() throws Exception {
        MemoryRecord record = MemoryRepository.fromDiscoveryGroup(group(
                "삿포로",
                Arrays.asList(ref("101", "IMG_101.jpg", MediaKind.PHOTO), ref("102", "VID_102.mp4", MediaKind.VIDEO))));

        DiscoveryAlbumOrganizer.Preparation result = DiscoveryAlbumOrganizer.prepare(
                Collections.singletonList(record),
                lookup(true));

        assertEquals(1, result.placeCount);
        assertEquals(2, result.items.size());
        assertEquals(1, result.copyableCount);
        assertEquals(1, result.duplicateCount);
        assertEquals("Pictures/삿포로에서/", result.items.get(0).targetRelativePath);
        assertTrue(result.items.get(0).targetExists);
        assertTrue(result.items.get(0).duplicateInTarget);
        assertTrue(result.items.get(1).video);
        assertFalse(result.items.get(1).duplicateInTarget);
    }

    @Test
    public void prepareDeduplicatesSameSourceAcrossGroups() throws Exception {
        DiscoveryPhotoRef shared = ref("201", "IMG_201.jpg", MediaKind.PHOTO);
        MemoryRecord first = MemoryRepository.fromDiscoveryGroup(group("수원", Collections.singletonList(shared)));
        MemoryRecord second = MemoryRepository.fromDiscoveryGroup(group("성남", Collections.singletonList(shared)));

        DiscoveryAlbumOrganizer.Preparation result = DiscoveryAlbumOrganizer.prepare(
                Arrays.asList(first, second),
                lookup(false));

        assertEquals(1, result.items.size());
        assertEquals(1, result.placeCount);
        assertEquals(1, result.skippedRefCount);
    }

    @Test
    public void duplicateSignatureTreatsCopySuffixAsSameFile() {
        assertEquals(
                MediaStoreAlbumLookup.fileSignature("IMG_100.jpg"),
                MediaStoreAlbumLookup.fileSignature("IMG_100 (2).jpg"));
    }

    private static DiscoveryAlbumOrganizer.AlbumLookup lookup(final boolean existing) {
        return new DiscoveryAlbumOrganizer.AlbumLookup() {
            @Override
            public String resolveTargetRelativePath(String placeName, String proposedRelativePath) {
                return proposedRelativePath;
            }

            @Override
            public DiscoveryAlbumOrganizer.Match find(String path, String name, boolean video) {
                return new DiscoveryAlbumOrganizer.Match(existing, existing && name.startsWith("IMG"));
            }
        };
    }

    private static DiscoveryMemoryGroup group(String place, java.util.List<DiscoveryPhotoRef> refs) {
        return new DiscoveryMemoryGroup(
                "discovery:" + place,
                place,
                place,
                "KR",
                "대한민국",
                "경기도",
                place,
                refs.size(),
                refs.size(),
                0,
                1785600000000L,
                1785600000000L,
                refs.get(0).sourceUri,
                refs,
                0,
                7L);
    }

    private static DiscoveryPhotoRef ref(String id, String name, MediaKind kind) {
        return new DiscoveryPhotoRef(
                "content://media/external/" + (kind == MediaKind.VIDEO ? "video/media/" : "images/media/") + id,
                Long.parseLong(id),
                kind,
                kind == MediaKind.VIDEO ? "video/mp4" : "image/jpeg",
                name,
                1785600000000L,
                "삿포로",
                "삿포로",
                "KR",
                "대한민국",
                "경기도",
                "삿포로",
                "",
                7L,
                7L,
                false);
    }
}

package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class MemoryBrowserCountProjectionTest {
    private static final long TAKEN = 1795046400000L;

    @Test public void legacyMixedAlbumDoesNotDoubleCountVideosInSummary() {
        List<DiscoveryPhotoRef> actual = new ArrayList<>();
        for (int i=0; i<100; i++) actual.add(ref(i, i<10 ? MediaKind.VIDEO : MediaKind.PHOTO, true));
        List<DiscoveryPhotoRef> discovery = new ArrayList<>();
        for (int i=0; i<10; i++) discovery.add(ref(i, MediaKind.VIDEO, false));
        MemoryRepository repository = repository(100, discovery);
        MemoryRecord original = repository.memories().get(0);
        assertEquals(100, original.itemCount);
        assertEquals("사진·동영상 100개", MemoryMediaCounts.forRecord(original));
        int[] reads = {0};
        MemoryBrowserEntry entry = entry(repository, actual, reads);
        assertEquals(100, entry.summary.photoCount);
        assertEquals(100, entry.records.get(0).photoCount);
        assertEquals(10, entry.records.get(0).videoCount);
        assertEquals("사진 90장 · 동영상 10개", entry.state.items.get(0).countText);
        assertEquals(original.memoryKey, entry.records.get(0).memoryKey);
        assertSame(original.discoveryGroup, entry.records.get(0).discoveryGroup);
        assertEquals(100, original.photoCount); // No mutation of the repository's estimate.
        assertEquals(1, reads[0]); // Summary reuses the card's already-resolved media.
    }

    @Test public void photoOnlyAlbumPlusNewVideoCountsTheNewFileExactlyOnce() {
        MemoryRepository repository = repository(2, Collections.singletonList(ref(30, MediaKind.VIDEO, false)));
        MemoryBrowserEntry entry = entry(repository, Arrays.asList(ref(1, MediaKind.PHOTO, true),
                ref(2, MediaKind.PHOTO, true)), new int[]{0});
        assertEquals(3, entry.summary.photoCount);
        assertEquals("사진 2장 · 동영상 1개", entry.state.items.get(0).countText);
    }

    @Test public void deniedReadKeepsTheEstimateAndUnavailableStateInsteadOfInventingAZero() {
        MemoryRepository repository = repository(100, Collections.singletonList(ref(1, MediaKind.VIDEO, false)));
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true, Collections.emptyList(),
                Collections.emptyMap(), (path, record) -> MemoryMediaStoreReader.Result.failed());
        assertEquals(100, entry.summary.photoCount);
        assertEquals("불러오기 필요", entry.state.items.get(0).countText);
        assertFalse(entry.complete());
    }

    @Test public void albumOnlyAndEmptyListsDoNotAddMediaQueries() {
        int[] reads = {0};
        MemoryBrowserEntry albums = entry(repository(7, Collections.emptyList()), Collections.emptyList(), reads);
        assertEquals(7, albums.summary.photoCount);
        assertEquals(0, reads[0]);
        MemoryBrowserEntry empty = entry(new MemoryRepository(null, Collections.emptyList()), Collections.emptyList(), reads);
        assertTrue(empty.state.isEmpty());
        assertEquals(0, empty.summary.photoCount);
        assertEquals(0, reads[0]);
    }

    @Test public void discoveryWithoutAnyAlbumsCanOpenAndPrepareItsFirstAlbum() {
        DiscoveryPhotoRef photo = ref(1, MediaKind.PHOTO, false);
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup("discovery:first", "수원", "수원", "KR", "대한민국",
                "경기도", "수원", 1, 1, 0, TAKEN, TAKEN, photo.sourceUri, Collections.singletonList(photo), 0, 1);
        MemoryRepository repository = new MemoryRepository(new DiscoverySnapshot(1, 1, 1, "first", 1,
                Collections.singletonList(group), "", ""), Collections.emptyList());
        MemoryMediaResolver.GalleryReader noAlbumReader = (path, record) -> {
            fail("Discovery-only state must not query any album folder"); return null;
        };
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true, Collections.emptyList(),
                Collections.emptyMap(), noAlbumReader);
        assertEquals(1, entry.state.items.size());
        assertEquals(1, entry.summary.photoCount);
        assertTrue(entry.complete());
        MemoryBrowserDetail detail = entry.state.detail(entry.state.items.get(0).memoryKey, repository, noAlbumReader);
        assertTrue(detail.canOrganize);
        assertTrue(detail.canOpenPhotos);
        assertFalse(detail.canOpenGalleryAlbum);
        assertEquals(Collections.singletonList(photo.sourceUri), detail.sourceUris);
    }

    private static MemoryBrowserEntry entry(MemoryRepository repository, List<DiscoveryPhotoRef> actual, int[] reads) {
        return MemoryBrowserEntry.build(repository, true, Collections.emptyList(), Collections.emptyMap(), (path, record) -> {
            reads[0]++;
            return MemoryMediaStoreReader.Result.found(actual);
        });
    }
    private static MemoryRepository repository(int total, List<DiscoveryPhotoRef> refs) {
        StoredAlbumSummary album = new StoredAlbumSummary("수원에서", "Pictures/수원에서/", total,
                "2026-11-19", "2026-11-19", "content://cover", "", 0, "KR", "대한민국", "경기도", "수원");
        DiscoverySnapshot snapshot = null;
        if (!refs.isEmpty()) {
            int videos = 0; for (DiscoveryPhotoRef ref : refs) if (ref.mediaKind == MediaKind.VIDEO) videos++;
            DiscoveryMemoryGroup group = new DiscoveryMemoryGroup("discovery:수원", "수원", "수원", "KR", "대한민국",
                    "경기도", "수원", refs.size(), refs.size()-videos, videos, TAKEN, TAKEN,
                    refs.get(0).sourceUri, refs, 0, 1);
            snapshot = new DiscoverySnapshot(1, 1, 1, "test", refs.size(), Collections.singletonList(group), "", "");
        }
        return new MemoryRepository(snapshot, Collections.singletonList(album));
    }
    private static DiscoveryPhotoRef ref(int id, MediaKind kind, boolean output) {
        return new DiscoveryPhotoRef("content://"+(output ? "output/" : "source/")+id, id, kind,
                kind == MediaKind.VIDEO ? "video/mp4" : "image/jpeg", "file"+id,
                TAKEN, "수원", "수원", "KR", "대한민국", "경기도", "수원",
                output ? "Pictures/수원에서/" : "DCIM/Camera/", 1, 1, false);
    }
}

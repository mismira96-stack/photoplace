package com.example.gallerysorter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PhysicalAlbumDetailTest {
    private static final StoredAlbumSummary ALBUM = new StoredAlbumSummary("수원에서", "Pictures/수원에서/", 1,
            "2026-11-19", "2026-11-19", "content://cover", "", 0, "KR", "대한민국", "경기도", "수원");

    @Test public void albumScopeNeverAppendsNewDiscoveryPhotosAndKeepsExistingIdentity() {
        DiscoveryPhotoRef old = ref(1, "Pictures/수원에서/");
        DiscoveryPhotoRef fresh = ref(2, "DCIM/Camera/");
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup("discovery:수원", "수원", "수원", "KR", "대한민국",
                "경기도", "수원", 2, 2, 0, old.takenAtMillis, fresh.takenAtMillis,
                fresh.sourceUri, Arrays.asList(old, fresh), 0, 1);
        DiscoverySnapshot snapshot = new DiscoverySnapshot(1, 1, 1, "test", 2,
                Collections.singletonList(group), "", "");
        MemoryRepository unified = new MemoryRepository(snapshot, Collections.singletonList(ALBUM));
        assertEquals(1, unified.memories().size());
        MemoryRecord merged = unified.memories().get(0);
        MemoryBrowserDetail all = MemoryBrowserState.from(unified).detail(merged.memoryKey, unified,
                (path, record) -> MemoryMediaStoreReader.Result.found(Collections.singletonList(old)));
        assertEquals(2, all.sourceUris.size());
        MemoryBrowserDetail album = PhysicalAlbumDetail.build(ALBUM, unified, (path, record) -> {
            assertEquals(ALBUM.relativePath, path);
            assertNull(record.discoveryGroup);
            return MemoryMediaStoreReader.Result.found(Collections.singletonList(old));
        });
        assertEquals(Collections.singletonList(old.sourceUri), album.sourceUris);
        assertEquals(merged.memoryKey, album.record.memoryKey);
        assertEquals("수원에서", album.item.title);
        assertEquals("사진 1장", album.item.countText);
        assertTrue(album.canOpenPhotos);
        assertFalse(album.canOrganize);
    }

    @Test public void deniedAlbumDoesNotFallBackToDiscoveryOrInventPhotos() {
        MemoryRepository repository = new MemoryRepository(null, Collections.singletonList(ALBUM));
        MemoryBrowserDetail result = PhysicalAlbumDetail.build(ALBUM, repository,
                (path, record) -> MemoryMediaStoreReader.Result.failed());
        assertEquals("불러오기 필요", result.item.countText);
        assertFalse(result.mediaResolved);
        assertTrue(result.sourceUris.isEmpty());
    }

    @Test public void videoOnlyAlbumUsesTheCommonVideoSections() {
        DiscoveryPhotoRef video = new DiscoveryPhotoRef("content://media/external/video/media/3", 3,
                MediaKind.VIDEO, "video/mp4", "video.mp4", 1795046400000L,
                "수원", "수원", "KR", "대한민국", "경기도", "수원", ALBUM.relativePath, 1, 1, false);
        MemoryBrowserDetail result = PhysicalAlbumDetail.build(ALBUM,
                new MemoryRepository(null, Collections.singletonList(ALBUM)),
                (path, record) -> MemoryMediaStoreReader.Result.found(Collections.singletonList(video)));
        assertEquals("동영상 1개", result.item.countText);
        assertEquals(Collections.singletonList(video.sourceUri), result.sourceUris);
        assertTrue(result.canOpenPhotos);
    }

    @Test public void albumsWithTheSameNameKeepTheirExactFolderAndWorkWithoutDiscoveryCache() {
        StoredAlbumSummary other = new StoredAlbumSummary("수원에서", "Pictures/other/", 1,
                "2026-11-19", "2026-11-19", "content://other", "", 0, "", "", "", "");
        List<MemoryRecord> records = new MemoryRepository(null, Arrays.asList(other, ALBUM)).memories();
        for (List<MemoryRecord> known : Arrays.asList(records, Collections.<MemoryRecord>emptyList())) {
            MemoryBrowserDetail detail = PhysicalAlbumDetail.buildFromRecords(ALBUM, known, (path, record) -> {
                assertEquals(ALBUM.relativePath, path);
                return MemoryMediaStoreReader.Result.found(Collections.singletonList(ref(1, path)));
            });
            assertEquals("path:Pictures/수원에서/", detail.record.memoryKey);
            assertEquals(ALBUM.relativePath, detail.record.organizedAlbum.relativePath);
        }
    }

    private static DiscoveryPhotoRef ref(long id, String path) {
        return new DiscoveryPhotoRef("content://media/external/images/media/" + id, id, MediaKind.PHOTO,
                "image/jpeg", "photo" + id + ".jpg", 1795046400000L + id,
                "수원", "수원", "KR", "대한민국", "경기도", "수원", path, 1, 1, false);
    }
}

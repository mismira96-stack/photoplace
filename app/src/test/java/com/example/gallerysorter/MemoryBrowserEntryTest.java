package com.example.gallerysorter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryBrowserEntryTest {
    @Test public void detailReusesCurrentBrowserEntryAndKeepsTheSelectedPlacesSources() {
        MemoryRepository repository = repository(Collections.singletonList(album("A")),
                group("A", ref("content://p/a", MediaKind.PHOTO, false)),
                group("B", ref("content://p/b", MediaKind.PHOTO, false)));
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true,
                Collections.emptyList(), aliases(), null).withRevision("current");
        MemoryMediaResolver.GalleryReader reader = (path, record) -> {
            assertTrue(path.startsWith("Pictures/A"));
            return MemoryMediaStoreReader.Result.found(Collections.singletonList(
                    ref("content://p/album-a", MediaKind.PHOTO, false)));
        };

        for (MemoryBrowserItem item : entry.state.items) {
            MemoryBrowserDetail existing = entry.state.detail(item.memoryKey, repository, reader);
            MemoryBrowserDetail reused = entry.detailForRevision(item.memoryKey, "current", reader);
            assertNotNull(reused);
            assertEquals(existing.record.memoryKey, reused.record.memoryKey);
            assertEquals(existing.sourceUris, reused.sourceUris);
            assertEquals(existing.item.countText, reused.item.countText);
            assertEquals(existing.canOpenGalleryAlbum, reused.canOpenGalleryAlbum);
        }
    }

    @Test public void detailSkipsStaleOrUnverifiedBrowserEntryBeforeReadingMedia() {
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository(Collections.emptyList(),
                        group("A", ref("content://p/a", MediaKind.PHOTO, false))), true,
                Collections.emptyList(), aliases(), null);
        MemoryMediaResolver.GalleryReader reader = (path, record) -> {
            throw new AssertionError("Media read for a stale or missing card");
        };
        assertNull(entry.detailForRevision("discovery:A", "", reader));
        assertNull(entry.withRevision("old").detailForRevision("discovery:A", "current", reader));
        assertNull(entry.withRevision("current").detailForRevision("discovery:missing", "current", reader));
    }

    private DiscoveryPhotoRef ref(String uri, MediaKind kind, boolean stale) {
        return new DiscoveryPhotoRef(uri, 1L, kind, "", uri, 100L, "A", "A", "KR", "대한민국",
                "경기", "", "DCIM/Camera/", 1L, 1L, stale);
    }

    private DiscoveryMemoryGroup group(String place, DiscoveryPhotoRef... refs) {
        return groupWithVersion(place, 1L, refs);
    }

    private DiscoveryMemoryGroup groupWithVersion(String place, long version, DiscoveryPhotoRef... refs) {
        return new DiscoveryMemoryGroup("discovery:" + place, place, place, "KR", "대한민국", "경기", "",
                refs.length, refs.length, 0, 100L, 100L, "", Arrays.asList(refs), 0, version);
    }

    private DiscoveryMemoryGroup groupWithDates(String place, long start, long end, DiscoveryPhotoRef... refs) {
        return new DiscoveryMemoryGroup("discovery:" + place, place, place, "KR", "대한민국", "경기", "",
                refs.length, refs.length, 0, start, end, "", Arrays.asList(refs), 0, 1L);
    }

    @Test public void seenPlaceHidesNewAndAddedCountEvenFromCachedEntryWithoutChangingTotals() {
        MemoryRepository repository = repository(Collections.emptyList(), group("A", ref("content://p/a", MediaKind.PHOTO, false)),
                group("B", ref("content://p/b", MediaKind.PHOTO, false)));
        MemoryBrowserEntry original = MemoryBrowserEntry.build(repository, false, Collections.emptyList(), aliases(), null);
        MemoryBrowserEntry seen = original.withSeenVersions(record -> MemorySeenPolicy.key(record).equals("discovery:A") ? 1 : -1);
        assertEquals(1, original.state.item("discovery:A").recentAddedCount);
        assertEquals(0, seen.state.item("discovery:A").recentAddedCount);
        assertEquals(1, seen.state.item("discovery:B").recentAddedCount);
        assertEquals(original.state.item("discovery:A").countText, seen.state.item("discovery:A").countText);
        assertSame(original.records, seen.records);
        assertEquals(0, seen.state.retainingRecords(Collections.singletonList(original.records.get(0)))
                .item("discovery:A").recentAddedCount);
    }

    @Test public void newSnapshotWithFreshPhotoShowsNewAgainButDuplicateStaleAndBlankRefsDoNot() {
        DiscoveryPhotoRef fresh = new DiscoveryPhotoRef("content://p/fresh", 2L, MediaKind.PHOTO, "image/jpeg",
                "fresh.jpg", 100L, "A", "A", "KR", "대한민국", "경기", "", "DCIM/Camera/", 2L, 2L, false);
        MemoryRepository repository = repository(Collections.emptyList(), groupWithVersion("A", 2L,
                ref("content://p/old", MediaKind.PHOTO, false), fresh, fresh,
                ref("", MediaKind.PHOTO, false), ref("content://p/stale", MediaKind.PHOTO, true)));
        MemoryRecord record = repository.discoveryMemories().get(0);
        assertEquals(1, MemorySeenPolicy.recentCount(record, 1));
        assertEquals(0, MemorySeenPolicy.recentCount(record, 2));
        assertEquals(0, MemorySeenPolicy.recentCount(record, 3));
    }

    @Test public void acknowledgementKeyIsSameForMixedAndDiscoveryRegardlessOfAlbumPath() {
        MemoryRepository repository = repository(Collections.singletonList(album("A")),
                group("A", ref("content://p/a", MediaKind.PHOTO, false)));
        MemoryRecord mixed = repository.memories().get(0);
        MemoryRecord discovery = repository.discoveryMemories().get(0);
        assertEquals(MemorySourceType.MIXED, mixed.sourceType);
        assertNotEquals(mixed.memoryKey, discovery.memoryKey);
        assertEquals(MemorySeenPolicy.key(mixed), MemorySeenPolicy.key(discovery));
    }

    @Test public void onlyResolvedBrowsableDetailCanBeAcknowledged() {
        MemoryRepository repository = repository(Collections.emptyList(), group("A", ref("content://p/a", MediaKind.PHOTO, false)));
        MemoryBrowserDetail detail = MemoryBrowserState.from(repository).detail("discovery:A", repository, null);
        assertTrue(MemorySeenPolicy.canAcknowledge(detail));
        assertFalse(MemorySeenPolicy.canAcknowledge(detail.withUnavailableCount()));
        assertFalse(MemorySeenPolicy.canAcknowledge(null));
        MemoryRepository empty = repository(Collections.emptyList(), group("empty"));
        assertFalse(MemorySeenPolicy.canAcknowledge(MemoryBrowserState.from(empty).detail("discovery:empty", empty, null)));
    }

    private MemoryRepository repository(List<StoredAlbumSummary> albums, DiscoveryMemoryGroup... groups) {
        DiscoverySnapshot snapshot = new DiscoverySnapshot(1, 1L, 1L, "test", 0, Arrays.asList(groups), "", "");
        return new MemoryRepository(snapshot, albums);
    }

    private StoredAlbumSummary album(String place) {
        return new StoredAlbumSummary(place + "에서", "Pictures/" + place + "에서/", 10,
                "", "", "", "", 1L, "KR", "대한민국", "경기", "");
    }

    private MemoryCollection collection() {
        return new MemoryCollection("group_trip", "여행", Arrays.asList(
                new MemoryCollection.Member("mem_A", "discovery:A"),
                new MemoryCollection.Member("mem_B", "discovery:B")), 1L, 1L);
    }

    private Map<String, String> aliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("discovery:A", "mem_A"); aliases.put("discovery:B", "mem_B");
        aliases.put("path:Pictures/A에서/", "mem_A"); aliases.put("path:Pictures/B에서/", "mem_B");
        return aliases;
    }

    @Test public void browserAndCollectionsPreserveCountsAndSkipDuplicateMissingAndStaleUris() {
        DiscoveryPhotoRef shared = ref("content://p/1", MediaKind.PHOTO, false);
        MemoryRepository repository = repository(Collections.emptyList(),
                group("A", shared, ref("content://v/2", MediaKind.VIDEO, false)),
                group("B", shared, ref("", MediaKind.PHOTO, false), ref("content://p/deleted", MediaKind.PHOTO, true)));
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, false,
                Collections.singletonList(collection()), aliases(), (path, record) -> { fail("No album to query"); return null; });
        assertEquals(2, entry.records.size());
        assertEquals(2, entry.state.items.size());
        MemoryCollectionCardSummary summary = entry.collectionSummaries.get("group_trip");
        assertEquals(1, summary.photoCount);
        assertEquals(1, summary.videoCount);
        assertEquals("content://p/1", summary.coverUri);
        assertTrue(entry.complete());
    }

    @Test public void galleryOnlyCollectionUsesActualOutputsOncePerMemberRatherThanStoredTotals() {
        MemoryRepository repository = repository(Arrays.asList(album("A"), album("B")));
        int[] reads = {0};
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true,
                Collections.singletonList(collection()), aliases(), (path, record) -> {
                    reads[0]++;
                    return MemoryMediaStoreReader.Result.found(Collections.singletonList(
                            ref("content://gallery/" + record.memoryKey, MediaKind.PHOTO, false)));
                });
        assertEquals(2, reads[0]);
        assertEquals(2, entry.collectionSummaries.get("group_trip").photoCount);
        assertTrue(entry.complete());
    }

    @Test public void failedGalleryOrUnresolvedMemberPreventsBrowserCacheValidation() throws Exception {
        MemoryRepository repository = repository(Arrays.asList(album("A"), album("B")));
        MemoryBrowserEntry failed = MemoryBrowserEntry.build(repository, true,
                Collections.singletonList(collection()), aliases(), (path, record) -> MemoryMediaStoreReader.Result.failed());
        assertFalse(failed.complete());
        MemoryBrowserEntry missingMember = MemoryBrowserEntry.build(repository, false,
                Collections.singletonList(collection()), aliases(), (path, record) -> MemoryMediaStoreReader.Result.missing());
        assertFalse(missingMember.complete());
        MemoryDetailLoadSession<MemoryBrowserEntry> cache = new MemoryDetailLoadSession<>(MemoryBrowserEntry::complete);
        assertEquals("", cache.read(cache.begin(), "browser", () -> "same", () -> failed).revision);
        MemoryBrowserEntry recovered = MemoryBrowserEntry.build(repository, true, Collections.emptyList(), aliases(), null);
        assertSame(recovered, cache.read(cache.begin(), "browser", () -> "same", () -> recovered).value);
    }

    @Test public void sourceScopeRemainsExplicitAndUnrelatedLegacyAlbumsAreNotQueriedForCards() {
        MemoryRepository repository = repository(Collections.singletonList(album("A")),
                group("B", ref("content://p/1", MediaKind.PHOTO, false)));
        int[] reads = {0};
        MemoryMediaResolver.GalleryReader reader = (path, record) -> { reads[0]++; return MemoryMediaStoreReader.Result.missing(); };
        assertEquals(1, MemoryBrowserEntry.build(repository, false, Collections.emptyList(), aliases(), reader).records.size());
        assertEquals(0, reads[0]);
        assertEquals(2, MemoryBrowserEntry.build(repository, true, Collections.emptyList(), aliases(), reader).records.size());
        assertEquals(0, reads[0]);
    }

    @Test public void mixedAndDiscoveryCardsUseSamePhotoVideoTextAndDetailCounts() {
        DiscoveryPhotoRef shared = ref("content://p/shared", MediaKind.PHOTO, false);
        MemoryRepository repository = repository(Collections.singletonList(album("A")),
                group("A", shared, ref("content://v/new", MediaKind.VIDEO, false)),
                group("B", ref("content://p/b", MediaKind.PHOTO, false), ref("content://v/b", MediaKind.VIDEO, false)));
        MemoryMediaResolver.GalleryReader reader = (path, record) -> MemoryMediaStoreReader.Result.found(Arrays.asList(
                shared, ref("content://p/gallery", MediaKind.PHOTO, false), shared));
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true, Collections.emptyList(), aliases(), reader);
        MemoryBrowserItem mixed = null, discovery = null;
        for (MemoryBrowserItem item : entry.state.items) {
            if (item.sourceType == MemorySourceType.MIXED) mixed = item;
            else discovery = item;
            assertEquals(entry.state.detail(item.memoryKey, repository, reader).item.countText, item.countText);
        }
        assertNotNull(mixed);
        assertNotNull(discovery);
        assertEquals("사진 2장 · 동영상 1개", mixed.countText);
        assertEquals("사진 1장 · 동영상 1개", discovery.countText);
        MemoryBrowserState visible = entry.state.retainingRecords(Collections.singletonList(entry.records.get(0)));
        assertEquals(1, visible.items.size());
        assertEquals(mixed.countText, visible.items.get(0).countText);
        assertEquals(mixed.recentAddedCount, visible.items.get(0).recentAddedCount);
        assertTrue(entry.state.retainingRecords(Collections.emptyList()).isEmpty());
        assertEquals(10, entry.records.get(0).itemCount); // Preserve stored history; never add aggregate estimates.
    }

    @Test public void failedAlbumCountWithoutCollectionsIsUnknownAndCannotValidateCache() {
        MemoryRepository repository = repository(Collections.singletonList(album("A")),
                group("A", ref("content://p/a", MediaKind.PHOTO, false)));
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true, Collections.emptyList(), aliases(),
                (path, record) -> MemoryMediaStoreReader.Result.failed());
        assertEquals("불러오기 필요", entry.state.items.get(0).countText);
        assertFalse(entry.complete());
    }

    @Test public void actualAlbumCountIsSharedWithCollectionSummaryInSameEntryBuild() {
        MemoryRepository repository = repository(Arrays.asList(album("A"), album("B")),
                group("A", ref("content://p/a", MediaKind.PHOTO, false)),
                group("B", ref("content://p/b", MediaKind.PHOTO, false)));
        int[] reads = {0};
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true, Collections.singletonList(collection()),
                aliases(), (path, record) -> {
                    reads[0]++;
                    return MemoryMediaStoreReader.Result.found(Collections.singletonList(ref(
                            "content://gallery/" + record.memoryKey, MediaKind.VIDEO, false)));
                });
        assertEquals(2, reads[0]);
        assertEquals("사진 1장 · 동영상 1개", entry.state.items.get(0).countText);
        assertEquals(2, entry.collectionSummaries.get("group_trip").videoCount);
    }

    @Test public void unchangedTabReentryDoesNotRepeatGalleryQueriesForCollectionCards() throws Exception {
        MemoryRepository repository = repository(Arrays.asList(album("A"), album("B")));
        int[] reads = {0};
        java.util.concurrent.Callable<MemoryBrowserEntry> source = () -> MemoryBrowserEntry.build(repository, true,
                Collections.singletonList(collection()), aliases(), (path, record) -> {
                    reads[0]++;
                    return MemoryMediaStoreReader.Result.found(Collections.singletonList(
                            ref("content://gallery/" + record.memoryKey, MediaKind.PHOTO, false)));
                });
        MemoryDetailLoadSession<MemoryBrowserEntry> cache = new MemoryDetailLoadSession<>(MemoryBrowserEntry::complete);
        MemoryBrowserEntry first = cache.read(cache.begin(), "browser|albums", () -> "same", source).value;
        assertSame(first, cache.read(cache.begin(), "browser|albums", () -> "same", source).value);
        assertEquals(2, reads[0]);
        cache.read(cache.begin(), "browser|albums", () -> "changed", source);
        assertEquals(4, reads[0]);
    }

    @Test public void matchesComparesItemKeysCountsAndTitlesAccurately() {
        MemoryRepository repo = repository(Collections.emptyList(), group("A", ref("content://p/a", MediaKind.PHOTO, false)));
        MemoryBrowserEntry first = MemoryBrowserEntry.build(repo, false, Collections.emptyList(), aliases(), null);
        MemoryBrowserEntry second = MemoryBrowserEntry.build(repo, false, Collections.emptyList(), aliases(), null);
        assertTrue(first.matches(second));
        assertTrue(second.matches(first));

        MemoryRepository differentRepo = repository(Collections.emptyList(), group("B", ref("content://p/b", MediaKind.PHOTO, false)));
        MemoryBrowserEntry different = MemoryBrowserEntry.build(differentRepo, false, Collections.emptyList(), aliases(), null);
        assertFalse(first.matches(different));
        assertFalse(first.matches(null));
    }

    @Test public void matchesDetectsCollectionTitleAndMemberChanges() {
        MemoryRepository repo = repository(Collections.emptyList(), group("A", ref("content://p/a", MediaKind.PHOTO, false)),
                group("B", ref("content://p/b", MediaKind.PHOTO, false)));
        MemoryCollection c1 = new MemoryCollection("group_trip", "여행", Arrays.asList(
                new MemoryCollection.Member("mem_A", "discovery:A"),
                new MemoryCollection.Member("mem_B", "discovery:B")), 1L, 1L);
        MemoryCollection c2 = new MemoryCollection("group_trip", "제주도 여행", Arrays.asList(
                new MemoryCollection.Member("mem_A", "discovery:A"),
                new MemoryCollection.Member("mem_B", "discovery:B")), 1L, 2L);

        MemoryBrowserEntry first = MemoryBrowserEntry.build(repo, false, Collections.singletonList(c1), aliases(), null);
        MemoryBrowserEntry second = MemoryBrowserEntry.build(repo, false, Collections.singletonList(c2), aliases(), null);

        assertFalse(first.matches(second));
        assertFalse(second.matches(first));
    }

    @Test public void matchesDetectsShootingDateChanges() {
        DiscoveryPhotoRef photo = ref("content://p/a", MediaKind.PHOTO, false);
        MemoryRepository repo1 = repository(Collections.emptyList(), groupWithDates("A", 1000000000L, 1000000000L, photo));
        MemoryRepository repo2 = repository(Collections.emptyList(), groupWithDates("A", 1700000000000L, 1700000000000L, photo));

        MemoryBrowserEntry first = MemoryBrowserEntry.build(repo1, false, Collections.emptyList(), aliases(), null);
        MemoryBrowserEntry second = MemoryBrowserEntry.build(repo2, false, Collections.emptyList(), aliases(), null);

        assertNotEquals(first.state.item("discovery:A").dateText, second.state.item("discovery:A").dateText);
        assertFalse(first.matches(second));
        assertFalse(second.matches(first));
    }
}

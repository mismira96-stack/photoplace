package com.example.gallerysorter;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HomeRecentPlacesResolverTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void mixedPlaceHasOnePlaceNamedCardAndColdHomeCanOpenIt() throws Exception {
        DiscoverySnapshot snapshot = snapshot(group("성남", 200L), group("송파구", 300L));
        List<StoredAlbumSummary> albums = Arrays.asList(album("성남"), album("송파구"));
        MemoryRepository repository = new MemoryRepository(snapshot, albums);
        List<HomeRecentPlacesResolver.Card> cards = HomeRecentPlacesResolver.build(repository, null);
        assertEquals(2, cards.size());
        assertEquals("송파구", cards.get(0).title);
        assertEquals("성남", cards.get(1).title);
        assertEquals("path:Pictures/성남에서/", cards.get(1).memoryKey);

        DiscoverySnapshotStore store = new DiscoverySnapshotStore(folder.newFolder());
        assertTrue(store.save(snapshot));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, null);
        for (HomeRecentPlacesResolver.Card card : cards) {
            // Reproduce the old default-false Home lookup, then test the explicit Home target.
            assertNull(controller.loadBrowserDetail(card.memoryKey, albums, false));
            MemoryBrowserDetail detail = controller.loadBrowserDetail(
                    card.memoryKey, albums, card.includeOrganizedSources);
            assertNotNull(detail);
            assertEquals(card.memoryKey, detail.item.memoryKey);
            assertEquals(card.title, detail.item.title);
            assertEquals(1, detail.sourceUris.size());
        }
    }

    @Test public void albumsAloneNeverFillDiscoveryRow() {
        assertTrue(HomeRecentPlacesResolver.build(new MemoryRepository(null,
                Collections.singletonList(album("성남"))), null).isEmpty());
        assertTrue(HomeRecentPlacesResolver.build(null, null).isEmpty());
    }

    @Test public void latestMediaWinsAndOnlyFourDiscoveryCardsAreReturned() {
        MemoryRepository repository = new MemoryRepository(snapshot(group("old", 100L),
                group("newest", 500L), group("second", 400L), group("third", 300L),
                group("fourth", 200L)), Collections.singletonList(album("unrelated")));
        List<HomeRecentPlacesResolver.Card> cards = HomeRecentPlacesResolver.build(repository, null);
        assertEquals(4, cards.size());
        assertEquals("newest", cards.get(0).title);
        assertEquals("fourth", cards.get(3).title);
    }

    @Test public void singleDiscoveryDoesNotBackfillAlbumCards() {
        List<HomeRecentPlacesResolver.Card> cards = HomeRecentPlacesResolver.build(
                new MemoryRepository(snapshot(group("성남", 200L)),
                        Arrays.asList(album("성남"), album("other"))), null);
        assertEquals(1, cards.size());
        assertEquals("성남", cards.get(0).title);
    }

    @Test public void exactGalleryAndNewDiscoveryUseResolvedTotalAndNewestPhoto() {
        DiscoveryPhotoRef old = ref("성남", "old.jpg", 100L);
        DiscoveryPhotoRef fresh = ref("성남", "new.jpg", 300L);
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup("discovery:성남", "성남", "성남",
                "KR", "대한민국", "경기", "", 2, 2, 0, 100L, 300L, old.sourceUri,
                Arrays.asList(old, fresh), 0, 1L);
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("discovery:성남", "mem_seongnam");
        aliases.put("path:Pictures/성남에서/", "mem_seongnam");
        OrganizationLink link = new OrganizationLink("link", OrganizationLink.SubjectType.MEMORY,
                "mem_seongnam", "request", "성남에서", "Pictures/성남에서/", 1L,
                OrganizationLink.Status.SUCCESS, 1, 0, 0);
        MemoryRepository repository = new MemoryRepository(snapshot(group),
                Collections.singletonList(album("성남")), aliases, Collections.singletonList(link));
        MemoryMediaResolver.GalleryReader reader = (path, memory) -> MemoryMediaStoreReader.Result.found(
                Collections.singletonList(ref("gallery", "old.jpg", 100L)));
        HomeRecentPlacesResolver.Card card = HomeRecentPlacesResolver.build(repository, reader).get(0);
        assertEquals("사진 2장", card.countText);
        assertEquals(fresh.sourceUri, card.coverUri);
        MemoryBrowserDetail detail = MemoryBrowserState.from(repository).detail(card.memoryKey, repository, reader);
        assertEquals(2, detail.sourceUris.size());
    }

    private static StoredAlbumSummary album(String place) {
        return new StoredAlbumSummary(place + "에서", "Pictures/" + place + "에서/", 1,
                "", "", "content://album/" + place, "", 1L, "KR", "대한민국", "경기", "");
    }

    private static DiscoveryMemoryGroup group(String place, long time) {
        DiscoveryPhotoRef ref = ref(place, "photo.jpg", time);
        return new DiscoveryMemoryGroup("discovery:" + place, place, place, "KR", "대한민국",
                "경기", "", 1, 1, 0, time, time, ref.sourceUri, Collections.singletonList(ref), 0, 1L);
    }

    private static DiscoveryPhotoRef ref(String place, String file, long time) {
        return new DiscoveryPhotoRef("content://media/" + place + "/" + file, time, MediaKind.PHOTO,
                "image/jpeg", file, time, place, place, "KR", "대한민국", "경기", "", "DCIM/Camera/",
                0L, 0L, false);
    }

    private static DiscoverySnapshot snapshot(DiscoveryMemoryGroup... groups) {
        return new DiscoverySnapshot(1, 1L, 1L, "test", groups.length, Arrays.asList(groups), "", "");
    }
}

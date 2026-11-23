package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class AutomaticCountryGroupsTest {
    private static final long TAKEN = 1795392000000L;

    @Test public void knownForeignCountriesAreGroupedWhileKoreaAndUnknownRemainInTheList() {
        MemoryBrowserEntry entry = entry(repository(group("tokyo", "도쿄", "JP"), group("osaka", "오사카", "JP"),
                group("melbourne", "멜버른", "AU"), group("seoul", "서울", "KR"), group("unknown", "미확인", "")));
        assertEquals(2, entry.countries.groups.size());
        AutomaticCountryGroups.Group japan = country(entry, "JP");
        assertEquals("일본", japan.title);
        assertEquals("country:JP", japan.key);
        assertEquals(2, japan.records.size());
        assertEquals("사진·동영상 2개", japan.summary.countText);
        assertEquals(2, AutomaticCountryGroups.domesticAndUnknown(entry.records).size());
        assertEquals(5, entry.state.items.size()); // Search retains the original complete source.
        assertEquals(1, MemoryBrowserSearch.filter(entry.records, "도쿄").size());
        assertTrue(entry.collections.isEmpty()); // No fake user collection was persisted or inserted.
    }

    @Test public void aSingleForeignPlaceGetsACountryCardAndEmptyListsStayEmpty() {
        MemoryBrowserEntry entry = entry(repository(group("guam", "투몬", "GU")));
        assertEquals(1, entry.countries.groups.size());
        assertEquals("괌", entry.countries.groups.get(0).title);
        assertTrue(AutomaticCountryGroups.domesticAndUnknown(entry.records).isEmpty());
        assertTrue(entry(new MemoryRepository(null, Collections.emptyList())).countries.groups.isEmpty());
    }

    @Test public void albumOnlyCountriesUseExistingCountsWithoutStartingExtraFolderReads() {
        StoredAlbumSummary album = album("도쿄", "JP", 8);
        int[] reads = {0};
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(new MemoryRepository(null, Collections.singletonList(album)),
                true, Collections.emptyList(), Collections.emptyMap(), (path, record) -> {
                    reads[0]++; return MemoryMediaStoreReader.Result.failed();
                });
        assertEquals(0, reads[0]);
        assertEquals(1, entry.countries.groups.size());
        assertEquals("사진·동영상 8개", entry.countries.groups.get(0).summary.countText);
        assertTrue(entry.countries.groups.get(0).summary.complete);
    }

    @Test public void mixedPlaceIsOneCountryMemberAndCurrentEntryCanServeItsDetail() {
        MemoryRepository repository = new MemoryRepository(snapshot(group("tokyo", "도쿄", "JP")),
                Collections.singletonList(album("도쿄", "JP", 8)));
        MemoryMediaResolver.GalleryReader reader = (path, record) -> MemoryMediaStoreReader.Result.found(
                Collections.singletonList(group("tokyo", "도쿄", "JP").photoRefs.get(0)));
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true, Collections.emptyList(),
                Collections.emptyMap(), reader).withRevision("current");
        assertEquals(1, country(entry, "JP").records.size());
        assertEquals("사진·동영상 1개", country(entry, "JP").summary.countText);
        MemoryBrowserEntry scoped = entry.countryForRevision("JP", "current");
        assertNotNull(scoped);
        assertEquals(1, scoped.summary.photoCount);
        assertEquals(1, scoped.state.items.size());
        assertNotNull(scoped.detailForRevision(scoped.records.get(0).memoryKey, "current", reader));
        assertNull(entry.countryForRevision("JP", "changed"));
    }

    @Test public void countryScopeAndHomeProjectionUseTheSameUnifiedPlaces() {
        MemoryRepository repository = repository(group("tokyo", "도쿄", "JP"), group("seoul", "서울", "KR"));
        MemoryBrowserEntry scoped = MemoryBrowserEntry.buildCountry(repository, "JP", null);
        OverseasCountryProjection projection = OverseasCountryProjection.fromUnified("JP", scoped.records);
        assertEquals(1, scoped.records.size());
        assertEquals(scoped.records, projection.discoveryRecords);
        assertTrue(projection.organizedAlbums.isEmpty());
        assertEquals("도쿄", scoped.state.items.get(0).title);
    }

    @Test public void automaticCountriesDoNotChangeManualCollectionsOrAliasOwnership() {
        MemoryRepository repository = repository(group("tokyo", "도쿄", "JP"), group("seoul", "서울", "KR"));
        Map<String,String> aliases = new LinkedHashMap<>();
        aliases.put("tokyo", "mem_tokyo"); aliases.put("seoul", "mem_seoul");
        Map<String,String> before = new LinkedHashMap<>(aliases);
        MemoryCollection manual = new MemoryCollection("group_manual", "수동 모음", Arrays.asList(
                new MemoryCollection.Member("mem_tokyo", "tokyo"), new MemoryCollection.Member("mem_seoul", "seoul")), 1, 1);
        MemoryBrowserEntry entry = MemoryBrowserEntry.build(repository, true, Collections.singletonList(manual), aliases, null);
        assertEquals(before, aliases);
        assertSame(manual, entry.collections.get(0));
        assertEquals(1, entry.countries.groups.size());
        assertEquals("tokyo", entry.countries.groups.get(0).records.get(0).memoryKey);
        assertEquals(2, manual.members.size());
    }

    @Test public void countryChangesInvalidateTheCountryPresentationSignature() {
        MemoryBrowserEntry japan = entry(repository(group("same", "장소", "JP")));
        MemoryBrowserEntry australia = entry(repository(group("same", "장소", "AU")));
        assertNotEquals(japan.countries.signature, australia.countries.signature);
        assertFalse(japan.matches(australia));
    }

    private static AutomaticCountryGroups.Group country(MemoryBrowserEntry entry, String code) {
        for (AutomaticCountryGroups.Group group : entry.countries.groups) if (code.equals(group.code)) return group;
        throw new AssertionError("Missing country: " + code);
    }
    private static MemoryBrowserEntry entry(MemoryRepository repository) {
        return MemoryBrowserEntry.build(repository, true, Collections.emptyList(), Collections.emptyMap(), null);
    }
    private static MemoryRepository repository(DiscoveryMemoryGroup... groups) {
        return new MemoryRepository(snapshot(groups), Collections.emptyList());
    }
    private static DiscoverySnapshot snapshot(DiscoveryMemoryGroup... groups) {
        return new DiscoverySnapshot(1, 1, TAKEN, "countries", groups.length, Arrays.asList(groups), "", "");
    }
    private static DiscoveryMemoryGroup group(String key, String place, String country) {
        DiscoveryPhotoRef ref = new DiscoveryPhotoRef("content://photo/" + key, 1, MediaKind.PHOTO, "image/jpeg",
                key+".jpg", TAKEN, place, place, country, "", "area", "", "DCIM/Camera/", 1, 1, false);
        return new DiscoveryMemoryGroup(key, place, place, country, "", "area", "", 1, 1, 0, TAKEN, TAKEN,
                ref.sourceUri, Collections.singletonList(ref), 0, 1);
    }
    private static StoredAlbumSummary album(String title, String country, int count) {
        return new StoredAlbumSummary(title+"에서", "Pictures/"+title+"에서/", count, "2026-11-23", "2026-11-23",
                "content://cover", "", 0, country, "", "area", "");
    }
}

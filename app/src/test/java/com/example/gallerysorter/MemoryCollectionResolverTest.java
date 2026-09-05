package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemoryCollectionResolverTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void keepsEachPlacesDateNoteWhenTwoPlacesShareADate() throws Exception {
        MemoryIdentityRegistryStore identities = new MemoryIdentityRegistryStore(temporaryFolder.newFolder("identity"));
        MemoryDateNoteStore notes = new MemoryDateNoteStore(temporaryFolder.newFolder("notes"));
        String sapporoId = identities.resolveOrCreate("discovery:sapporo");
        String otaruId = identities.resolveOrCreate("discovery:otaru");
        assertTrue(notes.save(sapporoId, "20260805", "징기스칸을 먹었다", 10L));
        assertTrue(notes.save(otaruId, "20260805", "운하를 걸었다", 10L));

        MemoryCollection collection = collection(sapporoId, otaruId);
        GroupMemoryDetail detail = new MemoryCollectionResolver(identities, notes).resolve(collection,
                Arrays.asList(record("sapporo", "삿포로", ref("content://1", 1785888000000L)),
                        record("otaru", "오타루", ref("content://2", 1785888000000L))));

        assertNotNull(detail);
        assertEquals(1, detail.dates.size());
        assertEquals(2, detail.dates.get(0).places.size());
        assertEquals("삿포로", detail.dates.get(0).places.get(0).placeTitle);
        assertEquals("징기스칸을 먹었다", detail.dates.get(0).places.get(0).noteText);
        assertEquals("오타루", detail.dates.get(0).places.get(1).placeTitle);
        assertEquals("운하를 걸었다", detail.dates.get(0).places.get(1).noteText);
    }

    @Test
    public void groupsDatesNewestFirstAndDeduplicatesSourceUrisAcrossPlaces() throws Exception {
        MemoryIdentityRegistryStore identities = new MemoryIdentityRegistryStore(temporaryFolder.newFolder("identity2"));
        MemoryDateNoteStore notes = new MemoryDateNoteStore(temporaryFolder.newFolder("notes2"));
        String sapporoId = identities.resolveOrCreate("discovery:sapporo");
        String otaruId = identities.resolveOrCreate("discovery:otaru");

        GroupMemoryDetail detail = new MemoryCollectionResolver(identities, notes).resolve(collection(sapporoId, otaruId),
                Arrays.asList(record("sapporo", "삿포로",
                                ref("content://shared", 1785888000000L), ref("content://old", 1785801600000L)),
                        record("otaru", "오타루",
                                ref("content://shared", 1785888000000L), ref("content://new", 1785974400000L))));

        assertEquals(3, detail.dates.size());
        assertEquals("20260806", detail.dates.get(0).dateKey);
        assertEquals("20260805", detail.dates.get(1).dateKey);
        assertEquals("20260804", detail.dates.get(2).dateKey);
        assertEquals(1, detail.dates.get(1).places.get(0).photos.size());
        assertEquals("content://shared", detail.dates.get(1).places.get(0).photos.get(0).sourceUri);
    }

    @Test
    public void ignoresUnavailableMembersWithoutDestroyingTheCollectionProjection() throws Exception {
        MemoryIdentityRegistryStore identities = new MemoryIdentityRegistryStore(temporaryFolder.newFolder("identity3"));
        MemoryDateNoteStore notes = new MemoryDateNoteStore(temporaryFolder.newFolder("notes3"));
        String sapporoId = identities.resolveOrCreate("discovery:sapporo");
        String missingId = identities.resolveOrCreate("discovery:missing");

        GroupMemoryDetail detail = new MemoryCollectionResolver(identities, notes).resolve(collection(sapporoId, missingId),
                Collections.singletonList(record("sapporo", "삿포로", ref("content://1", 1785888000000L))));

        assertNotNull(detail);
        assertEquals(1, detail.dates.size());
        assertEquals("삿포로", detail.dates.get(0).places.get(0).placeTitle);
    }

    private static MemoryCollection collection(String firstId, String secondId) {
        return new MemoryCollection("group_trip", "2026 홋카이도 여행", Arrays.asList(
                new MemoryCollection.Member(firstId, "discovery:sapporo"),
                new MemoryCollection.Member(secondId, "discovery:otaru")), 1L, 1L);
    }

    private static MemoryRecord record(String key, String title, DiscoveryPhotoRef... refs) {
        List<DiscoveryPhotoRef> values = Arrays.asList(refs);
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup("discovery:" + key, key, title,
                "JP", "Japan", "Hokkaido", title, values.size(), values.size(), 0,
                values.get(values.size() - 1).takenAtMillis, values.get(0).takenAtMillis,
                values.get(0).sourceUri, values, 0, 1L);
        return MemoryRepository.fromDiscoveryGroup(group);
    }

    private static DiscoveryPhotoRef ref(String uri, long takenAtMillis) {
        return new DiscoveryPhotoRef(uri, 1L, MediaKind.PHOTO, "image/jpeg", "photo.jpg",
                takenAtMillis, "", "", "JP", "Japan", "Hokkaido", "", "", 1L, 1L, false);
    }
}

package com.example.gallerysorter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PersonalPlaceTest {
    private File tempDir;

    // Seongnam Bundang base coordinate (37.382700, 127.118900)
    private static final double HOME_LAT = 37.382700d;
    private static final double HOME_LNG = 127.118900d;

    // ~35m North of HOME
    private static final double HOME_ROOM_LAT = 37.383015d;
    private static final double HOME_ROOM_LNG = 127.118900d;

    // ~72m North of HOME (near 80m boundary - should STILL auto-match when single candidate!)
    private static final double HOME_VERANDA_LAT = 37.383348d;
    private static final double HOME_VERANDA_LNG = 127.118900d;

    // ~90m North of HOME (outside 80m, inside 120m or adaptive indoor drift)
    private static final double HOME_DRIFT_90M_LAT = 37.383510d;
    private static final double HOME_DRIFT_90M_LNG = 127.118900d;

    // ~65m North of HOME (overlapping Cafe center)
    private static final double CAFE_LAT = 37.383285d;
    private static final double CAFE_LNG = 127.118900d;

    // ~500m away (Office)
    private static final double OFFICE_LAT = 37.387200d;
    private static final double OFFICE_LNG = 127.118900d;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("personal_place_test").toFile();
        PersonalPlaceGpsCacheStore.resetMemoryCacheForTests();
    }

    @After
    public void tearDown() {
        PersonalPlaceGpsCacheStore.resetMemoryCacheForTests();
        if (tempDir != null && tempDir.exists()) {
            File[] children = tempDir.listFiles();
            if (children != null) {
                for (File f : children) {
                    f.delete();
                }
            }
            tempDir.delete();
        }
    }

    @Test
    public void singleCandidateWithin80mRadiusAutoMatchesEvenNearBoundary() {
        File storeFile = new File(tempDir, "personal_places.json");
        PersonalPlaceStore store = new PersonalPlaceStore(storeFile);

        DiscoveryPhotoRef anchor = photoRef("content://media/external/images/media/1", "IMG_001.jpg", 1000L);
        DiscoveryPhotoRef roomNew = photoRef("content://media/external/images/media/2", "IMG_002.jpg", 2000L);
        DiscoveryPhotoRef verandaNew = photoRef("content://media/external/images/media/3", "IMG_003.jpg", 3000L);
        DiscoveryPhotoRef officeFar = photoRef("content://media/external/images/media/4", "IMG_004.jpg", 4000L);

        PersonalPlace home = store.savePlace(
                "",
                "mem_seongnam",
                "성남",
                "우리집",
                HOME_LAT,
                HOME_LNG,
                true,
                PersonalPlace.RADIUS_DEFAULT, // 80m
                PersonalPlace.RADIUS_DEFAULT,
                Collections.singletonList(anchor),
                Collections.emptyList(),
                1000L);

        Map<String, PersonalPlaceGpsPoint> gps = new LinkedHashMap<>();
        gps.put(anchor.sourceUri, PersonalPlaceGpsPoint.withCoordinates(anchor.sourceUri, "fp1", HOME_LAT, HOME_LNG));
        gps.put(roomNew.sourceUri, PersonalPlaceGpsPoint.withCoordinates(roomNew.sourceUri, "fp2", HOME_ROOM_LAT, HOME_ROOM_LNG));
        gps.put(verandaNew.sourceUri, PersonalPlaceGpsPoint.withCoordinates(verandaNew.sourceUri, "fp3", HOME_VERANDA_LAT, HOME_VERANDA_LNG));
        gps.put(officeFar.sourceUri, PersonalPlaceGpsPoint.withCoordinates(officeFar.sourceUri, "fp4", OFFICE_LAT, OFFICE_LNG));

        List<PersonalPlace> synced = store.syncAutoMatchesForGroup(
                "mem_seongnam",
                "성남",
                Arrays.asList(anchor, roomNew, verandaNew, officeFar),
                gps,
                5000L);

        assertEquals(1, synced.size());
        PersonalPlace updatedHome = synced.get(0);
        assertEquals(home.placeId, updatedHome.placeId);
        assertTrue(updatedHome.contains(anchor));
        assertTrue("35m photo must auto-match", updatedHome.isAutoMatched(roomNew));
        assertTrue("72m photo within 80m single candidate must auto-match without holdback",
                updatedHome.isAutoMatched(verandaNew));
        assertFalse("500m photo must not match 80m home", updatedHome.contains(officeFar));
    }

    @Test
    public void confirmedIndoorDriftExpandsEffectiveRadiusUpToCap() {
        PersonalPlaceGpsPoint center = PersonalPlaceGpsPoint.withCoordinates("u1", "f1", HOME_LAT, HOME_LNG);
        PersonalPlaceGpsPoint drifted90m = PersonalPlaceGpsPoint.withCoordinates("u2", "f2", HOME_DRIFT_90M_LAT, HOME_DRIFT_90M_LNG);

        int effective = PersonalPlaceMatcher.computeEffectiveRadius(
                PersonalPlace.RADIUS_DEFAULT, // 80m selected
                HOME_LAT,
                HOME_LNG,
                Arrays.asList(center, drifted90m));

        // ~90m + 15m margin = ~105m..106m
        assertTrue("Effective radius should expand beyond 80m to cover confirmed 90m indoor drift",
                effective >= 104 && effective <= 107);
    }

    @Test
    public void overlappingPlacesProtectExistingAutoMatchesAndHoldAmbiguousMidpoint() {
        File storeFile = new File(tempDir, "personal_places.json");
        PersonalPlaceStore store = new PersonalPlaceStore(storeFile);

        DiscoveryPhotoRef homeAnchor = photoRef("content://media/external/images/media/10", "HOME.jpg", 1000L);
        DiscoveryPhotoRef existingAutoInHome = photoRef("content://media/external/images/media/11", "AUTO_HOME.jpg", 2000L);
        DiscoveryPhotoRef cafeAnchor = photoRef("content://media/external/images/media/12", "CAFE.jpg", 3000L);
        DiscoveryPhotoRef ambiguousMidpoint = photoRef("content://media/external/images/media/13", "MID.jpg", 4000L);
        DiscoveryPhotoRef clearlyCloserToHome = photoRef("content://media/external/images/media/14", "NEAR_HOME.jpg", 5000L);

        // Step 1: Create '우리집' and auto-match existingAutoInHome (at ~35m)
        store.savePlace(
                "", "mem_seongnam", "성남", "우리집",
                HOME_LAT, HOME_LNG, true, 80, 80,
                Collections.singletonList(homeAnchor), Collections.emptyList(), 1000L);

        Map<String, PersonalPlaceGpsPoint> gps = new LinkedHashMap<>();
        gps.put(homeAnchor.sourceUri, PersonalPlaceGpsPoint.withCoordinates(homeAnchor.sourceUri, "f10", HOME_LAT, HOME_LNG));
        gps.put(existingAutoInHome.sourceUri, PersonalPlaceGpsPoint.withCoordinates(existingAutoInHome.sourceUri, "f11", HOME_ROOM_LAT, HOME_ROOM_LNG));

        store.syncAutoMatchesForGroup("mem_seongnam", "성남", Arrays.asList(homeAnchor, existingAutoInHome), gps, 2000L);

        // Step 2: Create '단골 카페' 65m away with 80m radius (overlaps '우리집')
        store.savePlace(
                "", "mem_seongnam", "성남", "단골 카페",
                CAFE_LAT, CAFE_LNG, true, 80, 80,
                Collections.singletonList(cafeAnchor), Collections.emptyList(), 3000L);

        // Midpoint (~32.5m from both HOME and CAFE -> difference < 15m -> should be held in Seongnam!)
        double midLat = (HOME_LAT + CAFE_LAT) / 2.0d;
        // 10m from HOME, 55m from CAFE -> difference 45m >= 15m -> clearly closer to HOME
        double nearHomeLat = HOME_LAT + 0.00009d;

        gps.put(cafeAnchor.sourceUri, PersonalPlaceGpsPoint.withCoordinates(cafeAnchor.sourceUri, "f12", CAFE_LAT, CAFE_LNG));
        gps.put(ambiguousMidpoint.sourceUri, PersonalPlaceGpsPoint.withCoordinates(ambiguousMidpoint.sourceUri, "f13", midLat, HOME_LNG));
        gps.put(clearlyCloserToHome.sourceUri, PersonalPlaceGpsPoint.withCoordinates(clearlyCloserToHome.sourceUri, "f14", nearHomeLat, HOME_LNG));

        List<PersonalPlace> places = store.syncAutoMatchesForGroup(
                "mem_seongnam",
                "성남",
                Arrays.asList(homeAnchor, existingAutoInHome, cafeAnchor, ambiguousMidpoint, clearlyCloserToHome),
                gps,
                5000L);

        PersonalPlace home = places.get(0).name.equals("우리집") ? places.get(0) : places.get(1);
        PersonalPlace cafe = places.get(0).name.equals("단골 카페") ? places.get(0) : places.get(1);

        assertTrue("Existing auto-matched photo in 우리집 must not be lost when 카페 is added",
                home.contains(existingAutoInHome));
        assertFalse("Existing auto-matched photo in 우리집 must not be stolen by 카페",
                cafe.contains(existingAutoInHome));
        assertFalse("Ambiguous midpoint photo must not auto-match to 우리집",
                home.contains(ambiguousMidpoint));
        assertFalse("Ambiguous midpoint photo must not auto-match to 카페",
                cafe.contains(ambiguousMidpoint));
        assertTrue("Photo clearly closer to 우리집 (>=15m margin) should auto-match to 우리집",
                home.contains(clearlyCloserToHome));
    }

    @Test
    public void cardShortcutsReuseOneSnapshotAndKeepStableAndLegacyParentMatches() {
        PersonalPlaceStore store = new PersonalPlaceStore(new File(tempDir, "personal_places.json"));
        MemoryIdentityRegistryStore registry = new MemoryIdentityRegistryStore(tempDir);
        String stableId = registry.resolveOrCreate("discovery:renamed");
        PersonalPlace home = store.savePlace("", stableId, "discovery:old", "우리집",
                HOME_LAT, HOME_LNG, true, 80, 80, Collections.emptyList(), Collections.emptyList(), 1000L);
        AtomicInteger aliasReads = new AtomicInteger();
        MemoryIdentityRegistryStore observedRegistry = new MemoryIdentityRegistryStore(tempDir, () -> {
            aliasReads.incrementAndGet();
            return ManualPlaceSnapshot.empty();
        });

        Function<String, List<PersonalPlace>> lookup = store.snapshotByParent(observedRegistry);
        assertEquals(home.placeId, lookup.apply("discovery:renamed").get(0).placeId);
        assertEquals(home.placeId, lookup.apply("discovery:old").get(0).placeId);
        assertEquals(home.placeId, lookup.apply("note-only:" + stableId).get(0).placeId);
        assertTrue(lookup.apply("discovery:unrelated").isEmpty());
        assertEquals(1, aliasReads.get());

        store.savePlace(home.placeId, stableId, "discovery:old", "이름 변경",
                HOME_LAT, HOME_LNG, true, 80, 80, Collections.emptyList(), Collections.emptyList(), 2000L);
        assertEquals("우리집", lookup.apply("discovery:old").get(0).name);
        assertEquals("이름 변경", store.snapshotByParent(observedRegistry).apply("discovery:old").get(0).name);
    }

    @Test
    public void manualExclusionPreventsReappearanceOnSubsequentAutoRefresh() {
        File storeFile = new File(tempDir, "personal_places.json");
        PersonalPlaceStore store = new PersonalPlaceStore(storeFile);

        DiscoveryPhotoRef anchor = photoRef("content://media/external/images/media/20", "A.jpg", 1000L);
        DiscoveryPhotoRef neighbor = photoRef("content://media/external/images/media/21", "B.jpg", 2000L);

        PersonalPlace home = store.savePlace(
                "", "mem_seongnam", "성남", "우리집",
                HOME_LAT, HOME_LNG, true, 80, 80,
                Collections.singletonList(anchor), Collections.emptyList(), 1000L);

        Map<String, PersonalPlaceGpsPoint> gps = new LinkedHashMap<>();
        gps.put(anchor.sourceUri, PersonalPlaceGpsPoint.withCoordinates(anchor.sourceUri, "f20", HOME_LAT, HOME_LNG));
        gps.put(neighbor.sourceUri, PersonalPlaceGpsPoint.withCoordinates(neighbor.sourceUri, "f21", HOME_ROOM_LAT, HOME_ROOM_LNG));

        store.syncAutoMatchesForGroup("mem_seongnam", "성남", Arrays.asList(anchor, neighbor), gps, 2000L);
        assertTrue(store.findById(home.placeId).contains(neighbor));

        // User excludes neighbor from '우리집'
        store.excludePhotos(home.placeId, Collections.singletonList(new MemoryPhotoItem(neighbor)), 3000L);
        assertFalse(store.findById(home.placeId).contains(neighbor));

        // Subsequent auto-refresh must NOT re-add neighbor
        store.syncAutoMatchesForGroup("mem_seongnam", "성남", Arrays.asList(anchor, neighbor), gps, 4000L);
        assertFalse("Excluded photo must never be re-added by auto-refresh",
                store.findById(home.placeId).contains(neighbor));
    }

    @Test
    public void multiAnchorSpreadDetectionFlagsDistantAnchors() {
        PersonalPlaceGpsPoint homePt = PersonalPlaceGpsPoint.withCoordinates("u1", "f1", HOME_LAT, HOME_LNG);
        // ~2.2km away
        PersonalPlaceGpsPoint farPt = PersonalPlaceGpsPoint.withCoordinates("u2", "f2", HOME_LAT + 0.020d, HOME_LNG);

        PersonalPlaceMatcher.AnchorAnalysis wide =
                PersonalPlaceMatcher.analyzeAnchors(Arrays.asList(homePt, farPt));
        assertTrue(wide.hasValidCoordinate);
        assertTrue("Anchors >1km apart must be flagged as spreadTooWide", wide.spreadTooWide);

        PersonalPlaceGpsPoint campusPt = PersonalPlaceGpsPoint.withCoordinates("u3", "f3", OFFICE_LAT, OFFICE_LNG);
        PersonalPlaceMatcher.AnchorAnalysis compact =
                PersonalPlaceMatcher.analyzeAnchors(Arrays.asList(homePt, campusPt));
        assertTrue(compact.hasValidCoordinate);
        assertFalse("Anchors 500m apart within a large company campus must be accepted", compact.spreadTooWide);
    }

    @Test
    public void signaturePreservesMembershipAfterAlbumCopyAndOriginalCleanup() {
        File storeFile = new File(tempDir, "personal_places.json");
        PersonalPlaceStore store = new PersonalPlaceStore(storeFile);

        DiscoveryPhotoRef originalCameraRef = photoRef(
                "content://media/external/images/media/100", "20261115_HOME.jpg", 1700000000000L);
        PersonalPlace home = store.savePlace(
                "", "mem_seongnam", "성남", "우리집",
                HOME_LAT, HOME_LNG, true, 80, 80,
                Collections.singletonList(originalCameraRef), Collections.emptyList(), 1000L);

        // Simulate gallery album copy with a new MediaStore URI after Original Cleanup trashed the original URI
        DiscoveryPhotoRef organizedCopyRef = photoRef(
                "content://media/external/images/media/999", "20261115_HOME.jpg", 1700000000000L);
        assertTrue("Organized copy with identical displayName and takenAtMillis must inherit membership",
                home.contains(organizedCopyRef));
    }

    @Test
    public void gpsCachePersistsHasGpsAndNoGpsButNeverCachesDeferredErrors() {
        File cacheFile = new File(tempDir, "personal_place_gps_cache.json");
        PersonalPlaceGpsCacheStore cache1 = new PersonalPlaceGpsCacheStore(cacheFile);

        PersonalPlaceGpsPoint validPt = PersonalPlaceGpsPoint.withCoordinates("content://media/1", "fp1", HOME_LAT, HOME_LNG);
        PersonalPlaceGpsPoint noGpsPt = PersonalPlaceGpsPoint.noGps("content://media/2", "fp2");
        PersonalPlaceGpsPoint deferredPt = PersonalPlaceGpsPoint.deferred("content://media/3", "fp3");

        cache1.putAll(Arrays.asList(validPt, noGpsPt, deferredPt));

        PersonalPlaceGpsCacheStore.resetMemoryCacheForTests();
        PersonalPlaceGpsCacheStore cache2 = new PersonalPlaceGpsCacheStore(cacheFile);

        assertNotNull(cache2.get("content://media/1", "fp1"));
        assertTrue(cache2.get("content://media/1", "fp1").hasCoordinates());
        assertNotNull(cache2.get("content://media/2", "fp2"));
        assertEquals(PersonalPlaceGpsPoint.Status.NO_GPS_TAG, cache2.get("content://media/2", "fp2").status);
        assertNull("READ_DEFERRED_OR_DENIED must never be persisted to disk cache",
                cache2.get("content://media/3", "fp3"));
    }

    @Test
    public void sectionFilteringSeparatesPlacePhotosRemainingPhotosAndDateNotes() {
        File storeFile = new File(tempDir, "personal_places.json");
        PersonalPlaceStore store = new PersonalPlaceStore(storeFile);

        // 2026-11-15: 1 home photo, 1 outing photo
        long day1 = 1763180000000L;
        // 2026-11-14: 1 office photo
        long day2 = 1763090000000L;

        DiscoveryPhotoRef homeRef = photoRef("content://media/1", "home.jpg", day1);
        DiscoveryPhotoRef outingRef = photoRef("content://media/2", "outing.jpg", day1 + 1000L);
        DiscoveryPhotoRef officeRef = photoRef("content://media/3", "office.jpg", day2);

        PersonalPlace home = store.savePlace(
                "", "mem_seongnam", "성남", "우리집",
                HOME_LAT, HOME_LNG, true, 80, 80,
                Collections.singletonList(homeRef), Collections.emptyList(), 1000L);
        PersonalPlace office = store.savePlace(
                "", "mem_seongnam", "성남", "회사",
                OFFICE_LAT, OFFICE_LNG, true, 80, 80,
                Collections.singletonList(officeRef), Collections.emptyList(), 1000L);

        List<MemoryPhotoSection> allSections = MemoryPhotoSection.fromDiscoveryRefs(
                Arrays.asList(homeRef, outingRef, officeRef));
        assertEquals(2, allSections.size());

        List<MemoryPhotoSection> homeSections = PersonalPlaceMatcher.filterSectionsForPlace(allSections, home);
        assertEquals(1, homeSections.size());
        assertEquals(1, homeSections.get(0).photos.size());
        assertEquals("content://media/1", homeSections.get(0).photos.get(0).sourceUri);

        List<MemoryPhotoSection> remainingSections = PersonalPlaceMatcher.filterSectionsForRemaining(
                allSections, Arrays.asList(home, office));
        assertEquals(1, remainingSections.size());
        assertEquals(1, remainingSections.get(0).photos.size());
        assertEquals("content://media/2", remainingSections.get(0).photos.get(0).sourceUri);

        String day1Key = homeSections.get(0).dateKey;
        String day2Key = PersonalPlaceMatcher.filterSectionsForPlace(allSections, office).get(0).dateKey;
        List<MemoryDateNote> allNotes = Arrays.asList(
                new MemoryDateNote("mem_seongnam", day1Key, "집과 공원 산책", 1000L, 1000L),
                new MemoryDateNote("mem_seongnam", day2Key, "야근한 날", 2000L, 2000L));

        List<MemoryDateNote> homeNotes = PersonalPlaceMatcher.filterNotesForSections(allNotes, homeSections);
        assertEquals(1, homeNotes.size());
        assertEquals("집과 공원 산책", homeNotes.get(0).text);
    }

    private static DiscoveryPhotoRef photoRef(String uri, String name, long takenAt) {
        return new DiscoveryPhotoRef(
                uri,
                1L,
                MediaKind.PHOTO,
                "image/jpeg",
                name,
                takenAt,
                "성남",
                "성남",
                "KR",
                "대한민국",
                "경기도",
                "성남시 분당구",
                "DCIM/Camera/",
                1L,
                1L,
                false,
                false);
    }
}

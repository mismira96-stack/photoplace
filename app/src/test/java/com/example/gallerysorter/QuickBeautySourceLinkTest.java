package com.example.gallerysorter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QuickBeautySourceLinkTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static long parseExifMillis(String exifDt) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
        fmt.setLenient(false);
        return fmt.parse(exifDt).getTime();
    }

    private static final class FakeSourceRepository implements QuickBeautySourceLink.SourceRepository {
        final Map<Long, QuickBeautySourceLink.CandidateMedia> mediaById = new LinkedHashMap<>();
        final Map<Long, QuickBeautySourceLink.CandidateInspection> inspectionById = new LinkedHashMap<>();

        void put(long id,
                 String name,
                 long takenMs,
                 QuickBeautySourceLink.CandidateInspection inspection) {
            mediaById.put(id, new QuickBeautySourceLink.CandidateMedia(id, name, takenMs));
            inspectionById.put(id, inspection);
        }

        @Override
        public QuickBeautySourceLink.CandidateMedia findById(long id) {
            return mediaById.get(id);
        }

        @Override
        public List<QuickBeautySourceLink.CandidateMedia> findByDisplayName(String displayName, int limit) {
            if (displayName == null) {
                return Collections.emptyList();
            }
            List<QuickBeautySourceLink.CandidateMedia> list = new ArrayList<>();
            for (QuickBeautySourceLink.CandidateMedia item : mediaById.values()) {
                if (displayName.equalsIgnoreCase(item.displayName)) {
                    list.add(item);
                    if (list.size() >= limit) {
                        break;
                    }
                }
            }
            return list;
        }

        @Override
        public QuickBeautySourceLink.CandidateInspection inspectCandidate(long id, boolean mediaLocationAllowed) {
            QuickBeautySourceLink.CandidateInspection inspection = inspectionById.get(id);
            return inspection != null ? inspection : QuickBeautySourceLink.CandidateInspection.unopened();
        }
    }

    @Test
    public void parseMarkerExtractsSourceIdNameAndOriginalDateTime() {
        String marker = "QuickBeauty|src_id=1000054321|src_name=20261015_142310.jpg|src_dt=2026:10:15 14:23:10|src_taken=1792041790000";
        QuickBeautySourceLink.ParsedLink parsed = QuickBeautySourceLink.parseMarker(marker);
        assertNotNull(parsed);
        assertEquals(1000054321L, parsed.sourceId);
        assertEquals("20261015_142310.jpg", parsed.sourceName);
        assertEquals("2026:10:15 14:23:10", parsed.sourceExifDateTime);
        assertEquals(1792041790000L, parsed.sourceTakenMillis);

        Date date = QuickBeautySourceLink.parseTakenDate(parsed);
        assertNotNull(date);
        assertEquals(
                "2026:10:15 14:23:10",
                new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(date)
        );
    }

    @Test
    public void parseMarkerHandlesUserCommentCharsetPrefixAndRejectsUnrelatedText() {
        String withPrefix = "ASCII\0\0\0QuickBeauty|src_id=42|src_name=IMG_0042.jpg|src_dt=|src_taken=1700000000000";
        QuickBeautySourceLink.ParsedLink parsed = QuickBeautySourceLink.parseMarker(withPrefix);
        assertNotNull(parsed);
        assertEquals(42L, parsed.sourceId);
        assertEquals("IMG_0042.jpg", parsed.sourceName);
        assertEquals(1700000000000L, parsed.sourceTakenMillis);

        assertNull(QuickBeautySourceLink.parseMarker("Some other comment"));
        assertNull(QuickBeautySourceLink.parseMarker(null));
    }

    @Test
    public void rejectsIdCollisionWhenDisplayNameOrTimestampDiffers() throws Exception {
        String dt = "2026:10:15 14:23:10";
        long takenMs = parseExifMillis(dt);
        FakeSourceRepository repo = new FakeSourceRepository();
        // ID 100 exists on this device, but belongs to a completely different photo.
        repo.put(
                100L,
                "OTHER_PHOTO.jpg",
                takenMs + 86400_000L,
                QuickBeautySourceLink.CandidateInspection.opened(true, 37.5665d, 126.9780d, "2026:10:16 14:23:10", null)
        );

        QuickBeautySourceLink.ParsedLink link = QuickBeautySourceLink.parseMarker(
                "QuickBeauty|src_id=100|src_name=20261015_142310.jpg|src_dt=" + dt + "|src_taken=" + takenMs
        );
        QuickBeautySourceLink.Resolution res = QuickBeautySourceLink.resolve(repo, link, true);
        assertFalse(res.hasCoordinates());
        assertEquals(QuickBeautySourceLink.Status.DEFERRED_OR_UNAVAILABLE, res.status);

        // Same ID and name, but wrong EXIF shooting date -> must also be rejected.
        repo.put(
                100L,
                "20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(true, 37.5665d, 126.9780d, "2026:10:16 14:23:10", null)
        );
        QuickBeautySourceLink.Resolution wrongDateRes = QuickBeautySourceLink.resolve(repo, link, true);
        assertFalse(wrongDateRes.hasCoordinates());
    }

    @Test
    public void rejectsBareNumericIdMarkerWithoutIdentityEvidence() {
        FakeSourceRepository repo = new FakeSourceRepository();
        repo.put(
                100L,
                "20261015_142310.jpg",
                1792041790000L,
                QuickBeautySourceLink.CandidateInspection.opened(true, 35.1796d, 129.0756d, "2026:10:15 14:23:10", null)
        );
        QuickBeautySourceLink.ParsedLink bareIdLink = QuickBeautySourceLink.parseMarker(
                "QuickBeauty|src_id=100|src_name=|src_dt=|src_taken="
        );
        QuickBeautySourceLink.Resolution res = QuickBeautySourceLink.resolve(repo, bareIdLink, true);
        assertFalse(res.hasCoordinates());
        assertEquals(QuickBeautySourceLink.Status.AMBIGUOUS, res.status);
    }

    @Test
    public void resolvesMovedOriginalWithNewIdAndRejectsAmbiguousDuplicates() throws Exception {
        String dt = "2026:10:15 14:23:10";
        long takenMs = parseExifMillis(dt);
        FakeSourceRepository repo = new FakeSourceRepository();
        // Original ID 100 was moved to a sorted album and got new ID 250.
        repo.put(
                250L,
                "20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(true, 35.6762d, 139.6503d, dt, null)
        );
        // Another photo with same timestamp but different name should not interfere.
        repo.put(
                251L,
                "20261015_142310_burst.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(true, 34.6937d, 135.5023d, dt, null)
        );
        // Another photo with same name but different date should be rejected.
        repo.put(
                252L,
                "20261015_142310.jpg",
                takenMs + 3600_000L,
                QuickBeautySourceLink.CandidateInspection.opened(true, 33.4996d, 126.5312d, "2026:10:15 15:23:10", null)
        );

        QuickBeautySourceLink.ParsedLink link = QuickBeautySourceLink.parseMarker(
                "QuickBeauty|src_id=100|src_name=20261015_142310.jpg|src_dt=" + dt + "|src_taken=" + takenMs
        );
        QuickBeautySourceLink.Resolution movedRes = QuickBeautySourceLink.resolve(repo, link, true);
        assertEquals(QuickBeautySourceLink.Status.FOUND, movedRes.status);
        assertEquals(35.6762d, movedRes.latitude, 1e-4d);
        assertEquals(139.6503d, movedRes.longitude, 1e-4d);

        // Now add a second identical candidate (same name + same date) -> must reject as AMBIGUOUS.
        repo.put(
                253L,
                "20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(true, 37.5665d, 126.9780d, dt, null)
        );
        QuickBeautySourceLink.Resolution ambiguousRes = QuickBeautySourceLink.resolve(repo, link, true);
        assertFalse(ambiguousRes.hasCoordinates());
        assertEquals(QuickBeautySourceLink.Status.AMBIGUOUS, ambiguousRes.status);
    }

    @Test
    public void transientSourceReadFailureReturnsDeferredAndSucceedsOnRetry() throws Exception {
        String dt = "2026:10:15 14:23:10";
        long takenMs = parseExifMillis(dt);
        FakeSourceRepository repo = new FakeSourceRepository();
        // First attempt: candidate row exists, but file stream cannot be opened yet.
        repo.put(
                100L,
                "20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.unopened()
        );

        QuickBeautySourceLink.ParsedLink link = QuickBeautySourceLink.parseMarker(
                "QuickBeauty|src_id=100|src_name=20261015_142310.jpg|src_dt=" + dt + "|src_taken=" + takenMs
        );
        QuickBeautySourceLink.Resolution firstAttempt = QuickBeautySourceLink.resolve(repo, link, true);
        assertEquals(QuickBeautySourceLink.Status.DEFERRED_OR_UNAVAILABLE, firstAttempt.status);

        // Second attempt: file becomes readable and yields GPS.
        repo.put(
                100L,
                "20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(true, 35.1587d, 129.1604d, dt, null)
        );
        QuickBeautySourceLink.Resolution secondAttempt = QuickBeautySourceLink.resolve(repo, link, true);
        assertEquals(QuickBeautySourceLink.Status.FOUND, secondAttempt.status);
        assertEquals(35.1587d, secondAttempt.latitude, 1e-4d);
        assertEquals(129.1604d, secondAttempt.longitude, 1e-4d);

        // Confirmed no GPS on original only when opened and genuinely has no GPS and no parent link.
        repo.put(
                100L,
                "20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(false, 0.0d, 0.0d, dt, null)
        );
        QuickBeautySourceLink.Resolution confirmedNoGps = QuickBeautySourceLink.resolve(repo, link, true);
        assertEquals(QuickBeautySourceLink.Status.CONFIRMED_NO_GPS, confirmedNoGps.status);
    }

    @Test
    public void buildFingerprintInvalidatesLegacyNegativeCacheForQuickBeautyFiles() throws Exception {
        File cacheFile = tempFolder.newFile("gps_cache.json");
        PersonalPlaceGpsCacheStore store = new PersonalPlaceGpsCacheStore(cacheFile);
        String uri = "content://media/external/images/media/9001";
        String legacyFingerprint = "9001:1792041790000:Beauty_20261015_142310.jpg";
        store.putAll(Collections.singletonList(PersonalPlaceGpsPoint.noGps(uri, legacyFingerprint)));
        assertNotNull(store.get(uri, legacyFingerprint));

        DiscoveryPhotoRef ref = new DiscoveryPhotoRef(
                uri,
                9001L,
                MediaKind.PHOTO,
                "image/jpeg",
                "Beauty_20261015_142310.jpg",
                1792041790000L,
                "__NO_LOCATION__",
                "위치 정보 없음",
                "",
                "",
                "",
                "",
                "DCIM/QuickBeauty/",
                1L,
                1L,
                false
        );
        String newFingerprint = PersonalPlaceGpsPoint.buildFingerprint(ref);
        assertTrue(newFingerprint.endsWith(":qb2"));
        // Stale legacy NO_GPS_TAG entry is bypassed because the new fingerprint includes :qb2.
        assertNull(store.get(uri, newFingerprint));
    }

    @Test
    public void resolvesMultiStepEditChainAndPreventsCycles() throws Exception {
        String dt = "2026:10:15 14:23:10";
        long takenMs = parseExifMillis(dt);
        FakeSourceRepository repo = new FakeSourceRepository();

        QuickBeautySourceLink.ParsedLink linkToOriginalO = QuickBeautySourceLink.parseMarker(
                "QuickBeauty|src_id=100|src_name=20261015_142310.jpg|src_dt=" + dt + "|src_taken=" + takenMs
        );
        QuickBeautySourceLink.ParsedLink linkToIntermediateA = QuickBeautySourceLink.parseMarker(
                "QuickBeauty|src_id=200|src_name=Beauty_20261015_142310.jpg|src_dt=" + dt + "|src_taken=" + takenMs
        );

        // Original O (id=100) has the real GPS.
        repo.put(
                100L,
                "20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(true, 37.5112d, 127.0980d, dt, null)
        );
        // Intermediate edit A (id=200) has no direct GPS, but links to O (id=100).
        repo.put(
                200L,
                "Beauty_20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(false, 0.0d, 0.0d, dt, linkToOriginalO)
        );

        // Edit B links to A -> should traverse B -> A -> O and find O's GPS.
        QuickBeautySourceLink.Resolution chainRes = QuickBeautySourceLink.resolve(repo, linkToIntermediateA, true);
        assertEquals(QuickBeautySourceLink.Status.FOUND, chainRes.status);
        assertEquals(37.5112d, chainRes.latitude, 1e-4d);
        assertEquals(127.0980d, chainRes.longitude, 1e-4d);

        // Cycle test: A (200) -> O (100) -> A (200) with no GPS.
        repo.put(
                100L,
                "20261015_142310.jpg",
                takenMs,
                QuickBeautySourceLink.CandidateInspection.opened(false, 0.0d, 0.0d, dt, linkToIntermediateA)
        );
        QuickBeautySourceLink.Resolution cycleRes = QuickBeautySourceLink.resolve(repo, linkToIntermediateA, true);
        assertFalse(cycleRes.hasCoordinates());
        assertEquals(QuickBeautySourceLink.Status.AMBIGUOUS, cycleRes.status);
    }
}

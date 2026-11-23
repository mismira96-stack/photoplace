package com.example.gallerysorter;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves original capture date and GPS coordinates for photos saved by QuickBeauty
 * using the embedded EXIF source marker ("QuickBeauty|src_id=...|src_name=...|src_dt=...|src_taken=...").
 *
 * Enforces candidate identity verification (name + timestamp cross-check), ambiguity rejection,
 * transient failure distinction (never caching transient source read failures as confirmed NO_GPS),
 * and bounded multi-hop chain resolution (O -> A -> B) with cycle protection.
 */
final class QuickBeautySourceLink {
    static final String MARKER_PREFIX = "QuickBeauty|";
    static final int MAX_CHAIN_DEPTH = 3;
    static final long TIMESTAMP_TOLERANCE_MILLIS = 2000L;

    enum Status {
        NOT_LINKED,
        FOUND,
        CONFIRMED_NO_GPS,
        DEFERRED_OR_UNAVAILABLE,
        AMBIGUOUS
    }

    static final class Resolution {
        final Status status;
        final double latitude;
        final double longitude;

        private Resolution(Status status, double latitude, double longitude) {
            this.status = status;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        static Resolution notLinked() {
            return new Resolution(Status.NOT_LINKED, 0.0d, 0.0d);
        }

        static Resolution found(double latitude, double longitude) {
            return new Resolution(Status.FOUND, latitude, longitude);
        }

        static Resolution confirmedNoGps() {
            return new Resolution(Status.CONFIRMED_NO_GPS, 0.0d, 0.0d);
        }

        static Resolution deferred() {
            return new Resolution(Status.DEFERRED_OR_UNAVAILABLE, 0.0d, 0.0d);
        }

        static Resolution ambiguous() {
            return new Resolution(Status.AMBIGUOUS, 0.0d, 0.0d);
        }

        boolean hasCoordinates() {
            return status == Status.FOUND && PersonalPlace.isUsableCoordinate(latitude, longitude);
        }
    }

    static final class ParsedLink {
        final long sourceId;
        final String sourceName;
        final String sourceExifDateTime;
        final long sourceTakenMillis;

        ParsedLink(long sourceId, String sourceName, String sourceExifDateTime, long sourceTakenMillis) {
            this.sourceId = sourceId;
            this.sourceName = sourceName == null ? "" : sourceName.trim();
            this.sourceExifDateTime = sourceExifDateTime == null ? "" : sourceExifDateTime.trim();
            this.sourceTakenMillis = sourceTakenMillis;
        }

        long expectedTakenMillis() {
            Date exifDate = parseExifDateString(sourceExifDateTime);
            if (exifDate != null) {
                return exifDate.getTime();
            }
            return sourceTakenMillis > 0L ? sourceTakenMillis : 0L;
        }

        /**
         * A bare numeric ID without any filename or capture time cannot prove identity across
         * device restores or MediaStore re-indexing.
         */
        boolean hasIdentityProofForIdLookup() {
            return !sourceName.isEmpty() || expectedTakenMillis() > 0L;
        }

        /**
         * Fallback lookup (when ID changed or is absent) requires BOTH filename and capture time
         * so generic filenames or same-second bursts do not falsely match unrelated photos.
         */
        boolean hasIdentityProofForFallbackLookup() {
            return !sourceName.isEmpty() && expectedTakenMillis() > 0L;
        }

        boolean hasAnyLookupKey() {
            return sourceId > 0L || !sourceName.isEmpty() || expectedTakenMillis() > 0L;
        }

        String signatureKey() {
            return sourceId + "|" + sourceName.toLowerCase(Locale.US) + "|" + sourceExifDateTime + "|" + sourceTakenMillis;
        }
    }

    static final class CandidateMedia {
        final long id;
        final String displayName;
        final long dateTakenMillis;

        CandidateMedia(long id, String displayName, long dateTakenMillis) {
            this.id = id;
            this.displayName = displayName == null ? "" : displayName.trim();
            this.dateTakenMillis = dateTakenMillis;
        }
    }

    static final class CandidateInspection {
        final boolean opened;
        final boolean hasGps;
        final double latitude;
        final double longitude;
        final String exifDateTime;
        final ParsedLink parentLink;

        private CandidateInspection(boolean opened,
                                    boolean hasGps,
                                    double latitude,
                                    double longitude,
                                    String exifDateTime,
                                    ParsedLink parentLink) {
            this.opened = opened;
            this.hasGps = hasGps;
            this.latitude = latitude;
            this.longitude = longitude;
            this.exifDateTime = exifDateTime == null ? "" : exifDateTime.trim();
            this.parentLink = parentLink;
        }

        static CandidateInspection unopened() {
            return new CandidateInspection(false, false, 0.0d, 0.0d, "", null);
        }

        static CandidateInspection opened(boolean hasGps,
                                          double latitude,
                                          double longitude,
                                          String exifDateTime,
                                          ParsedLink parentLink) {
            return new CandidateInspection(true, hasGps, latitude, longitude, exifDateTime, parentLink);
        }
    }

    interface SourceRepository {
        CandidateMedia findById(long id);
        List<CandidateMedia> findByDisplayName(String displayName, int limit);
        CandidateInspection inspectCandidate(long id, boolean mediaLocationAllowed);
    }

    private QuickBeautySourceLink() {
    }

    static ParsedLink parseFromExif(ExifInterface exif) {
        if (exif == null) {
            return null;
        }
        ParsedLink fromDesc = parseMarker(exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION));
        if (fromDesc != null) {
            return fromDesc;
        }
        return parseMarker(exif.getAttribute(ExifInterface.TAG_USER_COMMENT));
    }

    static ParsedLink parseMarker(String raw) {
        if (raw == null) {
            return null;
        }
        int start = raw.indexOf(MARKER_PREFIX);
        if (start < 0) {
            return null;
        }
        String payload = raw.substring(start + MARKER_PREFIX.length()).trim();
        if (payload.isEmpty()) {
            return null;
        }
        long srcId = 0L;
        String srcName = "";
        String srcDt = "";
        long srcTaken = 0L;
        String[] parts = payload.split("\\|");
        for (String part : parts) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = part.substring(0, eq).trim();
            String val = part.substring(eq + 1).trim();
            if (val.isEmpty()) {
                continue;
            }
            if ("src_id".equals(key)) {
                srcId = parsePositiveLong(val);
            } else if ("src_name".equals(key)) {
                srcName = val;
            } else if ("src_dt".equals(key)) {
                srcDt = val;
            } else if ("src_taken".equals(key)) {
                srcTaken = parsePositiveLong(val);
            }
        }
        ParsedLink link = new ParsedLink(srcId, srcName, srcDt, srcTaken);
        return link.hasAnyLookupKey() || !link.sourceExifDateTime.isEmpty() ? link : null;
    }

    static Date parseTakenDate(ParsedLink link) {
        if (link == null) {
            return null;
        }
        Date parsed = parseExifDateString(link.sourceExifDateTime);
        if (parsed != null) {
            return parsed;
        }
        if (link.sourceTakenMillis > 0L) {
            return new Date(link.sourceTakenMillis);
        }
        return null;
    }

    private static Date parseExifDateString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
            fmt.setLenient(false);
            return fmt.parse(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    static Resolution resolve(Context context, ExifInterface exif, boolean mediaLocationAllowed) {
        if (exif == null) {
            return Resolution.notLinked();
        }
        ParsedLink link = parseFromExif(exif);
        if (link == null) {
            return Resolution.notLinked();
        }
        if (context == null) {
            return Resolution.deferred();
        }
        return resolve(new ContentResolverSourceRepository(context.getContentResolver()), link, mediaLocationAllowed);
    }

    static Resolution resolve(Context context, ParsedLink link, boolean mediaLocationAllowed) {
        if (link == null) {
            return Resolution.notLinked();
        }
        if (context == null) {
            return Resolution.deferred();
        }
        return resolve(new ContentResolverSourceRepository(context.getContentResolver()), link, mediaLocationAllowed);
    }

    static double[] resolveCoordinates(Context context, ExifInterface exif, boolean mediaLocationAllowed) {
        Resolution res = resolve(context, exif, mediaLocationAllowed);
        return res.hasCoordinates() ? new double[]{res.latitude, res.longitude} : null;
    }

    static double[] resolveCoordinates(Context context, ParsedLink link, boolean mediaLocationAllowed) {
        Resolution res = resolve(context, link, mediaLocationAllowed);
        return res.hasCoordinates() ? new double[]{res.latitude, res.longitude} : null;
    }

    static Resolution resolve(SourceRepository repo, ParsedLink initialLink, boolean mediaLocationAllowed) {
        if (initialLink == null) {
            return Resolution.notLinked();
        }
        if (repo == null) {
            return Resolution.deferred();
        }
        Set<Long> visitedIds = new HashSet<>();
        Set<String> visitedMarkers = new HashSet<>();
        ParsedLink currentLink = initialLink;

        for (int depth = 0; depth < MAX_CHAIN_DEPTH; depth++) {
            if (currentLink == null) {
                return Resolution.deferred();
            }
            String sig = currentLink.signatureKey();
            if (!visitedMarkers.add(sig)) {
                return Resolution.ambiguous();
            }

            VerifiedStep step = resolveSingleStep(repo, currentLink, mediaLocationAllowed, visitedIds);
            if (step.terminalResolution != null) {
                return step.terminalResolution;
            }
            if (step.matchedMedia == null || step.inspection == null) {
                return Resolution.deferred();
            }
            visitedIds.add(step.matchedMedia.id);

            if (step.inspection.hasGps
                    && PersonalPlace.isUsableCoordinate(step.inspection.latitude, step.inspection.longitude)) {
                return Resolution.found(step.inspection.latitude, step.inspection.longitude);
            }

            if (step.inspection.parentLink != null) {
                if (step.inspection.parentLink.sourceId > 0L
                        && visitedIds.contains(step.inspection.parentLink.sourceId)) {
                    return Resolution.ambiguous();
                }
                currentLink = step.inspection.parentLink;
                continue;
            }

            if (Build.VERSION.SDK_INT >= 29 && !mediaLocationAllowed) {
                return Resolution.deferred();
            }
            return Resolution.confirmedNoGps();
        }
        return Resolution.ambiguous();
    }

    private static final class VerifiedStep {
        final CandidateMedia matchedMedia;
        final CandidateInspection inspection;
        final Resolution terminalResolution;

        private VerifiedStep(CandidateMedia matchedMedia,
                             CandidateInspection inspection,
                             Resolution terminalResolution) {
            this.matchedMedia = matchedMedia;
            this.inspection = inspection;
            this.terminalResolution = terminalResolution;
        }

        static VerifiedStep matched(CandidateMedia media, CandidateInspection inspection) {
            return new VerifiedStep(media, inspection, null);
        }

        static VerifiedStep terminal(Resolution resolution) {
            return new VerifiedStep(null, null, resolution);
        }
    }

    private static VerifiedStep resolveSingleStep(SourceRepository repo,
                                                  ParsedLink link,
                                                  boolean mediaLocationAllowed,
                                                  Set<Long> visitedIds) {
        if (!link.hasIdentityProofForIdLookup()) {
            return VerifiedStep.terminal(Resolution.ambiguous());
        }

        boolean sawUnopenedMatchingRow = false;

        // 1. Primary lookup by src_id hint, with strict name/timestamp verification.
        if (link.sourceId > 0L) {
            if (visitedIds.contains(link.sourceId)) {
                return VerifiedStep.terminal(Resolution.ambiguous());
            }
            CandidateMedia byId = repo.findById(link.sourceId);
            if (byId != null && matchesMediaRowPreliminarily(link, byId)) {
                CandidateInspection inspection = repo.inspectCandidate(byId.id, mediaLocationAllowed);
                if (!inspection.opened) {
                    sawUnopenedMatchingRow = true;
                } else if (matchesFullIdentity(link, byId, inspection)) {
                    return VerifiedStep.matched(byId, inspection);
                }
            }
        }

        // 2. Fallback lookup when src_id is missing, moved, or rejected by identity check.
        if (!link.hasIdentityProofForFallbackLookup()) {
            return VerifiedStep.terminal(sawUnopenedMatchingRow ? Resolution.deferred() : Resolution.ambiguous());
        }

        List<CandidateMedia> byName = repo.findByDisplayName(link.sourceName, 10);
        if (byName == null || byName.isEmpty()) {
            return VerifiedStep.terminal(Resolution.deferred());
        }

        List<CandidateMedia> verifiedMedia = new ArrayList<>(2);
        List<CandidateInspection> verifiedInspections = new ArrayList<>(2);

        for (CandidateMedia candidate : byName) {
            if (candidate == null || candidate.id <= 0L) {
                continue;
            }
            if (visitedIds.contains(candidate.id)) {
                return VerifiedStep.terminal(Resolution.ambiguous());
            }
            if (!matchesMediaRowPreliminarily(link, candidate)) {
                continue;
            }
            CandidateInspection inspection = repo.inspectCandidate(candidate.id, mediaLocationAllowed);
            if (!inspection.opened) {
                sawUnopenedMatchingRow = true;
                continue;
            }
            if (matchesFullIdentity(link, candidate, inspection)) {
                verifiedMedia.add(candidate);
                verifiedInspections.add(inspection);
                if (verifiedMedia.size() > 1) {
                    return VerifiedStep.terminal(Resolution.ambiguous());
                }
            }
        }

        if (verifiedMedia.size() == 1) {
            if (sawUnopenedMatchingRow) {
                // Another candidate with matching row metadata could not be opened to disambiguate.
                return VerifiedStep.terminal(Resolution.deferred());
            }
            return VerifiedStep.matched(verifiedMedia.get(0), verifiedInspections.get(0));
        }

        return VerifiedStep.terminal(Resolution.deferred());
    }

    static boolean matchesMediaRowPreliminarily(ParsedLink link, CandidateMedia candidate) {
        if (link == null || candidate == null) {
            return false;
        }
        if (!link.sourceName.isEmpty()) {
            if (candidate.displayName.isEmpty()
                    || !link.sourceName.equalsIgnoreCase(candidate.displayName)) {
                return false;
            }
        }
        long expectedMs = link.expectedTakenMillis();
        if (expectedMs > 0L && candidate.dateTakenMillis > 0L) {
            if (Math.abs(candidate.dateTakenMillis - expectedMs) > TIMESTAMP_TOLERANCE_MILLIS) {
                return false;
            }
        }
        return true;
    }

    static boolean matchesFullIdentity(ParsedLink link,
                                       CandidateMedia candidate,
                                       CandidateInspection inspection) {
        if (!matchesMediaRowPreliminarily(link, candidate) || inspection == null || !inspection.opened) {
            return false;
        }
        long expectedMs = link.expectedTakenMillis();
        if (expectedMs <= 0L) {
            return !link.sourceName.isEmpty();
        }
        Date candidateExifDate = parseExifDateString(inspection.exifDateTime);
        long candidateExifMs = candidateExifDate != null ? candidateExifDate.getTime() : 0L;
        long candidateParentMs = inspection.parentLink != null ? inspection.parentLink.expectedTakenMillis() : 0L;

        boolean hasAnyCandidateTimestamp = candidate.dateTakenMillis > 0L
                || candidateExifMs > 0L
                || candidateParentMs > 0L;
        if (!hasAnyCandidateTimestamp) {
            return false;
        }
        if (candidate.dateTakenMillis > 0L
                && Math.abs(candidate.dateTakenMillis - expectedMs) > TIMESTAMP_TOLERANCE_MILLIS) {
            return false;
        }
        if (candidateExifMs > 0L
                && Math.abs(candidateExifMs - expectedMs) > TIMESTAMP_TOLERANCE_MILLIS) {
            return false;
        }
        if (candidateParentMs > 0L
                && Math.abs(candidateParentMs - expectedMs) > TIMESTAMP_TOLERANCE_MILLIS) {
            return false;
        }
        return true;
    }

    private static final class ContentResolverSourceRepository implements SourceRepository {
        private static final String[] PROJECTION = new String[]{"_id", "_display_name", "datetaken"};
        private final ContentResolver resolver;

        ContentResolverSourceRepository(ContentResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public CandidateMedia findById(long id) {
            if (resolver == null || id <= 0L) {
                return null;
            }
            Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            try (Cursor cursor = resolver.query(uri, PROJECTION, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    return readRow(cursor, id);
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        @Override
        public List<CandidateMedia> findByDisplayName(String displayName, int limit) {
            if (resolver == null || displayName == null || displayName.trim().isEmpty()) {
                return Collections.emptyList();
            }
            List<CandidateMedia> out = new ArrayList<>(4);
            try (Cursor cursor = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    "_display_name = ?",
                    new String[]{displayName.trim()},
                    "_id ASC"
            )) {
                if (cursor == null) {
                    return out;
                }
                while (cursor.moveToNext() && out.size() < limit) {
                    CandidateMedia item = readRow(cursor, 0L);
                    if (item != null && item.id > 0L) {
                        out.add(item);
                    }
                }
            } catch (Exception ignored) {
            }
            return out;
        }

        @Override
        public CandidateInspection inspectCandidate(long id, boolean mediaLocationAllowed) {
            if (resolver == null || id <= 0L) {
                return CandidateInspection.unopened();
            }
            Uri rawUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            Uri targetUri = PersonalPlaceGpsReader.prepareOriginalUri(rawUri, mediaLocationAllowed);
            CandidateInspection primary = inspectUri(targetUri);
            if (primary.opened) {
                return primary;
            }
            if (!rawUri.equals(targetUri)) {
                return inspectUri(rawUri);
            }
            return CandidateInspection.unopened();
        }

        private CandidateInspection inspectUri(Uri uri) {
            if (uri == null) {
                return CandidateInspection.unopened();
            }
            try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r")) {
                if (pfd != null) {
                    return extractInspection(new ExifInterface(pfd.getFileDescriptor()));
                }
            } catch (Exception ignored) {
            }
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in != null) {
                    return extractInspection(new ExifInterface(in));
                }
            } catch (Exception ignored) {
            }
            return CandidateInspection.unopened();
        }

        private static CandidateInspection extractInspection(ExifInterface exif) {
            float[] latLong = new float[2];
            boolean hasGps = exif.getLatLong(latLong)
                    && PersonalPlace.isUsableCoordinate(latLong[0], latLong[1]);
            String dt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if (dt == null || dt.trim().isEmpty()) {
                dt = exif.getAttribute(ExifInterface.TAG_DATETIME);
            }
            ParsedLink parent = parseFromExif(exif);
            return CandidateInspection.opened(
                    hasGps,
                    hasGps ? latLong[0] : 0.0d,
                    hasGps ? latLong[1] : 0.0d,
                    dt,
                    parent
            );
        }

        private static CandidateMedia readRow(Cursor cursor, long fallbackId) {
            int idIdx = cursor.getColumnIndex("_id");
            int nameIdx = cursor.getColumnIndex("_display_name");
            int takenIdx = cursor.getColumnIndex("datetaken");
            long id = (idIdx >= 0 && !cursor.isNull(idIdx)) ? cursor.getLong(idIdx) : fallbackId;
            String name = (nameIdx >= 0 && !cursor.isNull(nameIdx)) ? cursor.getString(nameIdx) : "";
            long taken = (takenIdx >= 0 && !cursor.isNull(takenIdx)) ? cursor.getLong(takenIdx) : 0L;
            return new CandidateMedia(id, name, taken);
        }
    }

    private static long parsePositiveLong(String text) {
        try {
            long value = Long.parseLong(text);
            return value > 0L ? value : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }
}

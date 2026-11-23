package com.example.gallerysorter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Reads original media GPS coordinates for a single record's photo references,
 * reusing PersonalPlaceGpsCacheStore and respecting Android 10+ ACCESS_MEDIA_LOCATION.
 */
final class PersonalPlaceGpsReader {
    private static final int COMMIT_BATCH_SIZE = 40;

    private final Context context;
    private final PersonalPlaceGpsCacheStore cacheStore;

    PersonalPlaceGpsReader(Context context) {
        this(context, new PersonalPlaceGpsCacheStore(context));
    }

    PersonalPlaceGpsReader(Context context, PersonalPlaceGpsCacheStore cacheStore) {
        this.context = context.getApplicationContext();
        this.cacheStore = cacheStore;
    }

    Map<String, PersonalPlaceGpsPoint> resolveForGroup(List<DiscoveryPhotoRef> refs,
                                                       BiConsumer<Integer, Integer> progress,
                                                       BooleanSupplier cancelled) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyMap();
        }
        BooleanSupplier isCancelled = cancelled == null ? () -> false : cancelled;
        BiConsumer<Integer, Integer> onProgress = progress == null ? (done, total) -> { } : progress;

        LinkedHashMap<String, PersonalPlaceGpsPoint> result = new LinkedHashMap<>();
        ArrayList<PersonalPlaceGpsPoint> pendingCacheWrites = new ArrayList<>();
        int total = refs.size();
        int done = 0;

        for (DiscoveryPhotoRef ref : refs) {
            if (isCancelled.getAsBoolean()) {
                break;
            }
            done++;
            if (ref == null || ref.stale || ref.sourceUri == null || ref.sourceUri.trim().isEmpty()) {
                continue;
            }
            String uriStr = ref.sourceUri.trim();
            String fingerprint = PersonalPlaceGpsPoint.buildFingerprint(ref);
            PersonalPlaceGpsPoint cached = cacheStore.get(uriStr, fingerprint);
            if (cached != null) {
                result.put(uriStr, cached);
                if (done == 1 || done % 25 == 0 || done == total) {
                    onProgress.accept(done, total);
                }
                continue;
            }

            PersonalPlaceGpsPoint readPoint = readSingle(ref, fingerprint);
            result.put(uriStr, readPoint);
            if (readPoint.isCacheableOnDisk()) {
                pendingCacheWrites.add(readPoint);
                if (pendingCacheWrites.size() >= COMMIT_BATCH_SIZE) {
                    cacheStore.putAll(pendingCacheWrites);
                    pendingCacheWrites.clear();
                }
            }
            if (done == 1 || done % 15 == 0 || done == total) {
                onProgress.accept(done, total);
            }
        }

        if (!pendingCacheWrites.isEmpty()) {
            cacheStore.putAll(pendingCacheWrites);
        }
        return result;
    }

    PersonalPlaceGpsPoint readSingle(DiscoveryPhotoRef ref, String fingerprint) {
        if (ref == null || ref.sourceUri == null || ref.sourceUri.trim().isEmpty()) {
            return PersonalPlaceGpsPoint.deferred("", fingerprint);
        }
        String uriStr = ref.sourceUri.trim();
        Uri rawUri;
        try {
            rawUri = Uri.parse(uriStr);
        } catch (Exception invalid) {
            return PersonalPlaceGpsPoint.deferred(uriStr, fingerprint);
        }

        boolean mediaLocationAllowed = hasMediaLocationPermission();
        Uri targetUri = prepareOriginalUri(rawUri, mediaLocationAllowed);

        if (ref.mediaKind == MediaKind.VIDEO) {
            return readVideoPoint(uriStr, fingerprint, targetUri, rawUri, mediaLocationAllowed);
        }
        return readPhotoPoint(uriStr, fingerprint, targetUri, rawUri, mediaLocationAllowed);
    }

    private static final class ExifReadAttempt {
        final boolean opened;
        final boolean hasDirectGps;
        final double latitude;
        final double longitude;
        final QuickBeautySourceLink.Resolution linkedResolution;

        ExifReadAttempt(boolean opened,
                        boolean hasDirectGps,
                        double latitude,
                        double longitude,
                        QuickBeautySourceLink.Resolution linkedResolution) {
            this.opened = opened;
            this.hasDirectGps = hasDirectGps;
            this.latitude = latitude;
            this.longitude = longitude;
            this.linkedResolution = linkedResolution == null
                    ? QuickBeautySourceLink.Resolution.notLinked()
                    : linkedResolution;
        }
    }

    private PersonalPlaceGpsPoint readPhotoPoint(String uriStr,
                                                 String fingerprint,
                                                 Uri targetUri,
                                                 Uri fallbackUri,
                                                 boolean mediaLocationAllowed) {
        boolean openedAny = false;
        QuickBeautySourceLink.Status linkedStatus = QuickBeautySourceLink.Status.NOT_LINKED;

        ExifReadAttempt primary = tryReadPhotoExif(targetUri, mediaLocationAllowed);
        if (primary.opened) {
            openedAny = true;
            if (primary.hasDirectGps) {
                return PersonalPlaceGpsPoint.withCoordinates(uriStr, fingerprint, primary.latitude, primary.longitude);
            }
            if (primary.linkedResolution.hasCoordinates()) {
                return PersonalPlaceGpsPoint.withCoordinates(
                        uriStr, fingerprint, primary.linkedResolution.latitude, primary.linkedResolution.longitude);
            }
            if (primary.linkedResolution.status != QuickBeautySourceLink.Status.NOT_LINKED) {
                linkedStatus = primary.linkedResolution.status;
            }
        }
        if (!targetUri.equals(fallbackUri)) {
            ExifReadAttempt fallback = tryReadPhotoExif(fallbackUri, mediaLocationAllowed);
            if (fallback.opened) {
                openedAny = true;
                if (fallback.hasDirectGps) {
                    return PersonalPlaceGpsPoint.withCoordinates(
                            uriStr, fingerprint, fallback.latitude, fallback.longitude);
                }
                if (fallback.linkedResolution.hasCoordinates()) {
                    return PersonalPlaceGpsPoint.withCoordinates(
                            uriStr, fingerprint, fallback.linkedResolution.latitude, fallback.linkedResolution.longitude);
                }
                if (linkedStatus == QuickBeautySourceLink.Status.NOT_LINKED
                        && fallback.linkedResolution.status != QuickBeautySourceLink.Status.NOT_LINKED) {
                    linkedStatus = fallback.linkedResolution.status;
                }
            }
        }
        if (!openedAny) {
            return PersonalPlaceGpsPoint.deferred(uriStr, fingerprint);
        }
        if (linkedStatus == QuickBeautySourceLink.Status.DEFERRED_OR_UNAVAILABLE
                || linkedStatus == QuickBeautySourceLink.Status.AMBIGUOUS) {
            return PersonalPlaceGpsPoint.deferred(uriStr, fingerprint);
        }
        if (Build.VERSION.SDK_INT >= 29 && !mediaLocationAllowed) {
            // Without ACCESS_MEDIA_LOCATION on Android 10+, redacted 0,0 must not be permanently cached as NO_GPS_TAG.
            return PersonalPlaceGpsPoint.deferred(uriStr, fingerprint);
        }
        return PersonalPlaceGpsPoint.noGps(uriStr, fingerprint);
    }

    private PersonalPlaceGpsPoint readVideoPoint(String uriStr,
                                                 String fingerprint,
                                                 Uri targetUri,
                                                 Uri fallbackUri,
                                                 boolean mediaLocationAllowed) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            try {
                retriever.setDataSource(context, targetUri);
            } catch (Exception firstError) {
                if (!targetUri.equals(fallbackUri)) {
                    retriever.setDataSource(context, fallbackUri);
                } else {
                    return PersonalPlaceGpsPoint.deferred(uriStr, fingerprint);
                }
            }
            String location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
            double[] parsed = parseIso6709(location);
            if (parsed != null && PersonalPlace.isUsableCoordinate(parsed[0], parsed[1])) {
                return PersonalPlaceGpsPoint.withCoordinates(uriStr, fingerprint, parsed[0], parsed[1]);
            }
            if (Build.VERSION.SDK_INT >= 29 && !mediaLocationAllowed) {
                return PersonalPlaceGpsPoint.deferred(uriStr, fingerprint);
            }
            return PersonalPlaceGpsPoint.noGps(uriStr, fingerprint);
        } catch (Exception error) {
            return PersonalPlaceGpsPoint.deferred(uriStr, fingerprint);
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private ExifReadAttempt tryReadPhotoExif(Uri uri, boolean mediaLocationAllowed) {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd != null) {
                return evaluateExif(new ExifInterface(pfd.getFileDescriptor()), mediaLocationAllowed);
            }
        } catch (Exception ignored) {
        }
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                return evaluateExif(new ExifInterface(in), mediaLocationAllowed);
            }
        } catch (Exception ignored) {
        }
        return new ExifReadAttempt(false, false, 0.0d, 0.0d, QuickBeautySourceLink.Resolution.notLinked());
    }

    private ExifReadAttempt evaluateExif(ExifInterface exif, boolean mediaLocationAllowed) {
        float[] latLong = new float[2];
        if (exif.getLatLong(latLong) && PersonalPlace.isUsableCoordinate(latLong[0], latLong[1])) {
            return new ExifReadAttempt(
                    true, true, latLong[0], latLong[1], QuickBeautySourceLink.Resolution.notLinked());
        }
        QuickBeautySourceLink.Resolution linked = QuickBeautySourceLink.resolve(context, exif, mediaLocationAllowed);
        return new ExifReadAttempt(true, false, 0.0d, 0.0d, linked);
    }

    static Uri prepareOriginalUri(Uri uri, boolean mediaLocationAllowed) {
        if (uri == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 29 && mediaLocationAllowed && "content".equalsIgnoreCase(uri.getScheme())) {
            try {
                return MediaStore.setRequireOriginal(uri);
            } catch (Exception ignored) {
            }
        }
        return uri;
    }

    boolean hasMediaLocationPermission() {
        if (Build.VERSION.SDK_INT < 29) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static double[] parseIso6709(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int splitIndex = -1;
        for (int i = 1; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '+' || c == '-') {
                splitIndex = i;
                break;
            }
        }
        if (splitIndex <= 0 || splitIndex >= trimmed.length() - 1) {
            return null;
        }
        try {
            double lat = Double.parseDouble(trimmed.substring(0, splitIndex));
            double lng = Double.parseDouble(trimmed.substring(splitIndex));
            if (PersonalPlace.isUsableCoordinate(lat, lng)) {
                return new double[]{lat, lng};
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

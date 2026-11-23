package com.example.gallerysorter;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Incremental cache of raw GPS coordinates for Personal Place matching.
 * Never caches transient read failures or permission denials (READ_DEFERRED_OR_DENIED).
 */
final class PersonalPlaceGpsCacheStore {
    private static final String FILE_NAME = "personal_place_gps_cache.json";
    private static final int SCHEMA_VERSION = 1;
    private static final Object LOCK = new Object();
    private static Map<String, PersonalPlaceGpsPoint> memoryCache;

    private final File file;

    PersonalPlaceGpsCacheStore(Context context) {
        this(new File(context.getApplicationContext().getFilesDir(), FILE_NAME));
    }

    PersonalPlaceGpsCacheStore(File file) {
        this.file = file;
    }

    /**
     * Called opportunistically when MediaMetadataReader reads a new photo's EXIF/Video GPS
     * so auto-refresh can match new photos with zero extra file I/O.
     */
    static void recordObservedFromScanner(Context context, Uri uri, double latitude, double longitude) {
        if (context == null || uri == null) {
            return;
        }
        String uriStr = uri.toString().trim();
        if (uriStr.isEmpty() || !PersonalPlace.isUsableCoordinate(latitude, longitude)) {
            return;
        }
        try {
            if (!PersonalPlaceStore.hasAnyPlaces(context)) {
                return;
            }
            PersonalPlaceGpsCacheStore store = new PersonalPlaceGpsCacheStore(context);
            PersonalPlaceGpsPoint existing = store.get(uriStr, "");
            if (existing != null && existing.hasCoordinates()
                    && Math.abs(existing.latitude - latitude) < 1.0E-6d
                    && Math.abs(existing.longitude - longitude) < 1.0E-6d) {
                return;
            }
            store.putAll(Collections.singletonList(
                    PersonalPlaceGpsPoint.withCoordinates(uriStr, "", latitude, longitude)));
        } catch (Exception ignored) {
        }
    }

    Map<String, PersonalPlaceGpsPoint> readAll() {
        synchronized (LOCK) {
            ensureLoadedLocked();
            return Collections.unmodifiableMap(new LinkedHashMap<>(memoryCache));
        }
    }

    PersonalPlaceGpsPoint get(String sourceUri, String expectedFingerprint) {
        String cleanUri = clean(sourceUri);
        if (cleanUri.isEmpty()) {
            return null;
        }
        synchronized (LOCK) {
            ensureLoadedLocked();
            PersonalPlaceGpsPoint point = memoryCache.get(cleanUri);
            if (point == null) {
                return null;
            }
            String cleanExpected = clean(expectedFingerprint);
            if (!cleanExpected.isEmpty() && !point.fingerprint.isEmpty()
                    && !cleanExpected.equals(point.fingerprint)) {
                return null;
            }
            return point;
        }
    }

    void putAll(Collection<PersonalPlaceGpsPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        synchronized (LOCK) {
            ensureLoadedLocked();
            boolean changed = false;
            for (PersonalPlaceGpsPoint point : points) {
                if (point == null || point.sourceUri.isEmpty() || !point.isCacheableOnDisk()) {
                    continue;
                }
                PersonalPlaceGpsPoint existing = memoryCache.get(point.sourceUri);
                if (existing == null
                        || existing.status != point.status
                        || !existing.fingerprint.equals(point.fingerprint)
                        || Math.abs(existing.latitude - point.latitude) > 1.0E-7d
                        || Math.abs(existing.longitude - point.longitude) > 1.0E-7d) {
                    memoryCache.put(point.sourceUri, point);
                    changed = true;
                }
            }
            if (changed) {
                persistLocked();
            }
        }
    }

    private void ensureLoadedLocked() {
        if (memoryCache != null) {
            return;
        }
        memoryCache = new LinkedHashMap<>();
        if (file == null || !file.exists()) {
            return;
        }
        try {
            byte[] bytes = readBytes(file);
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (root.optInt("schemaVersion", -1) != SCHEMA_VERSION) {
                return;
            }
            JSONObject entries = root.optJSONObject("entries");
            if (entries == null) {
                return;
            }
            for (Iterator<String> keys = entries.keys(); keys.hasNext(); ) {
                String uri = keys.next();
                JSONObject item = entries.optJSONObject(uri);
                if (item == null) {
                    continue;
                }
                String statusStr = item.optString("status", "");
                String fingerprint = item.optString("fingerprint", "");
                if ("HAS_GPS".equals(statusStr)) {
                    double lat = item.optDouble("lat", 0.0d);
                    double lng = item.optDouble("lng", 0.0d);
                    PersonalPlaceGpsPoint pt = PersonalPlaceGpsPoint.withCoordinates(uri, fingerprint, lat, lng);
                    if (pt.hasCoordinates()) {
                        memoryCache.put(uri, pt);
                    }
                } else if ("NO_GPS_TAG".equals(statusStr)) {
                    memoryCache.put(uri, PersonalPlaceGpsPoint.noGps(uri, fingerprint));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void persistLocked() {
        if (file == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File temp = new File(file.getPath() + ".tmp");
        try {
            JSONObject root = new JSONObject();
            root.put("schemaVersion", SCHEMA_VERSION);
            JSONObject entries = new JSONObject();
            for (Map.Entry<String, PersonalPlaceGpsPoint> entry : memoryCache.entrySet()) {
                PersonalPlaceGpsPoint pt = entry.getValue();
                if (pt == null || !pt.isCacheableOnDisk()) {
                    continue;
                }
                JSONObject item = new JSONObject();
                item.put("status", pt.status.name());
                item.put("fingerprint", pt.fingerprint);
                if (pt.hasCoordinates()) {
                    item.put("lat", pt.latitude);
                    item.put("lng", pt.longitude);
                }
                entries.put(entry.getKey(), item);
            }
            root.put("entries", entries);
            byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream out = new FileOutputStream(temp)) {
                out.write(payload);
                out.getFD().sync();
            }
            if (!temp.renameTo(file)) {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(payload);
                    out.getFD().sync();
                }
                temp.delete();
            }
        } catch (Exception ignored) {
            temp.delete();
        }
    }

    private static byte[] readBytes(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(file.length(), 16L * 1024 * 1024)];
            int offset = 0;
            int read;
            while (offset < buffer.length && (read = in.read(buffer, offset, buffer.length - offset)) != -1) {
                offset += read;
            }
            if (offset == buffer.length) {
                return buffer;
            }
            byte[] exact = new byte[offset];
            System.arraycopy(buffer, 0, exact, 0, offset);
            return exact;
        }
    }

    static void resetMemoryCacheForTests() {
        synchronized (LOCK) {
            memoryCache = null;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

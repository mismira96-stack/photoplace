package com.example.gallerysorter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable model for an app-internal personal place (e.g. '우리집', '회사', '카페')
 * scoped to a parent discovery record. Never mutates MediaStore files or gallery albums.
 */
final class PersonalPlace {
    static final int CURRENT_POLICY_VERSION = 1;
    static final int RADIUS_NARROW = 50;
    static final int RADIUS_DEFAULT = 80;
    static final int RADIUS_WIDE = 150;
    static final int RADIUS_CAMPUS = 300;
    static final int RADIUS_COMPLEX = 500;
    static final int RADIUS_TOWN = 1000;
    static final int MAX_EFFECTIVE_RADIUS = 1200;

    final String placeId;
    final String parentStableId;
    final String parentMemoryKey;
    final String name;
    final double centerLat;
    final double centerLng;
    final boolean hasCenterCoordinates;
    final int selectedRadiusMeters;
    final int effectiveRadiusMeters;
    final int policyVersion;
    final long createdAtMillis;
    final long updatedAtMillis;
    final Set<String> includedUris;
    final Set<String> includedSignatures;
    final Set<String> autoMatchedUris;
    final Set<String> excludedUris;
    final Set<String> excludedSignatures;

    PersonalPlace(String placeId,
                  String parentStableId,
                  String parentMemoryKey,
                  String name,
                  double centerLat,
                  double centerLng,
                  boolean hasCenterCoordinates,
                  int selectedRadiusMeters,
                  int effectiveRadiusMeters,
                  int policyVersion,
                  long createdAtMillis,
                  long updatedAtMillis,
                  Collection<String> includedUris,
                  Collection<String> includedSignatures,
                  Collection<String> autoMatchedUris,
                  Collection<String> excludedUris,
                  Collection<String> excludedSignatures) {
        this.placeId = clean(placeId);
        this.parentStableId = clean(parentStableId);
        this.parentMemoryKey = clean(parentMemoryKey);
        this.name = clean(name);
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.hasCenterCoordinates = hasCenterCoordinates && isUsableCoordinate(centerLat, centerLng);
        this.selectedRadiusMeters = clampSelectedRadius(selectedRadiusMeters);
        this.effectiveRadiusMeters = Math.max(this.selectedRadiusMeters,
                Math.min(MAX_EFFECTIVE_RADIUS, effectiveRadiusMeters));
        this.policyVersion = policyVersion <= 0 ? CURRENT_POLICY_VERSION : policyVersion;
        this.createdAtMillis = Math.max(0L, createdAtMillis);
        this.updatedAtMillis = Math.max(this.createdAtMillis, updatedAtMillis);
        this.includedUris = immutableSet(includedUris);
        this.includedSignatures = immutableSet(includedSignatures);
        this.autoMatchedUris = immutableSet(autoMatchedUris);
        this.excludedUris = immutableSet(excludedUris);
        this.excludedSignatures = immutableSet(excludedSignatures);
    }

    static boolean isUsableCoordinate(double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng) || Double.isInfinite(lat) || Double.isInfinite(lng)) {
            return false;
        }
        if (Math.abs(lat) > 90.0d || Math.abs(lng) > 180.0d) {
            return false;
        }
        return Math.abs(lat) > 1.0E-4d || Math.abs(lng) > 1.0E-4d;
    }

    static int clampSelectedRadius(int radius) {
        if (radius <= 0) {
            return RADIUS_DEFAULT;
        }
        return Math.max(RADIUS_NARROW, Math.min(RADIUS_TOWN, radius));
    }

    static String photoSignature(DiscoveryPhotoRef ref) {
        if (ref == null) {
            return "";
        }
        return photoSignature(ref.displayName, ref.takenAtMillis);
    }

    static String photoSignature(MemoryPhotoItem item) {
        if (item == null) {
            return "";
        }
        return photoSignature(item.displayName, item.takenAtMillis);
    }

    static String photoSignature(String displayName, long takenAtMillis) {
        String cleanName = clean(displayName).toLowerCase(Locale.ROOT);
        if (cleanName.isEmpty() || takenAtMillis <= 0L) {
            return "";
        }
        return cleanName + "|" + takenAtMillis;
    }

    boolean matchesParent(String stableId, String memoryKey) {
        String cleanStable = clean(stableId);
        String cleanKey = clean(memoryKey);
        if (!cleanStable.isEmpty() && !parentStableId.isEmpty() && cleanStable.equals(parentStableId)) {
            return true;
        }
        return !cleanKey.isEmpty() && !parentMemoryKey.isEmpty() && cleanKey.equals(parentMemoryKey);
    }

    boolean isExcluded(DiscoveryPhotoRef ref) {
        if (ref == null) {
            return false;
        }
        return isExcluded(ref.sourceUri, photoSignature(ref));
    }

    boolean isExcluded(MemoryPhotoItem item) {
        if (item == null) {
            return false;
        }
        return isExcluded(item.sourceUri, photoSignature(item));
    }

    boolean isExcluded(String sourceUri, String signature) {
        String uri = clean(sourceUri);
        if (!uri.isEmpty() && excludedUris.contains(uri)) {
            return true;
        }
        String sig = clean(signature);
        return !sig.isEmpty() && excludedSignatures.contains(sig);
    }

    boolean isExplicitlyIncluded(DiscoveryPhotoRef ref) {
        if (ref == null || isExcluded(ref)) {
            return false;
        }
        return isExplicitlyIncluded(ref.sourceUri, photoSignature(ref));
    }

    boolean isExplicitlyIncluded(MemoryPhotoItem item) {
        if (item == null || isExcluded(item)) {
            return false;
        }
        return isExplicitlyIncluded(item.sourceUri, photoSignature(item));
    }

    boolean isExplicitlyIncluded(String sourceUri, String signature) {
        if (isExcluded(sourceUri, signature)) {
            return false;
        }
        String uri = clean(sourceUri);
        if (!uri.isEmpty() && includedUris.contains(uri)) {
            return true;
        }
        String sig = clean(signature);
        return !sig.isEmpty() && includedSignatures.contains(sig);
    }

    boolean isAutoMatched(DiscoveryPhotoRef ref) {
        if (ref == null || isExcluded(ref) || isExplicitlyIncluded(ref)) {
            return false;
        }
        return autoMatchedUris.contains(clean(ref.sourceUri));
    }

    boolean isAutoMatched(MemoryPhotoItem item) {
        if (item == null || isExcluded(item) || isExplicitlyIncluded(item)) {
            return false;
        }
        return autoMatchedUris.contains(clean(item.sourceUri));
    }

    boolean contains(DiscoveryPhotoRef ref) {
        return isExplicitlyIncluded(ref) || isAutoMatched(ref);
    }

    boolean contains(MemoryPhotoItem item) {
        return isExplicitlyIncluded(item) || isAutoMatched(item);
    }

    PersonalPlace withAutoMatchedUris(Collection<String> nextAutoUris, long nowMillis) {
        LinkedHashSet<String> filtered = new LinkedHashSet<>();
        if (nextAutoUris != null) {
            for (String uri : nextAutoUris) {
                String cleanUri = clean(uri);
                if (!cleanUri.isEmpty() && !excludedUris.contains(cleanUri) && !includedUris.contains(cleanUri)) {
                    filtered.add(cleanUri);
                }
            }
        }
        if (filtered.equals(this.autoMatchedUris)) {
            return this;
        }
        return new PersonalPlace(
                placeId,
                parentStableId,
                parentMemoryKey,
                name,
                centerLat,
                centerLng,
                hasCenterCoordinates,
                selectedRadiusMeters,
                effectiveRadiusMeters,
                policyVersion,
                createdAtMillis,
                Math.max(updatedAtMillis, nowMillis),
                includedUris,
                includedSignatures,
                filtered,
                excludedUris,
                excludedSignatures);
    }

    PersonalPlace withName(String nextName, long nowMillis) {
        String cleaned = clean(nextName);
        if (cleaned.isEmpty() || cleaned.equals(this.name)) {
            return this;
        }
        return new PersonalPlace(
                placeId,
                parentStableId,
                parentMemoryKey,
                cleaned,
                centerLat,
                centerLng,
                hasCenterCoordinates,
                selectedRadiusMeters,
                effectiveRadiusMeters,
                policyVersion,
                createdAtMillis,
                Math.max(updatedAtMillis, nowMillis),
                includedUris,
                includedSignatures,
                autoMatchedUris,
                excludedUris,
                excludedSignatures);
    }

    JSONObject toJson() throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("placeId", placeId);
        obj.put("parentStableId", parentStableId);
        obj.put("parentMemoryKey", parentMemoryKey);
        obj.put("name", name);
        obj.put("centerLat", centerLat);
        obj.put("centerLng", centerLng);
        obj.put("hasCenterCoordinates", hasCenterCoordinates);
        obj.put("selectedRadiusMeters", selectedRadiusMeters);
        obj.put("effectiveRadiusMeters", effectiveRadiusMeters);
        obj.put("policyVersion", policyVersion);
        obj.put("createdAtMillis", createdAtMillis);
        obj.put("updatedAtMillis", updatedAtMillis);
        obj.put("includedUris", toJsonArray(includedUris));
        obj.put("includedSignatures", toJsonArray(includedSignatures));
        obj.put("autoMatchedUris", toJsonArray(autoMatchedUris));
        obj.put("excludedUris", toJsonArray(excludedUris));
        obj.put("excludedSignatures", toJsonArray(excludedSignatures));
        return obj;
    }

    static PersonalPlace fromJson(JSONObject obj) throws Exception {
        if (obj == null) {
            return null;
        }
        String placeId = clean(obj.optString("placeId", ""));
        String name = clean(obj.optString("name", ""));
        if (placeId.isEmpty() || name.isEmpty()) {
            return null;
        }
        double lat = obj.optDouble("centerLat", 0.0d);
        double lng = obj.optDouble("centerLng", 0.0d);
        boolean hasCenter = obj.optBoolean("hasCenterCoordinates", isUsableCoordinate(lat, lng));
        int selectedRadius = obj.optInt("selectedRadiusMeters", RADIUS_DEFAULT);
        int effectiveRadius = obj.optInt("effectiveRadiusMeters", selectedRadius);
        return new PersonalPlace(
                placeId,
                obj.optString("parentStableId", ""),
                obj.optString("parentMemoryKey", ""),
                name,
                lat,
                lng,
                hasCenter,
                selectedRadius,
                effectiveRadius,
                obj.optInt("policyVersion", CURRENT_POLICY_VERSION),
                obj.optLong("createdAtMillis", 0L),
                obj.optLong("updatedAtMillis", 0L),
                fromJsonArray(obj.optJSONArray("includedUris")),
                fromJsonArray(obj.optJSONArray("includedSignatures")),
                fromJsonArray(obj.optJSONArray("autoMatchedUris")),
                fromJsonArray(obj.optJSONArray("excludedUris")),
                fromJsonArray(obj.optJSONArray("excludedSignatures")));
    }

    private static JSONArray toJsonArray(Collection<String> values) {
        JSONArray array = new JSONArray();
        if (values != null) {
            for (String v : values) {
                String cleaned = clean(v);
                if (!cleaned.isEmpty()) {
                    array.put(cleaned);
                }
            }
        }
        return array;
    }

    private static List<String> fromJsonArray(JSONArray array) {
        if (array == null || array.length() == 0) {
            return Collections.emptyList();
        }
        ArrayList<String> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            String value = clean(array.optString(i, ""));
            if (!value.isEmpty()) {
                list.add(value);
            }
        }
        return list;
    }

    private static Set<String> immutableSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String v : values) {
            String cleaned = clean(v);
            if (!cleaned.isEmpty()) {
                set.add(cleaned);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Atomic JSON store for Personal Places (personal_places.json).
 * Kept completely separate from discovery_snapshot.json and CleanupMemorySnapshotReader
 * so gallery copy, trash, and original cleanup pipelines remain 100% unaffected.
 */
final class PersonalPlaceStore {
    private static final String FILE_NAME = "personal_places.json";
    private static final int SCHEMA_VERSION = 1;
    private static final Object LOCK = new Object();

    private final File file;

    PersonalPlaceStore(Context context) {
        this(new File(context.getApplicationContext().getFilesDir(), FILE_NAME));
    }

    PersonalPlaceStore(File file) {
        this.file = file;
    }

    static boolean hasAnyPlaces(Context context) {
        if (context == null) {
            return false;
        }
        File f = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
        return f.exists() && f.length() > 32L;
    }

    List<PersonalPlace> readAll() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(readAllLocked());
        }
    }

    List<PersonalPlace> listForParent(String parentStableId, String parentMemoryKey) {
        return listForParent(readAll(), parentStableId, parentMemoryKey);
    }

    Function<String, List<PersonalPlace>> snapshotByParent(MemoryIdentityRegistryStore identities) {
        List<PersonalPlace> places = readAll();
        if (places.isEmpty()) return ignored -> Collections.emptyList();

        Map<String, String> aliases = identities.readAliasesSnapshot();
        Supplier<Map<String, String>> aliasSnapshot = () -> aliases;
        return memoryKey -> listForParent(places,
                MemoryIdentityRegistryStore.findStableId(memoryKey, aliasSnapshot), memoryKey);
    }

    private static List<PersonalPlace> listForParent(List<PersonalPlace> all, String parentStableId,
                                                   String parentMemoryKey) {
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<PersonalPlace> matched = new ArrayList<>();
        for (PersonalPlace place : all) {
            if (place != null && place.matchesParent(parentStableId, parentMemoryKey)) {
                matched.add(place);
            }
        }
        return Collections.unmodifiableList(matched);
    }

    PersonalPlace findById(String placeId) {
        String cleanId = clean(placeId);
        if (cleanId.isEmpty()) {
            return null;
        }
        for (PersonalPlace place : readAll()) {
            if (place != null && cleanId.equals(place.placeId)) {
                return place;
            }
        }
        return null;
    }

    /**
     * Saves a new or updated PersonalPlace atomically along with its explicit INCLUDED and EXCLUDED memberships.
     * Enforces 1:1 membership within the same parent record by removing newly included photos from sibling places.
     */
    PersonalPlace savePlace(String existingPlaceId,
                            String parentStableId,
                            String parentMemoryKey,
                            String name,
                            double centerLat,
                            double centerLng,
                            boolean hasCenterCoordinates,
                            int selectedRadiusMeters,
                            int effectiveRadiusMeters,
                            Collection<DiscoveryPhotoRef> includedRefs,
                            Collection<DiscoveryPhotoRef> excludedRefs,
                            long nowMillis) {
        String cleanName = clean(name);
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("장소 이름을 입력해 주세요.");
        }
        synchronized (LOCK) {
            ArrayList<PersonalPlace> all = readAllLocked();
            PersonalPlace existing = null;
            String targetId = clean(existingPlaceId);
            if (!targetId.isEmpty()) {
                for (PersonalPlace place : all) {
                    if (place != null && targetId.equals(place.placeId)) {
                        existing = place;
                        break;
                    }
                }
            }
            String placeId = existing != null ? existing.placeId
                    : "pp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            long createdAt = existing != null ? existing.createdAtMillis : Math.max(1L, nowMillis);

            LinkedHashSet<String> includedUris = new LinkedHashSet<>();
            LinkedHashSet<String> includedSigs = new LinkedHashSet<>();
            if (existing != null) {
                includedUris.addAll(existing.includedUris);
                includedSigs.addAll(existing.includedSignatures);
            }
            if (includedRefs != null) {
                includedUris.clear();
                includedSigs.clear();
                for (DiscoveryPhotoRef ref : includedRefs) {
                    if (ref != null && !clean(ref.sourceUri).isEmpty()) {
                        includedUris.add(clean(ref.sourceUri));
                        String sig = PersonalPlace.photoSignature(ref);
                        if (!sig.isEmpty()) {
                            includedSigs.add(sig);
                        }
                    }
                }
            }

            LinkedHashSet<String> excludedUris = new LinkedHashSet<>();
            LinkedHashSet<String> excludedSigs = new LinkedHashSet<>();
            if (existing != null) {
                excludedUris.addAll(existing.excludedUris);
                excludedSigs.addAll(existing.excludedSignatures);
            }
            if (excludedRefs != null) {
                for (DiscoveryPhotoRef ref : excludedRefs) {
                    if (ref != null && !clean(ref.sourceUri).isEmpty()) {
                        String uri = clean(ref.sourceUri);
                        if (!includedUris.contains(uri)) {
                            excludedUris.add(uri);
                            String sig = PersonalPlace.photoSignature(ref);
                            if (!sig.isEmpty()) {
                                excludedSigs.add(sig);
                            }
                        }
                    }
                }
            }
            // Any photo explicitly included now is no longer excluded
            excludedUris.removeAll(includedUris);
            excludedSigs.removeAll(includedSigs);

            LinkedHashSet<String> autoUris = new LinkedHashSet<>();
            if (existing != null) {
                for (String autoUri : existing.autoMatchedUris) {
                    if (!includedUris.contains(autoUri) && !excludedUris.contains(autoUri)) {
                        autoUris.add(autoUri);
                    }
                }
            }

            PersonalPlace saved = new PersonalPlace(
                    placeId,
                    clean(parentStableId),
                    clean(parentMemoryKey),
                    cleanName,
                    centerLat,
                    centerLng,
                    hasCenterCoordinates,
                    selectedRadiusMeters,
                    effectiveRadiusMeters,
                    PersonalPlace.CURRENT_POLICY_VERSION,
                    createdAt,
                    Math.max(createdAt, nowMillis),
                    includedUris,
                    includedSigs,
                    autoUris,
                    excludedUris,
                    excludedSigs);

            ArrayList<PersonalPlace> updatedList = new ArrayList<>(all.size() + 1);
            boolean replaced = false;
            for (PersonalPlace item : all) {
                if (item == null) {
                    continue;
                }
                if (item.placeId.equals(placeId)) {
                    updatedList.add(saved);
                    replaced = true;
                } else if (item.matchesParent(parentStableId, parentMemoryKey) && !includedUris.isEmpty()) {
                    // Enforce 1:1 membership within the same parent record when user explicitly claims photos
                    LinkedHashSet<String> siblingIncUris = new LinkedHashSet<>(item.includedUris);
                    LinkedHashSet<String> siblingIncSigs = new LinkedHashSet<>(item.includedSignatures);
                    LinkedHashSet<String> siblingAutoUris = new LinkedHashSet<>(item.autoMatchedUris);
                    siblingIncUris.removeAll(includedUris);
                    siblingIncSigs.removeAll(includedSigs);
                    siblingAutoUris.removeAll(includedUris);
                    updatedList.add(new PersonalPlace(
                            item.placeId,
                            item.parentStableId,
                            item.parentMemoryKey,
                            item.name,
                            item.centerLat,
                            item.centerLng,
                            item.hasCenterCoordinates,
                            item.selectedRadiusMeters,
                            item.effectiveRadiusMeters,
                            item.policyVersion,
                            item.createdAtMillis,
                            item.updatedAtMillis,
                            siblingIncUris,
                            siblingIncSigs,
                            siblingAutoUris,
                            item.excludedUris,
                            item.excludedSignatures));
                } else {
                    updatedList.add(item);
                }
            }
            if (!replaced) {
                updatedList.add(saved);
            }
            writeAllLocked(updatedList);
            return saved;
        }
    }

    /** Explicitly excludes selected photos from a PersonalPlace so they never re-appear via auto-matching. */
    PersonalPlace excludePhotos(String placeId, Collection<MemoryPhotoItem> itemsToExclude, long nowMillis) {
        String cleanId = clean(placeId);
        if (cleanId.isEmpty() || itemsToExclude == null || itemsToExclude.isEmpty()) {
            return findById(cleanId);
        }
        synchronized (LOCK) {
            ArrayList<PersonalPlace> all = readAllLocked();
            ArrayList<PersonalPlace> updatedList = new ArrayList<>(all.size());
            PersonalPlace updatedTarget = null;
            for (PersonalPlace place : all) {
                if (place == null) {
                    continue;
                }
                if (!cleanId.equals(place.placeId)) {
                    updatedList.add(place);
                    continue;
                }
                LinkedHashSet<String> incUris = new LinkedHashSet<>(place.includedUris);
                LinkedHashSet<String> incSigs = new LinkedHashSet<>(place.includedSignatures);
                LinkedHashSet<String> autoUris = new LinkedHashSet<>(place.autoMatchedUris);
                LinkedHashSet<String> excUris = new LinkedHashSet<>(place.excludedUris);
                LinkedHashSet<String> excSigs = new LinkedHashSet<>(place.excludedSignatures);
                for (MemoryPhotoItem item : itemsToExclude) {
                    if (item == null || clean(item.sourceUri).isEmpty()) {
                        continue;
                    }
                    String uri = clean(item.sourceUri);
                    String sig = PersonalPlace.photoSignature(item);
                    incUris.remove(uri);
                    autoUris.remove(uri);
                    excUris.add(uri);
                    if (!sig.isEmpty()) {
                        incSigs.remove(sig);
                        excSigs.add(sig);
                    }
                }
                updatedTarget = new PersonalPlace(
                        place.placeId,
                        place.parentStableId,
                        place.parentMemoryKey,
                        place.name,
                        place.centerLat,
                        place.centerLng,
                        place.hasCenterCoordinates,
                        place.selectedRadiusMeters,
                        place.effectiveRadiusMeters,
                        place.policyVersion,
                        place.createdAtMillis,
                        Math.max(place.updatedAtMillis, nowMillis),
                        incUris,
                        incSigs,
                        autoUris,
                        excUris,
                        excSigs);
                updatedList.add(updatedTarget);
            }
            if (updatedTarget != null) {
                writeAllLocked(updatedList);
            }
            return updatedTarget;
        }
    }

    boolean deletePlace(String placeId) {
        String cleanId = clean(placeId);
        if (cleanId.isEmpty()) {
            return false;
        }
        synchronized (LOCK) {
            ArrayList<PersonalPlace> all = readAllLocked();
            ArrayList<PersonalPlace> remaining = new ArrayList<>(all.size());
            boolean removed = false;
            for (PersonalPlace place : all) {
                if (place != null && cleanId.equals(place.placeId)) {
                    removed = true;
                } else if (place != null) {
                    remaining.add(place);
                }
            }
            if (removed) {
                writeAllLocked(remaining);
            }
            return removed;
        }
    }

    /**
     * Evaluates and persists automatic GPS matches for all personal places belonging to a parent group.
     */
    List<PersonalPlace> syncAutoMatchesForGroup(String parentStableId,
                                                String parentMemoryKey,
                                                List<DiscoveryPhotoRef> groupRefs,
                                                Map<String, PersonalPlaceGpsPoint> gpsByUri,
                                                long nowMillis) {
        synchronized (LOCK) {
            ArrayList<PersonalPlace> all = readAllLocked();
            ArrayList<PersonalPlace> parentPlaces = new ArrayList<>();
            for (PersonalPlace place : all) {
                if (place != null && place.matchesParent(parentStableId, parentMemoryKey)) {
                    parentPlaces.add(place);
                }
            }
            if (parentPlaces.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, Set<String>> autoByPlace = PersonalPlaceMatcher.evaluateAutoMatches(
                    parentPlaces, groupRefs, gpsByUri);
            boolean changed = false;
            ArrayList<PersonalPlace> updatedAll = new ArrayList<>(all.size());
            ArrayList<PersonalPlace> updatedParentPlaces = new ArrayList<>(parentPlaces.size());

            for (PersonalPlace place : all) {
                if (place == null) {
                    continue;
                }
                if (!place.matchesParent(parentStableId, parentMemoryKey)) {
                    updatedAll.add(place);
                    continue;
                }
                Set<String> nextAuto = autoByPlace.get(place.placeId);
                PersonalPlace nextPlace = place.withAutoMatchedUris(nextAuto, nowMillis);
                if (nextPlace != place) {
                    changed = true;
                }
                updatedAll.add(nextPlace);
                updatedParentPlaces.add(nextPlace);
            }
            if (changed) {
                writeAllLocked(updatedAll);
            }
            return Collections.unmodifiableList(updatedParentPlaces);
        }
    }

    /**
     * Invoked when DiscoverySnapshotController saves a refreshed snapshot so new photos on app resume
     * automatically enter matching personal places without blocking or re-scanning unrelated groups.
     */
    static void syncSnapshotOnRefresh(Context context,
                                      DiscoverySnapshot snapshot,
                                      MemoryIdentityRegistryStore identityStore) {
        if (context == null || snapshot == null || snapshot.groups == null || snapshot.groups.isEmpty()) {
            return;
        }
        if (!hasAnyPlaces(context)) {
            return;
        }
        try {
            PersonalPlaceStore store = new PersonalPlaceStore(context);
            List<PersonalPlace> allPlaces = store.readAll();
            if (allPlaces.isEmpty()) {
                return;
            }
            PersonalPlaceGpsReader gpsReader = new PersonalPlaceGpsReader(context);
            PersonalPlaceGpsCacheStore cacheStore = new PersonalPlaceGpsCacheStore(context);
            long now = System.currentTimeMillis();

            for (DiscoveryMemoryGroup group : snapshot.groups) {
                if (group == null || group.photoRefs == null || group.photoRefs.isEmpty()) {
                    continue;
                }
                String stableId = identityStore == null ? "" : identityStore.findStableId(group.memoryKey);
                List<PersonalPlace> groupPlaces = store.listForParent(stableId, group.memoryKey);
                if (groupPlaces.isEmpty()) {
                    continue;
                }
                // Only read GPS for photos that are already cached OR are newly added in the latest snapshot version,
                // so auto-refresh never triggers a multi-thousand-photo cold backfill on the background refresh thread.
                LinkedHashMap<String, PersonalPlaceGpsPoint> gpsByUri = new LinkedHashMap<>();
                ArrayList<PersonalPlaceGpsPoint> newlyRead = new ArrayList<>();
                for (DiscoveryPhotoRef ref : group.photoRefs) {
                    if (ref == null || ref.stale || clean(ref.sourceUri).isEmpty()) {
                        continue;
                    }
                    String uri = clean(ref.sourceUri);
                    String fp = PersonalPlaceGpsPoint.buildFingerprint(ref);
                    PersonalPlaceGpsPoint cached = cacheStore.get(uri, fp);
                    if (cached != null) {
                        gpsByUri.put(uri, cached);
                    } else if (ref.firstSeenSnapshotVersion >= snapshot.snapshotVersion && snapshot.snapshotVersion > 0L) {
                        PersonalPlaceGpsPoint fresh = gpsReader.readSingle(ref, fp);
                        gpsByUri.put(uri, fresh);
                        if (fresh.isCacheableOnDisk()) {
                            newlyRead.add(fresh);
                        }
                    }
                }
                if (!newlyRead.isEmpty()) {
                    cacheStore.putAll(newlyRead);
                }
                store.syncAutoMatchesForGroup(stableId, group.memoryKey, group.photoRefs, gpsByUri, now);
            }
        } catch (Exception ignored) {
        }
    }

    /** Checks whether a record (by stableId or memoryKey) has a personal place matching the search query. */
    static boolean matchesSearchQuery(Context context, String stableId, String memoryKey, String normalizedQuery) {
        if (context == null || normalizedQuery == null || normalizedQuery.isEmpty() || !hasAnyPlaces(context)) {
            return false;
        }
        try {
            List<PersonalPlace> places = new PersonalPlaceStore(context).listForParent(stableId, memoryKey);
            for (PersonalPlace place : places) {
                if (place != null && place.name.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private ArrayList<PersonalPlace> readAllLocked() {
        ArrayList<PersonalPlace> result = new ArrayList<>();
        if (file == null || !file.exists()) {
            return result;
        }
        try {
            byte[] bytes = readBytes(file);
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (root.optInt("schemaVersion", -1) != SCHEMA_VERSION) {
                return result;
            }
            JSONArray array = root.optJSONArray("places");
            if (array == null) {
                return result;
            }
            for (int i = 0; i < array.length(); i++) {
                PersonalPlace place = PersonalPlace.fromJson(array.optJSONObject(i));
                if (place != null) {
                    result.add(place);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private void writeAllLocked(List<PersonalPlace> places) {
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
            JSONArray array = new JSONArray();
            if (places != null) {
                for (PersonalPlace place : places) {
                    if (place != null) {
                        array.put(place.toJson());
                    }
                }
            }
            root.put("places", array);
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
        } catch (Exception error) {
            temp.delete();
            throw new IllegalStateException("개인 장소를 저장하지 못했어요.", error);
        }
    }

    private static byte[] readBytes(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(file.length(), 8L * 1024 * 1024)];
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

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

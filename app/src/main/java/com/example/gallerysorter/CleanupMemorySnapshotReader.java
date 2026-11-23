package com.example.gallerysorter;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Strict primary-file reader: never invokes stores that repair backups or migrate user data. */
final class CleanupMemorySnapshotReader implements CleanupReviewCoordinator.SnapshotProvider {
    private static final String[] NAMES = {"discovery_snapshot.json", "memory_identity_registry.json",
            "memory_organization_links.json", "memory_collections.json", "memory_date_notes.json",
            "memory_personalization.json", "album_summary_history.json"};
    private static final String[] KEYS = {"groups", "aliases", "links", "collections", "notes", "memories", "sessions"};
    private final Context context;
    private final Map<String, JSONObject> roots = new LinkedHashMap<>();
    final String token;
    private Set<String> knownPaths;
    private DiscoverySnapshot parsedSnapshot;
    private final ManualPlaceSnapshot manualSnapshot;
    private final List<JSONObject> manualPending;
    private java.util.function.BooleanSupplier canceled = () -> false;
    private java.util.function.BiConsumer<Integer,Integer> progress = (done,total) -> { };
    void observe(java.util.function.BooleanSupplier canceled, java.util.function.BiConsumer<Integer,Integer> progress) {
        this.canceled = canceled; this.progress = progress;
    }
    private void checkCanceled() throws InterruptedException {
        if (canceled.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new InterruptedException("Cleanup projection canceled");
    }

    private final Map<String, String> canonicalUris = new HashMap<>();
    private final Map<String, String> normalizedUris = new HashMap<>();
    private final Set<String> relevantIds = new HashSet<>();
    CleanupMemorySnapshotReader(Context context, List<CleanupEvidenceRecord> selected) throws Exception {
        this(context);
        CleanupMediaAccess access = new CleanupMediaAccess(context);
        // MediaStore's external and external_primary URIs can name the SAME row.
        // Prefer a selected source as the representative, so a target alias cannot hide a cycle.
        for (CleanupEvidenceRecord r : selected) {
            canonicalUris.putIfAbsent(access.key(r.sourceUri), r.sourceUri);
            relevantIds.add(android.net.Uri.parse(r.sourceUri).getLastPathSegment());
        }
        for (CleanupEvidenceRecord r : selected) {
            canonicalUris.putIfAbsent(access.key(r.targetUri), r.targetUri);
            relevantIds.add(android.net.Uri.parse(r.targetUri).getLastPathSegment());
        }
        for (JSONObject root : roots.values()) normalizeTree(root);
    }

    private String normalizeUri(String uri) throws Exception {
        if (!uri.matches("content://media/[^/]+/images/media/[0-9]+")
                || !relevantIds.contains(android.net.Uri.parse(uri).getLastPathSegment())) return uri;
        String result = normalizedUris.get(uri);
        if (result == null) {
            String key = new CleanupMediaAccess(context).key(uri);
            result = canonicalUris.getOrDefault(key, uri);
            normalizedUris.put(uri, result);
        }
        return result;
    }
    private void normalizeTree(Object value) throws Exception {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            for (Iterator<String> keys = object.keys(); keys.hasNext();) {
                String key = keys.next(); Object child = object.get(key);
                if (child instanceof String) object.put(key, normalizeUri((String) child));
                else normalizeTree(child);
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                Object child = array.get(i);
                if (child instanceof String) array.put(i, normalizeUri((String) child)); else normalizeTree(child);
            }
        }
    }
    private DiscoveryPhotoRef normalizeRef(DiscoveryPhotoRef r) throws Exception {
        String uri = normalizeUri(r.sourceUri);
        if (uri.equals(r.sourceUri)) return r;
        return new DiscoveryPhotoRef(uri, r.mediaStoreId, r.mediaKind, r.mimeType, r.displayName,
                r.takenAtMillis, r.locationKey, r.placeName, r.countryCode, r.countryName, r.adminArea,
                r.addressLine, r.sourceRelativePath, r.firstSeenSnapshotVersion, r.lastSeenSnapshotVersion, r.stale,r.manualAssigned);
    }

    CleanupMemorySnapshotReader(Context context) throws Exception {
        this.context = context.getApplicationContext();
        manualSnapshot = new ManualPlaceStore(this.context).readAssignments();
        manualPending = new ManualPlaceStore(this.context).pendingCopies();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long total = 0;
        for (int i = 0; i < NAMES.length; i++) {
            File file = new File(this.context.getFilesDir(), NAMES[i]);
            if (!file.exists() && new File(file.getPath() + ".bak").exists())
                throw new IllegalStateException("Primary snapshot unavailable");
            byte[] bytes = file.exists() ? boundedRead(file, 32L * 1024 * 1024 - total) : new byte[0];
            total += bytes.length;
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
            digest.update(bytes);
            JSONObject root;
            if (!file.exists()) {
                root = new JSONObject().put("schemaVersion", 1).put(KEYS[i],
                        i == 0 || i == 2 || i == 3 || i == 6 ? new JSONArray() : new JSONObject());
            } else {
                root = new JSONObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                if (root.optInt("schemaVersion", -1) != 1 || !root.has(KEYS[i]))
                    throw new IllegalStateException("Unknown snapshot schema");
            }
            boolean array = i == 0 || i == 2 || i == 3 || i == 6;
            if (array ? !(root.get(KEYS[i]) instanceof JSONArray) : !(root.get(KEYS[i]) instanceof JSONObject))
                throw new IllegalStateException("Unknown snapshot payload");
            roots.put(NAMES[i], root);
        }
        String media = new HomeRevisionReader(this.context).read();
        if (media == null) throw new IllegalStateException("Media identity/permissions unavailable");
        if (new ManualPlaceStore(this.context).revision() != manualSnapshot.revision)
            throw new CleanupDependencyRetry.RevisionChanged();
        token = CleanupPairVerifier.hex(digest.digest()) + "|" + media + "|manual:" + manualSnapshot.revision;
    }

    @Override public MemoryDependencyGuard.Snapshot project(List<CleanupEvidenceRecord> selected, boolean excludeSources) throws Exception {
        checkCanceled();
        if (manualSnapshot.hasAssignments() && manualSnapshot.assignments().isEmpty())
            throw new IllegalStateException("수동 위치기록 형식을 확인하지 못했어요. 사진은 변경하지 않았어요.");
        long started = android.os.SystemClock.elapsedRealtime();
        Set<String> sources = new HashSet<>();
        for (CleanupEvidenceRecord pair : selected) sources.add(pair.sourceUri);
        JSONObject aliases = root(1).getJSONObject("aliases");
        Map<String, String> legacyAliases = new LinkedHashMap<>();
        for (Iterator<String> keys = aliases.keys(); keys.hasNext();) {
            String key = keys.next(); legacyAliases.put(key, aliases.getString(key));
        }
        for (Map.Entry<String, String> entry : manualSnapshot.mergedAliases(legacyAliases).entrySet())
            aliases.put(entry.getKey(), entry.getValue());
        JSONArray rawGroups = root(0).getJSONArray("groups");
        if (parsedSnapshot == null) parsedSnapshot = DiscoverySnapshotJson.fromJson(root(0));
        DiscoverySnapshot snapshot = parsedSnapshot;
        if (snapshot.groups.size() != rawGroups.length()) throw new IllegalStateException("Incomplete discovery snapshot");
        // Validate persisted raw structure before adding any manual records to the in-memory projection.
        for(int i=0;i<snapshot.groups.size();i++)
            if(rawGroups.getJSONObject(i).getJSONArray("photoRefs").length()!=snapshot.groups.get(i).photoRefs.size())
                throw new IllegalStateException("Incomplete media references");
        Set<String> manualUnverified=new HashSet<>();
        List<UnresolvedMediaRecord> expected=new ArrayList<>(),selectedManual=new ArrayList<>();
        for(ManualMediaAssignment assignment:manualSnapshot.assignments()) {
            expected.add(assignment.media);
            if(sources.contains(normalizeUri(assignment.media.uri)))selectedManual.add(assignment.media);
        }
        MemoryMediaAccess manualAccess=new MemoryMediaAccess(context);
        Map<String,UnresolvedMediaRecord> live=manualAccess.available(expected,false);
        Map<String,UnresolvedMediaRecord> selectedLive=manualAccess.available(selectedManual,true);
        live.putAll(selectedLive);
        for(UnresolvedMediaRecord row:selectedManual)if(!selectedLive.containsKey(row.assetKey()))manualUnverified.add(normalizeUri(row.uri));
        for(JSONObject pending:manualPending) {
            ManualMediaAssignment source=ManualMediaAssignment.fromJson(pending.getJSONObject("source"));
            String uri=normalizeUri(source.media.uri);if(sources.contains(uri))manualUnverified.add(uri);
        }
        snapshot=EffectiveMediaPlaceResolver.project(snapshot,manualSnapshot,live,true);
        List<DiscoveryMemoryGroup> normalizedManual=new ArrayList<>();
        for(DiscoveryMemoryGroup group:snapshot.groups) {
            List<DiscoveryPhotoRef> refs=new ArrayList<>();for(DiscoveryPhotoRef ref:group.photoRefs)refs.add(normalizeRef(ref));
            normalizedManual.add(EffectiveMediaPlaceResolver.group(group,refs,group.snapshotVersion));
        }
        snapshot=new DiscoverySnapshot(snapshot.schemaVersion,snapshot.snapshotVersion,snapshot.createdAtMillis,snapshot.sourceSignature,
                snapshot.sourceItemCount,normalizedManual,snapshot.analysisPolicyVersion,snapshot.countryIdentityPolicyVersion);
        Map<String,String> effectiveAliases=new LinkedHashMap<>();
        for(Iterator<String> keys=aliases.keys();keys.hasNext();){String k=keys.next();effectiveAliases.put(k,aliases.getString(k));}
        ManualGalleryPolicy manualGallery=new ManualGalleryPolicy(manualSnapshot,live,effectiveAliases);
        List<OrganizationLink> links = new ArrayList<>();
        JSONArray rawLinks = root(2).getJSONArray("links");
        for (int i = 0; i < rawLinks.length(); i++) {
            OrganizationLink link = OrganizationLink.fromJson(rawLinks.getJSONObject(i));
            if (link == null) throw new IllegalStateException("Unknown output link");
            links.add(link);
        }
        Map<String, Set<String>> collections = new HashMap<>();
        JSONArray rawCollections = root(3).getJSONArray("collections");
        for (int i = 0; i < rawCollections.length(); i++) {
            MemoryCollection collection = MemoryCollection.fromJson(rawCollections.getJSONObject(i));
            if (collection == null) throw new IllegalStateException("Unknown collection");
            Set<String> ids = new HashSet<>();
            for (MemoryCollection.Member member : collection.members) ids.add(member.stableMemoryId);
            if (ids.size() != rawCollections.getJSONObject(i).getJSONArray("members").length())
                throw new IllegalStateException("Incomplete collection references");
            collections.put(collection.collectionId, ids);
        }
        Map<String, String> notes = new HashMap<>();
        JSONObject rawNotes = root(4).getJSONObject("notes");
        for (Iterator<String> keys = rawNotes.keys(); keys.hasNext();) {
            String key = keys.next();
            if (!key.matches("memory-id:mem_.+#\\d{8}")) throw new IllegalStateException("Unknown note identity");
            notes.put(key, rawNotes.getJSONObject(key).toString());
        }
        Set<String> direct = new HashSet<>();
        // Persisted cover/raw URI references cannot be remapped merely because bytes match.
        Map<String, Set<String>> referenceKinds = new HashMap<>();
        collectDirectUris(root(3), sources, direct, referenceKinds, "모음 기록");
        collectDirectUris(root(4), sources, direct, referenceKinds, "날짜별 메모");
        collectDirectUris(root(5), sources, direct, referenceKinds, "사용자 대표사진");
        collectDirectUris(root(6), sources, direct, referenceKinds, "앨범 정리 이력");
        Map<String, MemoryDependencyGuard.Memory> memories = new HashMap<>();
        Map<String, Set<String>> sourceMemories = new HashMap<>();
        boolean complete = true;
        Set<String> unverifiedSources = new HashSet<>();
        unverifiedSources.addAll(manualUnverified);
        for (CleanupEvidenceRecord pair : selected) {
            checkCanceled();
            if ("historical".equals(pair.jobId) && !knownOutputPath(pair.targetRelativePath)) {
                unverifiedSources.add(pair.sourceUri);
                referenceKinds.computeIfAbsent(pair.sourceUri, key -> new HashSet<>()).add("앱의 앨범 생성 기록 확인 필요");
            }
        }
        android.util.Log.i("CleanupProjection", "Scope ready ms=" + (android.os.SystemClock.elapsedRealtime()-started));
        MemoryMediaStoreReader gallery = new MemoryMediaStoreReader(context.getContentResolver());
        for (int i = 0; i < snapshot.groups.size(); i++) {
            checkCanceled();
            progress.accept(i + 1, snapshot.groups.size());
            DiscoveryMemoryGroup group = snapshot.groups.get(i);
            Set<String> impacted = new HashSet<>();
            for (DiscoveryPhotoRef ref : group.photoRefs) if (sources.contains(ref.sourceUri)) impacted.add(ref.sourceUri);
            if (impacted.isEmpty()) continue;
            String id = aliases.optString(group.memoryKey, "");
            if (!id.startsWith("mem_")) { complete = false; unverifiedSources.addAll(impacted); continue; }
            OrganizationLink link = latestLink(links, id);
            if (link == null) { complete = false; unverifiedSources.addAll(impacted); continue; }
            for (String uri : impacted) sourceMemories.computeIfAbsent(uri, key -> new HashSet<>()).add(id);
            MemoryRecord record = MemoryRepository.fromDiscoveryGroup(group).withGalleryPolicy(manualGallery);
            MemoryMediaStoreReader.Result output = gallery.read(link.relativePath, record);
            if (output.lookup != MemoryMediaSourcePolicy.GalleryLookup.FOUND) {
                complete = false; unverifiedSources.addAll(impacted); continue;
            }
            List<DiscoveryPhotoRef> keep = new ArrayList<>();
            for (DiscoveryPhotoRef ref : group.photoRefs)
                if (!excludeSources || !sources.contains(ref.sourceUri)) keep.add(ref);
            DiscoveryMemoryGroup projected = copyWithRefs(group, keep);
            MemoryRecord afterRecord = MemoryRepository.fromDiscoveryGroup(projected).withGalleryPolicy(manualGallery);
            List<DiscoveryPhotoRef> normalizedOutput = new ArrayList<>();
            for (DiscoveryPhotoRef ref : output.refs) normalizedOutput.add(normalizeRef(ref));
            MemoryMediaResolution media = MemoryMediaResolver.resolve(afterRecord, link, (path, ignored) -> {
                List<DiscoveryPhotoRef> refs = new ArrayList<>();
                for (DiscoveryPhotoRef ref : normalizedOutput)
                    if (!excludeSources || !sources.contains(ref.sourceUri)) refs.add(ref);
                return refs.isEmpty() ? MemoryMediaStoreReader.Result.missing() : MemoryMediaStoreReader.Result.found(refs);
            });
            Map<String, Set<String>> dates = dates(media.refs);
            Map<String, Set<String>> originalDates = new HashMap<>();
            for (MemoryPhotoSection section : MemoryPhotoSection.fromDiscoveryRefs(group.photoRefs))
                for (MemoryPhotoItem photo : section.photos)
                    if (sources.contains(photo.sourceUri))
                        originalDates.computeIfAbsent(photo.sourceUri, key -> new HashSet<>()).add(section.dateKey);
            MemoryDependencyGuard.Memory previous = memories.get(id);
            if (previous != null) {
                merge(dates, previous.dates); merge(originalDates, previous.originalDates);
            }
            memories.put(id, new MemoryDependencyGuard.Memory(dates, originalDates));
        }
        android.util.Log.i("CleanupProjection", "Projection complete exclude=" + excludeSources + " ms=" + (android.os.SystemClock.elapsedRealtime()-started));
        return new MemoryDependencyGuard.Snapshot(complete, memories, sourceMemories, collections, notes,
                direct, unverifiedSources, referenceKinds);
    }

    @Override public boolean isCurrent() {
        try { return token.equals(new CleanupMemorySnapshotReader(context).token); }
        catch (Exception unknown) { return false; }
    }

    private JSONObject root(int i) { return roots.get(NAMES[i]); }
    boolean knownOutputPath(String path) throws Exception {
        if (knownPaths != null) return knownPaths.contains(CleanupPairVerifier.path(path));
        Set<String> built = new HashSet<>();
        String wanted = CleanupPairVerifier.path(path);
        JSONArray links = root(2).getJSONArray("links");
        for (int i = 0; i < links.length(); i++) {
            OrganizationLink link = OrganizationLink.fromJson(links.getJSONObject(i));
            if (link != null && (link.status == OrganizationLink.Status.SUCCESS || link.status == OrganizationLink.Status.PARTIAL)
                    ) built.add(CleanupPairVerifier.path(link.relativePath));
        }
        JSONArray sessions = root(6).getJSONArray("sessions");
        for (int i = 0; i < sessions.length(); i++) {
            JSONArray albums = sessions.getJSONObject(i).optJSONArray("albums");
            if (albums != null) for (int j = 0; j < albums.length(); j++)
                built.add(CleanupPairVerifier.path(albums.getJSONObject(j).optString("relativePath", "")));
        }
        built.remove(""); knownPaths = Collections.unmodifiableSet(built);
        return knownPaths.contains(wanted);
    }
    private static byte[] boundedRead(File file, long remaining) throws Exception {
        try (FileInputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                if (output.size() + (long) count > remaining)
                    throw new IllegalStateException("Snapshot exceeds review memory budget");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
    private static OrganizationLink latestLink(List<OrganizationLink> links, String id) {
        OrganizationLink latest = null;
        for (OrganizationLink link : links)
            if (link.subjectType == OrganizationLink.SubjectType.MEMORY && link.subjectId.equals(id)
                    && (link.status == OrganizationLink.Status.SUCCESS || link.status == OrganizationLink.Status.PARTIAL)
                    && (latest == null || link.organizedAtMillis >= latest.organizedAtMillis)) latest = link;
        return latest;
    }
    private static DiscoveryMemoryGroup copyWithRefs(DiscoveryMemoryGroup g, List<DiscoveryPhotoRef> refs) {
        return new DiscoveryMemoryGroup(g.memoryKey, g.placeKey, g.placeName, g.countryCode, g.countryName,
                g.adminArea, g.addressLine, g.itemCount, g.photoCount, g.videoCount, g.startDateMillis,
                g.endDateMillis, g.coverUri, refs, g.staleCount, g.snapshotVersion);
    }
    private static Map<String, Set<String>> dates(List<DiscoveryPhotoRef> refs) {
        Map<String, Set<String>> result = new HashMap<>();
        for (MemoryPhotoSection section : MemoryPhotoSection.fromDiscoveryRefs(refs))
            for (MemoryPhotoItem photo : section.photos)
                result.computeIfAbsent(section.dateKey, key -> new HashSet<>()).add(photo.sourceUri);
        return result;
    }
    private static void merge(Map<String, Set<String>> into, Map<String, Set<String>> from) {
        for (Map.Entry<String, Set<String>> e : from.entrySet())
            into.computeIfAbsent(e.getKey(), key -> new HashSet<>()).addAll(e.getValue());
    }
    static void collectDirectUris(Object value, Set<String> selected, Set<String> result,
                                          Map<String, Set<String>> kinds, String kind) throws Exception {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            for (Iterator<String> keys = object.keys(); keys.hasNext();)
                collectDirectUris(object.get(keys.next()), selected, result, kinds, kind);
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) collectDirectUris(array.get(i), selected, result, kinds, kind);
        } else if (value instanceof String && selected.contains(value)) {
            result.add((String) value);
            kinds.computeIfAbsent((String) value, key -> new HashSet<>()).add(kind);
        }
    }
}

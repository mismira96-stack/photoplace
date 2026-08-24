package com.example.gallerysorter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DiscoverySnapshotLiveFilter {
    private static final int QUERY_CHUNK_SIZE = 500;

    private final ContentResolver resolver;

    DiscoverySnapshotLiveFilter(ContentResolver resolver) {
        this.resolver = resolver;
    }

    DiscoverySnapshot filter(DiscoverySnapshot snapshot, List<StoredAlbumSummary> organizedAlbums) {
        if (snapshot == null || snapshot.groups.isEmpty() || resolver == null) {
            return snapshot;
        }
        try {
            Set<String> organizedPaths = organizedRelativePaths(organizedAlbums);
            Set<Long> photoIds = collectIds(snapshot.groups, MediaKind.PHOTO);
            Set<Long> videoIds = collectIds(snapshot.groups, MediaKind.VIDEO);
            Set<Long> livePhotoIds = queryLiveIds(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoIds, organizedPaths);
            Set<Long> liveVideoIds = queryLiveIds(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoIds, organizedPaths);
            return filter(snapshot, livePhotoIds, liveVideoIds);
        } catch (Exception ignored) {
            // Permission and provider failures are unknown state, not proof that media was deleted.
            return snapshot;
        }
    }

    static DiscoverySnapshot filter(DiscoverySnapshot snapshot,
                                    Set<Long> livePhotoIds,
                                    Set<Long> liveVideoIds) {
        if (snapshot == null || snapshot.groups.isEmpty()) {
            return snapshot;
        }
        Set<Long> photos = livePhotoIds == null ? Collections.<Long>emptySet() : livePhotoIds;
        Set<Long> videos = liveVideoIds == null ? Collections.<Long>emptySet() : liveVideoIds;
        ArrayList<DiscoveryMemoryGroup> groups = new ArrayList<>();
        for (DiscoveryMemoryGroup group : snapshot.groups) {
            DiscoveryMemoryGroup filtered = filterGroup(group, photos, videos);
            if (filtered != null) {
                groups.add(filtered);
            }
        }
        return new DiscoverySnapshot(
                snapshot.schemaVersion,
                snapshot.snapshotVersion,
                snapshot.createdAtMillis,
                snapshot.sourceSignature,
                snapshot.sourceItemCount,
                groups,
                snapshot.analysisPolicyVersion,
                snapshot.countryIdentityPolicyVersion);
    }

    private static DiscoveryMemoryGroup filterGroup(DiscoveryMemoryGroup group,
                                                     Set<Long> livePhotoIds,
                                                     Set<Long> liveVideoIds) {
        if (group == null || group.photoRefs.isEmpty()) {
            return null;
        }
        ArrayList<DiscoveryPhotoRef> refs = new ArrayList<>();
        int photoCount = 0;
        int videoCount = 0;
        long start = 0L;
        long end = 0L;
        for (DiscoveryPhotoRef ref : group.photoRefs) {
            if (!isLive(ref, livePhotoIds, liveVideoIds)) {
                continue;
            }
            refs.add(ref);
            if (ref.mediaKind == MediaKind.VIDEO) {
                videoCount++;
            } else {
                photoCount++;
            }
            if (ref.takenAtMillis > 0L) {
                start = start <= 0L ? ref.takenAtMillis : Math.min(start, ref.takenAtMillis);
                end = Math.max(end, ref.takenAtMillis);
            }
        }
        if (refs.isEmpty()) {
            return null;
        }
        return new DiscoveryMemoryGroup(
                group.memoryKey,
                group.placeKey,
                group.placeName,
                group.countryCode,
                group.countryName,
                group.adminArea,
                group.addressLine,
                refs.size(),
                photoCount,
                videoCount,
                start,
                end,
                refs.get(0).sourceUri,
                refs,
                0,
                group.snapshotVersion);
    }

    private static boolean isLive(DiscoveryPhotoRef ref,
                                  Set<Long> livePhotoIds,
                                  Set<Long> liveVideoIds) {
        if (ref == null || ref.stale || ref.sourceUri.isEmpty()) {
            return false;
        }
        if (ref.mediaStoreId == DiscoveryPhotoRef.UNKNOWN_ID) {
            return true;
        }
        return (ref.mediaKind == MediaKind.VIDEO ? liveVideoIds : livePhotoIds).contains(ref.mediaStoreId);
    }

    private static Set<Long> collectIds(List<DiscoveryMemoryGroup> groups, MediaKind kind) {
        HashSet<Long> ids = new HashSet<>();
        for (DiscoveryMemoryGroup group : groups) {
            if (group == null) {
                continue;
            }
            for (DiscoveryPhotoRef ref : group.photoRefs) {
                if (ref != null && ref.mediaKind == kind && ref.mediaStoreId != DiscoveryPhotoRef.UNKNOWN_ID) {
                    ids.add(ref.mediaStoreId);
                }
            }
        }
        return ids;
    }

    private Set<Long> queryLiveIds(Uri collection, Set<Long> requestedIds, Set<String> organizedPaths) {
        if (requestedIds.isEmpty()) {
            return Collections.emptySet();
        }
        ArrayList<Long> ids = new ArrayList<>(requestedIds);
        HashSet<Long> liveIds = new HashSet<>();
        for (int start = 0; start < ids.size(); start += QUERY_CHUNK_SIZE) {
            int end = Math.min(ids.size(), start + QUERY_CHUNK_SIZE);
            StringBuilder selection = new StringBuilder(MediaStore.MediaColumns._ID).append(" IN (");
            String[] args = new String[end - start];
            for (int index = start; index < end; index++) {
                if (index > start) {
                    selection.append(',');
                }
                selection.append('?');
                args[index - start] = String.valueOf(ids.get(index));
            }
            selection.append(')');
            if (Build.VERSION.SDK_INT >= 29) {
                selection.append(" AND ").append(MediaStore.MediaColumns.IS_PENDING).append(" = 0");
            }
            if (Build.VERSION.SDK_INT >= 30) {
                selection.append(" AND ").append(MediaStore.MediaColumns.IS_TRASHED).append(" = 0");
            }
            try (Cursor cursor = resolver.query(
                    collection,
                    new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.RELATIVE_PATH},
                    selection.toString(),
                    args,
                    null)) {
                if (cursor == null) {
                    throw new IllegalStateException("MediaStore query returned null");
                }
                while (cursor.moveToNext()) {
                    String relativePath = cursor.isNull(1) ? "" : cursor.getString(1);
                    if (!isOrganizedLocationPath(relativePath, organizedPaths)) {
                        liveIds.add(cursor.getLong(0));
                    }
                }
            }
        }
        return liveIds;
    }

    static Set<String> organizedRelativePaths(List<StoredAlbumSummary> organizedAlbums) {
        HashSet<String> paths = new HashSet<>();
        if (organizedAlbums == null) {
            return paths;
        }
        for (StoredAlbumSummary summary : organizedAlbums) {
            String path = summary == null ? "" : normalizeRelativePath(summary.relativePath);
            if (!path.isEmpty()) {
                paths.add(path);
            }
        }
        return paths;
    }

    static boolean isOrganizedLocationPath(String relativePath, Set<String> organizedPaths) {
        return organizedPaths != null && organizedPaths.contains(normalizeRelativePath(relativePath));
    }

    private static String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }
        String value = relativePath.trim().replace('\\', '/');
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}

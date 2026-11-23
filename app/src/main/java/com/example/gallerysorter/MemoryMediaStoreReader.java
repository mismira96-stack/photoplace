package com.example.gallerysorter;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only MediaStore adapter for one exact Gallery relative path (with optional batch preload). */
final class MemoryMediaStoreReader implements MemoryMediaResolver.GalleryReader {
    private static final String[] PROJECTION = {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.RELATIVE_PATH
    };

    private static final class RawRow {
        final String uri;
        final long id;
        final MediaKind kind;
        final String mime;
        final String name;
        final long takenAt;
        final String relativePath;

        RawRow(String uri, long id, MediaKind kind, String mime, String name, long takenAt, String relativePath) {
            this.uri = uri;
            this.id = id;
            this.kind = kind;
            this.mime = mime;
            this.name = name;
            this.takenAt = takenAt;
            this.relativePath = relativePath;
        }

        DiscoveryPhotoRef toRef(MemoryRecord memory) {
            return new DiscoveryPhotoRef(
                    uri, id, kind, mime, name, takenAt,
                    memory == null ? "" : memory.placeKey,
                    memory == null ? "" : memory.title,
                    memory == null ? "" : memory.countryCode,
                    memory == null ? "" : memory.countryName,
                    memory == null ? "" : memory.adminArea,
                    memory == null ? "" : memory.addressLine,
                    relativePath, 0L, 0L, false);
        }
    }

    private final ContentResolver resolver;

    MemoryMediaStoreReader(ContentResolver resolver) {
        this.resolver = resolver;
    }

    MemoryMediaResolver.GalleryReader withBatchPreload(Set<String> normalizedPaths) {
        if (resolver == null || normalizedPaths == null || normalizedPaths.size() <= 1) {
            return this;
        }
        try {
            Map<String, List<RawRow>> byPath = new LinkedHashMap<>();
            for (String path : normalizedPaths) {
                String clean = normalizePath(path);
                if (!clean.isEmpty()) {
                    byPath.put(clean, new ArrayList<RawRow>());
                }
            }
            if (byPath.isEmpty()) {
                return this;
            }
            preloadKind(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaKind.PHOTO, byPath);
            preloadKind(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaKind.VIDEO, byPath);
            return (relativePath, memory) -> {
                String clean = normalizePath(relativePath);
                if (clean.isEmpty()) {
                    return Result.unknown();
                }
                List<RawRow> rows = byPath.get(clean);
                if (rows == null) {
                    return read(relativePath, memory);
                }
                if (rows.isEmpty()) {
                    return Result.missing();
                }
                ArrayList<DiscoveryPhotoRef> refs = new ArrayList<>(rows.size());
                for (RawRow row : rows) {
                    refs.add(row.toRef(memory));
                }
                Collections.sort(refs, (left, right) -> Long.compare(sortTime(right), sortTime(left)));
                return Result.found(refs);
            };
        } catch (Throwable ignored) {
            return this;
        }
    }

    private void preloadKind(Uri collection, MediaKind kind, Map<String, List<RawRow>> byPath) {
        ArrayList<String> allCandidates = new ArrayList<>();
        for (String cleanPath : byPath.keySet()) {
            allCandidates.addAll(relativePathCandidates(cleanPath));
        }
        for (int start = 0; start < allCandidates.size(); start += 180) {
            int end = Math.min(allCandidates.size(), start + 180);
            List<String> chunk = allCandidates.subList(start, end);
            StringBuilder selection = new StringBuilder(MediaStore.MediaColumns.RELATIVE_PATH).append(" IN (");
            for (int i = 0; i < chunk.size(); i++) {
                if (i > 0) selection.append(',');
                selection.append('?');
            }
            selection.append(')');
            if (Build.VERSION.SDK_INT >= 30) {
                selection.append(" AND ").append(MediaStore.MediaColumns.IS_PENDING).append("=0")
                        .append(" AND ").append(MediaStore.MediaColumns.IS_TRASHED).append("=0");
            } else if (Build.VERSION.SDK_INT >= 29) {
                selection.append(" AND ").append(MediaStore.MediaColumns.IS_PENDING).append("=0");
            }
            try (Cursor cursor = resolver.query(collection, PROJECTION, selection.toString(),
                    chunk.toArray(new String[0]), MediaStore.MediaColumns.DATE_TAKEN + " DESC")) {
                if (cursor == null) {
                    throw new IllegalStateException("MediaStore query returned null");
                }
                int idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
                int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                int takenIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN);
                int pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH);
                while (cursor.moveToNext()) {
                    String rawPath = pathIndex < 0 || cursor.isNull(pathIndex) ? "" : cursor.getString(pathIndex);
                    String cleanPath = normalizePath(rawPath);
                    List<RawRow> bucket = byPath.get(cleanPath);
                    if (bucket == null) continue;
                    long id = cursor.getLong(idIndex);
                    long takenAt = timestampMillis(cursor, takenIndex, 1L);
                    if (takenAt <= 0L) {
                        int modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);
                        takenAt = timestampMillis(cursor, modifiedIndex, 1000L);
                    }
                    if (takenAt <= 0L) {
                        int addedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);
                        takenAt = timestampMillis(cursor, addedIndex, 1000L);
                    }
                    String mime = mimeIndex < 0 ? "" : cursor.getString(mimeIndex);
                    String name = nameIndex < 0 ? "" : cursor.getString(nameIndex);
                    Uri itemUri = ContentUris.withAppendedId(collection, id);
                    bucket.add(new RawRow(itemUri.toString(), id, kind, mime, name, takenAt, cleanPath));
                }
            }
        }
    }

    @Override
    public Result read(String relativePath, MemoryRecord memory) {
        String path = normalizePath(relativePath);
        if (resolver == null || path.isEmpty()) {
            return Result.unknown();
        }
        ArrayList<DiscoveryPhotoRef> refs = new ArrayList<>();
        try {
            readKind(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaKind.PHOTO,
                    path, memory, refs);
            readKind(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaKind.VIDEO,
                    path, memory, refs);
            Collections.sort(refs, new Comparator<DiscoveryPhotoRef>() {
                @Override
                public int compare(DiscoveryPhotoRef left, DiscoveryPhotoRef right) {
                    return Long.compare(sortTime(right), sortTime(left));
                }
            });
            return refs.isEmpty() ? Result.missing() : Result.found(refs);
        } catch (Throwable ignored) {
            return Result.failed();
        }
    }

    private void readKind(Uri collection,
                          MediaKind kind,
                          String relativePath,
                          MemoryRecord memory,
                          List<DiscoveryPhotoRef> refs) {
        String selection = "(" + MediaStore.MediaColumns.RELATIVE_PATH + "=? OR "
                + MediaStore.MediaColumns.RELATIVE_PATH + "=?)";
        if (Build.VERSION.SDK_INT >= 30) {
            selection += " AND " + MediaStore.MediaColumns.IS_PENDING + "=0"
                    + " AND " + MediaStore.MediaColumns.IS_TRASHED + "=0";
        } else if (Build.VERSION.SDK_INT >= 29) {
            selection += " AND " + MediaStore.MediaColumns.IS_PENDING + "=0";
        }
        List<String> pathArgs = relativePathCandidates(relativePath);
        try (Cursor cursor = resolver.query(collection, PROJECTION, selection,
                pathArgs.toArray(new String[0]), MediaStore.MediaColumns.DATE_TAKEN + " DESC")) {
            if (cursor == null) {
                throw new IllegalStateException("MediaStore query returned null");
            }
            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
            int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            int takenIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                long takenAt = timestampMillis(cursor, takenIndex, 1L);
                if (takenAt <= 0L) {
                    int modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);
                    takenAt = timestampMillis(cursor, modifiedIndex, 1000L);
                }
                if (takenAt <= 0L) {
                    int addedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);
                    takenAt = timestampMillis(cursor, addedIndex, 1000L);
                }
                String mime = mimeIndex < 0 ? "" : cursor.getString(mimeIndex);
                String name = nameIndex < 0 ? "" : cursor.getString(nameIndex);
                Uri itemUri = ContentUris.withAppendedId(collection, id);
                refs.add(new DiscoveryPhotoRef(
                        itemUri.toString(), id, kind, mime, name, takenAt,
                        memory == null ? "" : memory.placeKey,
                        memory == null ? "" : memory.title,
                        memory == null ? "" : memory.countryCode,
                        memory == null ? "" : memory.countryName,
                        memory == null ? "" : memory.adminArea,
                        memory == null ? "" : memory.addressLine,
                        relativePath, 0L, 0L, false));
            }
        }
    }

    static String normalizePath(String value) {
        String path = value == null ? "" : value.trim().replace('\\', '/');
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    static List<String> relativePathCandidates(String value) {
        String path = normalizePath(value);
        if (path.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> candidates = new ArrayList<>();
        candidates.add(path);
        candidates.add(path + "/");
        return Collections.unmodifiableList(candidates);
    }

    private static long timestampMillis(Cursor cursor, int index, long multiplier) {
        if (index < 0 || cursor.isNull(index)) {
            return DiscoveryPhotoRef.UNKNOWN_TIME;
        }
        long value = cursor.getLong(index);
        return value <= 0L ? DiscoveryPhotoRef.UNKNOWN_TIME : value * multiplier;
    }

    private static long sortTime(DiscoveryPhotoRef ref) {
        return ref == null ? DiscoveryPhotoRef.UNKNOWN_TIME : ref.takenAtMillis;
    }

    static final class Result {
        final MemoryMediaSourcePolicy.GalleryLookup lookup;
        final List<DiscoveryPhotoRef> refs;

        private Result(MemoryMediaSourcePolicy.GalleryLookup lookup,
                       List<DiscoveryPhotoRef> refs) {
            this.lookup = lookup;
            this.refs = refs == null || refs.isEmpty()
                    ? Collections.<DiscoveryPhotoRef>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(refs));
        }

        static Result found(List<DiscoveryPhotoRef> refs) {
            return new Result(MemoryMediaSourcePolicy.GalleryLookup.FOUND, refs);
        }

        static Result missing() {
            return new Result(MemoryMediaSourcePolicy.GalleryLookup.MISSING,
                    Collections.<DiscoveryPhotoRef>emptyList());
        }

        static Result failed() {
            return new Result(MemoryMediaSourcePolicy.GalleryLookup.FAILED,
                    Collections.<DiscoveryPhotoRef>emptyList());
        }

        static Result unknown() {
            return new Result(MemoryMediaSourcePolicy.GalleryLookup.UNKNOWN,
                    Collections.<DiscoveryPhotoRef>emptyList());
        }
    }
}

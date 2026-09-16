package com.example.gallerysorter;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only MediaStore adapter for one exact Gallery relative path. */
final class MemoryMediaStoreReader implements MemoryMediaResolver.GalleryReader {
    private static final String[] PROJECTION = {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.RELATIVE_PATH
    };

    private final ContentResolver resolver;

    MemoryMediaStoreReader(ContentResolver resolver) {
        this.resolver = resolver;
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
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
        if (Build.VERSION.SDK_INT >= 30) {
            selection += " AND " + MediaStore.MediaColumns.IS_PENDING + "=0"
                    + " AND " + MediaStore.MediaColumns.IS_TRASHED + "=0";
        } else if (Build.VERSION.SDK_INT >= 29) {
            selection += " AND " + MediaStore.MediaColumns.IS_PENDING + "=0";
        }
        try (Cursor cursor = resolver.query(collection, PROJECTION, selection,
                new String[]{relativePath}, MediaStore.MediaColumns.DATE_TAKEN + " DESC")) {
            if (cursor == null) {
                throw new IllegalStateException("MediaStore query returned null");
            }
            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
            int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            int takenIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                long takenAt = takenIndex < 0 || cursor.isNull(takenIndex)
                        ? DiscoveryPhotoRef.UNKNOWN_TIME : cursor.getLong(takenIndex);
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

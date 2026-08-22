package com.example.gallerysorter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reads existing target albums once per path so discovery organization stays duplicate-safe. */
final class MediaStoreAlbumLookup implements DiscoveryAlbumOrganizer.AlbumLookup {
    private final ContentResolver resolver;
    private final Map<String, Set<String>> namesByPathAndKind = new HashMap<>();
    private final Set<String> existingPaths = new HashSet<>();
    private Map<String, String> existingPathsByPlace;

    MediaStoreAlbumLookup(ContentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public String resolveTargetRelativePath(String placeName, String proposedRelativePath) throws Exception {
        if (existingPathsByPlace == null) {
            existingPathsByPlace = loadExistingPlacePaths();
        }
        String normalizedPlace = PlaceNamePolicy.normalizeForMatch(placeName);
        String existing = existingPathsByPlace.get(normalizedPlace);
        if (existing == null) {
            existing = existingPathsByPlace.get(normalizedPlace + "에서");
        }
        return existing == null ? clean(proposedRelativePath) : existing;
    }

    @Override
    public DiscoveryAlbumOrganizer.Match find(String targetRelativePath,
                                               String displayName,
                                               boolean video) throws Exception {
        String cacheKey = (video ? "video|" : "photo|") + clean(targetRelativePath);
        Set<String> names = namesByPathAndKind.get(cacheKey);
        if (names == null) {
            names = loadNames(targetRelativePath, video);
            namesByPathAndKind.put(cacheKey, names);
        }
        return new DiscoveryAlbumOrganizer.Match(
                existingPaths.contains(clean(targetRelativePath)) || !names.isEmpty(),
                names.contains(fileSignature(displayName)));
    }

    private Set<String> loadNames(String relativePath, boolean video) throws Exception {
        HashSet<String> names = new HashSet<>();
        Uri collection = video
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String selection = "relative_path=?";
        if (Build.VERSION.SDK_INT >= 30) {
            selection += " AND is_pending=0 AND is_trashed=0";
        } else if (Build.VERSION.SDK_INT >= 29) {
            selection += " AND is_pending=0";
        }
        try (Cursor cursor = resolver.query(
                collection,
                new String[]{"_display_name"},
                selection,
                new String[]{clean(relativePath)},
                null)) {
            if (cursor == null) {
                throw new IllegalStateException("기존 위치 앨범을 확인할 수 없습니다.");
            }
            int nameIndex = cursor.getColumnIndexOrThrow("_display_name");
            while (cursor.moveToNext()) {
                names.add(fileSignature(cursor.getString(nameIndex)));
            }
        }
        return names;
    }

    private Map<String, String> loadExistingPlacePaths() throws Exception {
        HashMap<String, String> paths = new HashMap<>();
        loadExistingPlacePaths(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, paths);
        loadExistingPlacePaths(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, paths);
        return paths;
    }

    private void loadExistingPlacePaths(Uri collection, Map<String, String> paths) throws Exception {
        String selection = "relative_path LIKE ?";
        if (Build.VERSION.SDK_INT >= 30) {
            selection += " AND is_pending=0 AND is_trashed=0";
        } else if (Build.VERSION.SDK_INT >= 29) {
            selection += " AND is_pending=0";
        }
        try (Cursor cursor = resolver.query(
                collection,
                new String[]{"relative_path"},
                selection,
                new String[]{"Pictures/%"},
                null)) {
            if (cursor == null) {
                throw new IllegalStateException("기존 위치 앨범 경로를 확인할 수 없습니다.");
            }
            int pathIndex = cursor.getColumnIndexOrThrow("relative_path");
            while (cursor.moveToNext()) {
                String path = clean(cursor.getString(pathIndex));
                String folderName = lastFolderName(path);
                if (!folderName.isEmpty()) {
                    existingPaths.add(path);
                    paths.put(PlaceNamePolicy.normalizeForMatch(folderName), path);
                }
            }
        }
    }

    private static String lastFolderName(String relativePath) {
        String value = clean(relativePath);
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        int slash = value.lastIndexOf('/');
        return slash >= 0 ? value.substring(slash + 1) : value;
    }

    static String fileSignature(String value) {
        String lower = clean(value).toLowerCase(Locale.US);
        int extensionIndex = lower.lastIndexOf('.');
        String stem = extensionIndex > 0 ? lower.substring(0, extensionIndex) : lower;
        String extension = extensionIndex > 0 ? lower.substring(extensionIndex) : "";
        return stem.replaceAll("\\s*\\(\\d+\\)$", "") + extension;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

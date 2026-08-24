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

/** Loads each candidate target album once per analysis instead of querying it for every source file. */
final class MediaStoreDuplicateIndex {
    private final ContentResolver resolver;
    private final Map<String, Set<String>> namesByAlbum = new HashMap<>();

    MediaStoreDuplicateIndex(ContentResolver resolver) {
        this.resolver = resolver;
    }

    boolean contains(String relativePath, String displayName, boolean video) {
        if (relativePath == null || relativePath.trim().isEmpty() || displayName == null || displayName.trim().isEmpty()) {
            return false;
        }
        Set<String> names = namesByAlbum.get(key(relativePath, video));
        if (names == null) {
            names = load(relativePath, video);
            namesByAlbum.put(key(relativePath, video), names);
        }
        return names.contains(exact(displayName)) || names.contains(signature(displayName));
    }

    static boolean containsNames(Set<String> names, String displayName) {
        return names != null && (names.contains(exact(displayName)) || names.contains(signature(displayName)));
    }

    private Set<String> load(String relativePath, boolean video) {
        HashSet<String> names = new HashSet<>();
        Uri uri = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String selection = "relative_path=? AND is_pending=0";
        if (Build.VERSION.SDK_INT >= 30) {
            selection += " AND is_trashed=0";
        }
        try (Cursor cursor = resolver.query(uri, new String[]{"_display_name"}, selection, new String[]{relativePath}, null)) {
            if (cursor == null) {
                return names;
            }
            int nameIndex = cursor.getColumnIndex("_display_name");
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    String name = cursor.getString(nameIndex);
                    names.add(exact(name));
                    names.add(signature(name));
                }
            }
        } catch (Exception ignored) {
        }
        return names;
    }

    private static String key(String relativePath, boolean video) {
        return (video ? "v|" : "i|") + relativePath.trim();
    }

    private static String exact(String name) {
        return "e|" + clean(name);
    }

    private static String signature(String name) {
        String value = clean(name);
        int extensionIndex = value.lastIndexOf('.');
        String stem = extensionIndex > 0 ? value.substring(0, extensionIndex) : value;
        String extension = extensionIndex > 0 ? value.substring(extensionIndex) : "";
        return "s|" + stem.replaceAll("\\s*\\(\\d+\\)$", "") + extension;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}

package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Match the scanner's explicit BINARY path predicate; never merge case-distinct folders. */
final class ManualPlacePaths {
    private ManualPlacePaths() { }
    static String normalize(String path) {
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("Unknown scan path");
        String normalized = path.replace('\\', '/');
        while (normalized.contains("//")) normalized = normalized.replace("//", "/");
        if (normalized.startsWith("/")) throw new IllegalArgumentException("Expected relative path");
        for (String part : normalized.split("/"))
            if (part.equals(".") || part.equals("..")) throw new IllegalArgumentException("Invalid relative path");
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
    static List<String> normalizeAll(List<String> paths) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String path : paths) result.add(normalize(path));
        return new ArrayList<>(result);
    }
}

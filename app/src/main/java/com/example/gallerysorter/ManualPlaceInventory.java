package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure validation shared by scoped SQL updates and in-memory projections. No JSON round trips. */
final class ManualPlaceInventory {
    private ManualPlaceInventory() { }

    static final class Update {
        final long nextRevision;
        final List<UnresolvedMediaRecord> incoming;
        private final Set<String> scope;
        private final Map<String, UnresolvedMediaRecord> incomingByKey;

        Update(long revision, List<String> scannedPaths, List<UnresolvedMediaRecord> rows) {
            if (revision < 0) throw new IllegalArgumentException("Negative manual revision");
            nextRevision = Math.addExact(revision, 1);
            scope = new HashSet<>(ManualPlacePaths.normalizeAll(scannedPaths));
            incomingByKey = new LinkedHashMap<>();
            for (UnresolvedMediaRecord row : rows) {
                if (row == null || !containsPath(row.relativePath)) throw new IllegalArgumentException("Item outside scan scope");
                if (incomingByKey.put(row.assetKey(), row) != null)
                    throw new IllegalStateException("Duplicate incoming inventory identity");
            }
            incoming = Collections.unmodifiableList(new ArrayList<>(incomingByKey.values()));
        }

        boolean containsPath(String path) { return scope.contains(ManualPlacePaths.normalize(path)); }
        boolean containsIncoming(String key) { return incomingByKey.containsKey(key); }

        void verifyOutsideScope(UnresolvedMediaRecord existing) {
            UnresolvedMediaRecord next = incomingByKey.get(existing.assetKey());
            if (!containsPath(existing.relativePath) && next != null && !next.uri.equals(existing.uri))
                throw new IllegalStateException("Conflicting inventory row outside scan scope");
        }
    }

    static ManualPlaceSnapshot reconcile(ManualPlaceSnapshot before, List<String> scannedPaths,
                                          List<UnresolvedMediaRecord> incoming) {
        Update update = new Update(before.revision, scannedPaths, incoming);
        LinkedHashMap<String, UnresolvedMediaRecord> rows = new LinkedHashMap<>();
        for (UnresolvedMediaRecord existing : before.unresolved()) {
            update.verifyOutsideScope(existing);
            if (!update.containsPath(existing.relativePath)) rows.put(existing.assetKey(), existing);
        }
        for (UnresolvedMediaRecord row : update.incoming) rows.put(row.assetKey(), row);
        return before.withInventory(update.nextRevision, new ArrayList<>(rows.values()));
    }
}

package com.example.gallerysorter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/** Whole-selection, read-only projection check. Never rewrites aliases, covers, notes or memberships. */
final class MemoryDependencyGuard {
    enum Status { PRESERVED, UNVERIFIED_IDENTITY, SOURCE_ONLY, CYCLIC_SELECTION,
        DATE_SECTION_CHANGED, COLLECTION_DEPENDENCY, SNAPSHOT_CHANGED }
    static final class Memory {
        final Map<String, Set<String>> dates;
        final Map<String, Set<String>> originalDates;
        Memory(Map<String, Set<String>> dates, Map<String, Set<String>> originalDates) {
            this.dates = freeze(dates); this.originalDates = freeze(originalDates);
        }
    }
    static final class Snapshot {
        final boolean complete;
        final Map<String, Memory> memories;
        final Map<String, Set<String>> sourceMemories, collections;
        final Map<String, String> notes;
        final Set<String> directUriReferences;
        final Set<String> unverifiedSources;
        final Map<String, Set<String>> referenceKinds;
        Snapshot(boolean complete, Map<String, Memory> memories, Map<String, Set<String>> sourceMemories,
                 Map<String, Set<String>> collections, Map<String, String> notes, Set<String> directUriReferences) {
            this(complete, memories, sourceMemories, collections, notes, directUriReferences, Collections.emptySet());
        }
        Snapshot(boolean complete, Map<String, Memory> memories, Map<String, Set<String>> sourceMemories,
                 Map<String, Set<String>> collections, Map<String, String> notes, Set<String> directUriReferences,
                 Set<String> unverifiedSources) {
            this(complete, memories, sourceMemories, collections, notes, directUriReferences,
                    unverifiedSources, Collections.emptyMap());
        }
        Snapshot(boolean complete, Map<String, Memory> memories, Map<String, Set<String>> sourceMemories,
                 Map<String, Set<String>> collections, Map<String, String> notes, Set<String> directUriReferences,
                 Set<String> unverifiedSources, Map<String, Set<String>> referenceKinds) {
            this.complete = complete;
            this.memories = Collections.unmodifiableMap(new HashMap<>(memories));
            this.sourceMemories = freeze(sourceMemories); this.collections = freeze(collections);
            this.notes = Collections.unmodifiableMap(new HashMap<>(notes));
            this.directUriReferences = Collections.unmodifiableSet(new HashSet<>(directUriReferences));
            this.unverifiedSources = Collections.unmodifiableSet(new HashSet<>(unverifiedSources));
            this.referenceKinds = freeze(referenceKinds);
        }
        String referenceDescription(String source) {
            Set<String> kinds = referenceKinds.get(source);
            return kinds == null ? "" : String.join(" · ", new java.util.TreeSet<>(kinds));
        }
    }

    static final class Report {
        final Status status;
        final Map<String, Status> blockedSources;
        Report(Status status, Map<String, Status> blockedSources) {
            this.status = status;
            this.blockedSources = Collections.unmodifiableMap(new LinkedHashMap<>(blockedSources));
        }
    }

    static Status check(List<CleanupEvidenceRecord> selected, Snapshot before, Snapshot after) {
        return inspect(selected, before, after).status;
    }

    static Report inspect(List<CleanupEvidenceRecord> selected, Snapshot before, Snapshot after) {
        Map<String, Status> blocked = new LinkedHashMap<>();
        if (selected == null || selected.isEmpty() || before == null || after == null)
            return new Report(Status.UNVERIFIED_IDENTITY, blocked);
        Set<String> removed = new LinkedHashSet<>();
        Map<String, Set<String>> targets = new HashMap<>();
        for (CleanupEvidenceRecord pair : selected) {
            if (pair == null) return new Report(Status.UNVERIFIED_IDENTITY, blocked);
            removed.add(pair.sourceUri);
            targets.computeIfAbsent(pair.sourceUri, key -> new HashSet<>()).add(pair.targetUri);
        }
        Map<String, MemoryCheck> memoryChecks = new HashMap<>();
        Set<String> memoryIds = new HashSet<>(before.memories.keySet());
        memoryIds.addAll(after.memories.keySet());
        for (String id : memoryIds) memoryChecks.put(id, inspectMemory(before.memories.get(id), after.memories.get(id), removed));
        for (String source : removed) {
            Set<String> protectedTargets = new HashSet<>(targets.get(source));
            protectedTargets.removeAll(removed);
            if (protectedTargets.isEmpty()) blocked.put(source, Status.CYCLIC_SELECTION);
            targets.put(source, protectedTargets);
        }
        for (String source : removed) {
            if (blocked.containsKey(source)) continue;
            if (before.directUriReferences.contains(source) || after.directUriReferences.contains(source)) {
                blocked.put(source, Status.COLLECTION_DEPENDENCY); continue;
            }
            if (before.unverifiedSources.contains(source) || after.unverifiedSources.contains(source)) {
                blocked.put(source, Status.UNVERIFIED_IDENTITY); continue;
            }
            Status local = inspectSource(source, targets.get(source), before, after, memoryChecks);
            if (local != Status.PRESERVED) blocked.put(source, local);
        }
        Status status;
        if (!before.complete || !after.complete) status = Status.UNVERIFIED_IDENTITY;
        else if (blocked.containsValue(Status.CYCLIC_SELECTION)) status = Status.CYCLIC_SELECTION;
        else if (!before.collections.equals(after.collections) || !before.notes.equals(after.notes)) status = Status.COLLECTION_DEPENDENCY;
        else status = blocked.isEmpty() ? Status.PRESERVED : blocked.values().iterator().next();
        return new Report(status, blocked);
    }

    private static Status inspectSource(String source, Set<String> targets,
                                        Snapshot before, Snapshot after, Map<String, MemoryCheck> memoryChecks) {
            Set<String> ids = before.sourceMemories.get(source);
            if (ids == null || ids.isEmpty()) {
                Set<String> afterIds = after.sourceMemories.get(source);
                if (afterIds != null && !afterIds.isEmpty()) return Status.UNVERIFIED_IDENTITY;
                return Status.PRESERVED;
            }
            if (!ids.equals(after.sourceMemories.get(source)))
                return Status.UNVERIFIED_IDENTITY;
            for (String id : ids) {
                if (!id.startsWith("mem_")) return Status.UNVERIFIED_IDENTITY;
                Memory a = before.memories.get(id), b = after.memories.get(id);
                MemoryCheck common = memoryChecks.get(id);
                if (common == null) return Status.SOURCE_ONLY;
                if (common.status != Status.PRESERVED) return common.status;
                Set<String> originalDates = a.originalDates.get(source);
                if (originalDates == null || originalDates.isEmpty()) return Status.UNVERIFIED_IDENTITY;
                if (common.containsRemoved) return Status.SOURCE_ONLY;
                for (String date : originalDates) {
                    Set<String> refs = b.dates.get(date);
                    if (refs == null || !overlaps(refs, targets))
                        return Status.DATE_SECTION_CHANGED;
                }
            }
        return Status.PRESERVED;
    }

    private static final class MemoryCheck {
        final Status status;
        final boolean containsRemoved;
        MemoryCheck(Status status, boolean containsRemoved) { this.status = status; this.containsRemoved = containsRemoved; }
    }
    private static MemoryCheck inspectMemory(Memory before, Memory after, Set<String> removed) {
        if (before == null || after == null || after.dates.isEmpty()) return new MemoryCheck(Status.SOURCE_ONLY, false);
        if (!before.dates.keySet().equals(after.dates.keySet())) return new MemoryCheck(Status.DATE_SECTION_CHANGED, false);
        for (Set<String> refs : after.dates.values()) if (overlaps(refs, removed))
            return new MemoryCheck(Status.PRESERVED, true);
        return new MemoryCheck(Status.PRESERVED, false);
    }
    static boolean overlaps(Set<String> a, Set<String> b) {
        // Android Collections.disjoint may iterate the ENTIRE selection for every tiny date set.
        Set<String> smaller = a.size() <= b.size() ? a : b;
        Set<String> larger = smaller == a ? b : a;
        for (String value : smaller) if (larger.contains(value)) return true;
        return false;
    }

    private static Map<String, Set<String>> freeze(Map<String, Set<String>> map) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : map.entrySet())
            result.put(entry.getKey(), Collections.unmodifiableSet(new HashSet<>(entry.getValue())));
        return Collections.unmodifiableMap(result);
    }
}

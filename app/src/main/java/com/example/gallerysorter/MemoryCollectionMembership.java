package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Collection membership projection; source Memory records remain untouched for search. */
final class MemoryCollectionMembership {
    private MemoryCollectionMembership() {
    }

    static Set<String> stableIds(List<MemoryCollection> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<String> result = new HashSet<>();
        for (MemoryCollection collection : collections) {
            if (collection == null) {
                continue;
            }
            for (MemoryCollection.Member member : collection.members) {
                if (member != null && !member.stableMemoryId.isEmpty()) {
                    result.add(member.stableMemoryId);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    static List<MemoryRecord> ungroupedRecords(List<MemoryRecord> records,
                                               Set<String> memberIds,
                                               Map<String, String> aliases) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        if (memberIds == null || memberIds.isEmpty() || aliases == null || aliases.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<>(records));
        }
        ArrayList<MemoryRecord> visible = new ArrayList<>();
        for (MemoryRecord record : records) {
            String stableId = record == null ? "" : clean(aliases.get(record.memoryKey));
            if (record == null || stableId.isEmpty() || !memberIds.contains(stableId)) {
                visible.add(record);
            }
        }
        return Collections.unmodifiableList(visible);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

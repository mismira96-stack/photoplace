package com.example.gallerysorter;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable parsed identities plus immutable inventory; never serializes inventory into JSON. */
final class ManualPlaceSnapshot {
    final long revision;
    private final String identitiesPayload;
    private final Map<String, String> aliases;
    private final boolean assignmentsPresent;
    private final List<UnresolvedMediaRecord> unresolved;
    private final List<ManualMediaAssignment> assignments;

    ManualPlaceSnapshot(long revision, String payload) throws Exception {
        if (revision < 0) throw new IllegalArgumentException("Negative manual revision");
        if (payload == null || payload.length() > 32 * 1024 * 1024)
            throw new IllegalArgumentException("Manual identities unavailable or oversized");
        JSONObject root = new JSONObject(payload);
        if (root.getInt("schemaVersion") != 1) throw new IllegalStateException("Unknown manual schema");
        assignmentsPresent = root.getJSONObject("assignments").length() != 0;
        if (root.getJSONObject("unresolved").length() != 0)
            throw new IllegalStateException("Inventory must be stored separately from identities");
        aliases = parseAliases(root.getJSONObject("places"));
        this.revision = revision;
        identitiesPayload = root.toString();
        unresolved = Collections.emptyList();
        assignments = Collections.emptyList();
    }

    private ManualPlaceSnapshot(ManualPlaceSnapshot identities, long revision,
                                List<UnresolvedMediaRecord> rows, List<ManualMediaAssignment> assigned, int assignmentCount) {
        if (revision < 0) throw new IllegalArgumentException("Negative manual revision");
        this.revision = revision;
        identitiesPayload = identities.identitiesPayload;
        aliases = identities.aliases;
        assignmentsPresent = identities.assignmentsPresent || assignmentCount > 0;
        assignments = Collections.unmodifiableList(new ArrayList<>(assigned));
        Set<String> keys = new HashSet<>();
        ArrayList<UnresolvedMediaRecord> copy = new ArrayList<>(rows.size());
        for (UnresolvedMediaRecord row : rows) {
            if (row == null || !keys.add(row.assetKey())) throw new IllegalStateException("Duplicate inventory identity");
            copy.add(row);
        }
        unresolved = Collections.unmodifiableList(copy);
    }

    static ManualPlaceSnapshot empty() {
        try {
            return new ManualPlaceSnapshot(0,
                    "{\"schemaVersion\":1,\"places\":{},\"assignments\":{},\"unresolved\":{}}");
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }

    JSONObject writableCopy() throws Exception { return new JSONObject(identitiesPayload); }
    String identitiesPayload() { return identitiesPayload; }
    List<UnresolvedMediaRecord> unresolved() { return unresolved; }
    boolean hasAssignments() { return assignmentsPresent; }
    List<ManualMediaAssignment> assignments() { return assignments; }

    ManualPlaceSnapshot withInventory(long revision, List<UnresolvedMediaRecord> rows) {
        return new ManualPlaceSnapshot(this, revision, rows, assignments, assignments.size());
    }

    ManualPlaceSnapshot withAssignments(List<ManualMediaAssignment> assigned, int totalCount) {
        return new ManualPlaceSnapshot(this, revision, unresolved, assigned, totalCount);
    }

    Map<String, String> mergedAliases(Map<String, String> legacy) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>(legacy);
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            String old = result.get(alias.getKey());
            if (old != null && !old.equals(alias.getValue())) throw new IllegalStateException("Manual alias conflict");
            result.put(alias.getKey(), alias.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> parseAliases(JSONObject places) throws Exception {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Iterator<String> keys = places.keys(); keys.hasNext();) {
            String placeId = keys.next();
            JSONObject place = places.getJSONObject(placeId);
            String alias = place.getString("memoryKey");
            String id = place.getString("stableMemoryId");
            if (!alias.startsWith("manual:") || alias.length() <= 7
                    || !id.startsWith("mem_") || id.length() <= 4 || !alias.equals("manual:" + placeId))
                throw new IllegalStateException("Invalid manual identity");
            result.put(alias, id);
            String output=place.optString("outputPath","");
            if(!output.isEmpty()) {
                String outputAlias="path:"+ManualPlacePaths.normalize(output);
                String previous=result.put(outputAlias,id);
                if(previous!=null&&!previous.equals(id))throw new IllegalStateException("Conflicting manual output identity");
            }
        }
        return Collections.unmodifiableMap(result);
    }
}

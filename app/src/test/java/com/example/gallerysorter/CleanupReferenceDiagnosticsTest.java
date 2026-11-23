package com.example.gallerysorter;

import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupReferenceDiagnosticsTest {
    @Test public void referenceKindsAreAttachedOnlyToTheExactSelectedUri() throws Exception {
        String selected = "content://media/external/images/media/1";
        String other = "content://media/external/images/media/2";
        Set<String> direct = new HashSet<>(); Map<String, Set<String>> kinds = new HashMap<>();
        JSONObject history = new JSONObject().put("albums", new JSONArray()
                .put(new JSONObject().put("thumbnailUri", selected))
                .put(new JSONObject().put("thumbnailUri", other)));
        CleanupMemorySnapshotReader.collectDirectUris(history, Collections.singleton(selected), direct, kinds, "앨범 정리 이력");
        CleanupMemorySnapshotReader.collectDirectUris(new JSONObject().put("userCoverUri", selected),
                Collections.singleton(selected), direct, kinds, "사용자 대표사진");
        assertEquals(Collections.singleton(selected), direct);
        assertEquals(new HashSet<>(Arrays.asList("앨범 정리 이력", "사용자 대표사진")), kinds.get(selected));
        assertFalse(kinds.containsKey(other));
    }

    @Test public void globalCycleFailureDoesNotInventReasonsForUnrelatedSources() {
        String prefix = "content://media/external/images/media/";
        List<CleanupEvidenceRecord> pairs = new ArrayList<>();
        for (int[] ids : new int[][]{{1, 2}, {2, 1}, {3, 4}})
            pairs.add(new CleanupEvidenceRecord("job", prefix + ids[0], prefix + ids[1], "Pictures/Test/", 1, 3, 1));
        MemoryDependencyGuard.Snapshot snapshot = new MemoryDependencyGuard.Snapshot(true, Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
        MemoryDependencyGuard.Report report = MemoryDependencyGuard.inspect(pairs, snapshot, snapshot);
        assertEquals(MemoryDependencyGuard.Status.CYCLIC_SELECTION, report.status);
        assertEquals(2, report.blockedSources.size());
        assertFalse(report.blockedSources.containsKey(prefix + 3));
    }

    @Test public void unknownScopeRemainsUnknownEvenWhenNoDirectReferencesAreFound() {
        String prefix = "content://media/external/images/media/";
        CleanupEvidenceRecord pair = new CleanupEvidenceRecord("job", prefix + 1, prefix + 2, "Pictures/Test/", 1, 3, 1);
        MemoryDependencyGuard.Snapshot snapshot = new MemoryDependencyGuard.Snapshot(false, Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
        MemoryDependencyGuard.Report report = MemoryDependencyGuard.inspect(Collections.singletonList(pair), snapshot, snapshot);
        assertEquals(MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY, report.status);
        assertTrue(report.blockedSources.isEmpty());
    }
}

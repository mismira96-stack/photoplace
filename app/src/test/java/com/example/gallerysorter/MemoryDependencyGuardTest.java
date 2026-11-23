package com.example.gallerysorter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryDependencyGuardTest {
    private static final String ID = "mem_test", DATE = "20261024";
    private String uri(int id) { return "content://media/external/images/media/" + id; }
    private Set<String> set(String... values) { return new HashSet<>(Arrays.asList(values)); }
    private CleanupEvidenceRecord pair(int a, int b) {
        return new CleanupEvidenceRecord("job", uri(a), uri(b), "Pictures/Test/", 1, 5, 1);
    }
    private MemoryDependencyGuard.Snapshot snapshot(Set<String> visible, boolean complete,
            Map<String, Set<String>> sourceIds, Set<String> direct, String note) {
        Map<String, Set<String>> dates = Collections.singletonMap(DATE, visible);
        Map<String, Set<String>> originals = new HashMap<>();
        for (String source : sourceIds.keySet()) originals.put(source, set(DATE));
        return new MemoryDependencyGuard.Snapshot(complete,
                Collections.singletonMap(ID, new MemoryDependencyGuard.Memory(dates, originals)), sourceIds,
                Collections.singletonMap("group_trip", set(ID)),
                Collections.singletonMap("memory-id:" + ID + "#" + DATE, note), direct);
    }
    private MemoryDependencyGuard.Snapshot state(Set<String> visible) {
        return snapshot(visible, true, Collections.singletonMap(uri(1), set(ID)), Collections.emptySet(), "memo");
    }
    @Test public void protectedCopyKeepsStableMemoryDateNoteAndCollection() {
        assertEquals(MemoryDependencyGuard.Status.PRESERVED,
                MemoryDependencyGuard.check(Collections.singletonList(pair(1, 11)), state(set(uri(11))), state(set(uri(11)))));
    }
    @Test public void checkingPairsIndividuallyCannotAuthorizeACyclicWholeSelection() {
        assertEquals(MemoryDependencyGuard.Status.CYCLIC_SELECTION,
                MemoryDependencyGuard.check(Arrays.asList(pair(1, 2), pair(2, 1)), state(set(uri(1), uri(2))), state(set())));
    }
    @Test public void independentProtectedOutputsAllowASelectionWithAnExtraInternalTarget() {
        Map<String, Set<String>> ids = new HashMap<>(); ids.put(uri(1), set(ID)); ids.put(uri(2), set(ID));
        MemoryDependencyGuard.Snapshot a = snapshot(set(uri(11), uri(12)), true, ids, set(), "memo");
        assertEquals(MemoryDependencyGuard.Status.PRESERVED,
                MemoryDependencyGuard.check(Arrays.asList(pair(1, 2), pair(1, 11), pair(2, 12)), a, a));
    }
    @Test public void survivingSourceIsNotAReplacementProjection() {
        assertEquals(MemoryDependencyGuard.Status.SOURCE_ONLY,
                MemoryDependencyGuard.check(Collections.singletonList(pair(1, 11)), state(set(uri(1), uri(11))),
                        state(set(uri(1), uri(11)))));
    }
    @Test public void missingTargetInSameDateCannotPass() {
        assertEquals(MemoryDependencyGuard.Status.DATE_SECTION_CHANGED,
                MemoryDependencyGuard.check(Collections.singletonList(pair(1, 11)), state(set(uri(11))), state(set(uri(12)))));
    }
    @Test public void changedNoteAndDirectCoverReferencesBlockTheWholeSelection() {
        Map<String, Set<String>> ids = Collections.singletonMap(uri(1), set(ID));
        for (MemoryDependencyGuard.Snapshot b : Arrays.asList(
                snapshot(set(uri(11)), true, ids, set(uri(1)), "memo"),
                snapshot(set(uri(11)), true, ids, set(), "changed memo"))) {
            assertEquals(MemoryDependencyGuard.Status.COLLECTION_DEPENDENCY,
                    MemoryDependencyGuard.check(Collections.singletonList(pair(1, 11)), state(set(uri(11))), b));
        }
    }
    @Test public void incompleteReferenceScopeIsNeverAPass() {
        Map<String, Set<String>> ids = Collections.singletonMap(uri(1), set(ID));
        assertEquals(MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY,
                MemoryDependencyGuard.check(Collections.singletonList(pair(1, 11)), state(set(uri(11))),
                        snapshot(set(uri(11)), false, ids, set(), "memo")));
    }
    @Test public void lostOrChangedStableIdentityIsUnknown() {
        assertEquals(MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY,
                MemoryDependencyGuard.check(Collections.singletonList(pair(1, 11)), state(set(uri(11))),
                        snapshot(set(uri(11)), true, Collections.emptyMap(), set(), "memo")));
    }
    @Test public void everyKnownMemoryDependencyMustPass() {
        Map<String, Set<String>> ids = Collections.singletonMap(uri(1), set(ID, "mem_other"));
        MemoryDependencyGuard.Snapshot incomplete = snapshot(set(uri(11)), true, ids, set(), "memo");
        assertEquals(MemoryDependencyGuard.Status.SOURCE_ONLY,
                MemoryDependencyGuard.check(Collections.singletonList(pair(1, 11)), incomplete, incomplete));
    }
    @Test public void sourceOutsideDiscoveryMemoriesPassesWithoutMemoryDependencies() {
        // Photos in Download/ or Camera that have no discovery memory references (sourceMemories is empty)
        MemoryDependencyGuard.Snapshot emptySourceMemories = snapshot(set(uri(11)), true, Collections.emptyMap(), set(), "memo");
        assertEquals(MemoryDependencyGuard.Status.PRESERVED,
                MemoryDependencyGuard.check(Collections.singletonList(pair(1, 11)), emptySourceMemories, emptySourceMemories));
    }
}

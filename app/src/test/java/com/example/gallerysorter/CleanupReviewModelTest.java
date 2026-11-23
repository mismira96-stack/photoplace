package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupReviewModelTest {
    private CleanupCandidate candidate(int id, boolean eligible) {
        CleanupEvidenceRecord r=new CleanupEvidenceRecord("ui","content://media/external/images/media/"+id,
                "content://media/external/images/media/"+(100000+id),"Pictures/성남에서/",1,100,1);
        return new CleanupCandidate(r,CleanupPairVerifier.Status.MATCH,"a",
                eligible?MemoryDependencyGuard.Status.PRESERVED:MemoryDependencyGuard.Status.COLLECTION_DEPENDENCY);
    }
    private CleanupReviewModel.Source source(String path,String volume) {return new CleanupReviewModel.Source(path,"IMG.jpg",volume);}
    @Test public void threeThousandPhotosAreOneFolderGroupAndStartUnselected() {
        List<CleanupCandidate> rows=new ArrayList<>();Map<String,CleanupReviewModel.Source> sources=new HashMap<>();
        for(int n=1;n<=3000;n++){CleanupCandidate c=candidate(n,true);rows.add(c);sources.put(c.evidence.sourceUri,source("DCIM/Camera/","external_primary"));}
        CleanupReviewModel m=new CleanupReviewModel(new CleanupReviewAssessment("ui",rows),sources);
        assertEquals(1,m.groups(false).size());assertEquals(3000,m.availableCount());
        Set<String> selected=new HashSet<>();assertEquals(0,m.chosen(selected).size());
        m.selectAll(selected,true);assertEquals(3000,m.chosen(selected).size());
        assertFalse(selected.contains(rows.get(0).evidence.targetUri));
    }
    @Test public void heldRowsAreSeparateAndNeverIncludedInBulkSelection() {
        CleanupCandidate yes=candidate(1,true),no=candidate(2,false);Map<String,CleanupReviewModel.Source> sources=new HashMap<>();
        sources.put(yes.evidence.sourceUri,source("Download/","external_primary"));sources.put(no.evidence.sourceUri,source("Download/","external_primary"));
        CleanupReviewModel m=new CleanupReviewModel(new CleanupReviewAssessment("ui",Arrays.asList(yes,no)),sources);
        Set<String> selected=new HashSet<>();m.selectAll(selected,true);m.toggle(m.groups(true).get(0),selected);m.toggle(no,selected);
        assertEquals(1,m.chosen(selected).size());assertFalse(selected.contains(no.evidence.sourceUri));assertEquals(1,m.heldCount());
    }
    @Test public void groupToggleCompletesPartialSelectionThenClearsOnlyThatGroup() {
        CleanupCandidate a=candidate(1,true),b=candidate(2,true),c=candidate(3,true);
        Map<String,CleanupReviewModel.Source> sources=new HashMap<>();
        sources.put(a.evidence.sourceUri,source("DCIM/Camera/","external_primary"));sources.put(b.evidence.sourceUri,source("DCIM/Camera/","external_primary"));sources.put(c.evidence.sourceUri,source("Download/","external_primary"));
        CleanupReviewModel m=new CleanupReviewModel(new CleanupReviewAssessment("ui",Arrays.asList(a,b,c)),sources);
        Set<String> selected=new HashSet<>();m.toggle(a,selected);m.toggle(c,selected);
        m.toggle(m.groups(false).get(0),selected);assertEquals(3,m.chosen(selected).size());
        m.toggle(m.groups(false).get(0),selected);assertEquals(Collections.singleton(c.evidence.sourceUri),selected);
    }
    @Test public void unknownSourceLocationIsHeldInsteadOfDisplayingTheTargetAsTheOriginal() {
        CleanupCandidate c=candidate(1,true);
        CleanupReviewModel m=new CleanupReviewModel(new CleanupReviewAssessment("ui",Collections.singletonList(c)),Collections.emptyMap());
        Set<String> selected=new HashSet<>();m.selectAll(selected,true);m.toggle(c,selected);
        assertEquals(0,m.availableCount());assertTrue(selected.isEmpty());assertTrue(m.source(c).location().contains("읽지 못"));
        assertEquals("Pictures/성남에서",m.groups(true).get(0).keptPath);
    }
    @Test public void identicallyNamedFoldersOnDifferentVolumesStayDistinct() {
        CleanupCandidate a=candidate(1,true),b=candidate(2,true);Map<String,CleanupReviewModel.Source> sources=new HashMap<>();
        sources.put(a.evidence.sourceUri,source("DCIM/Camera/","external_primary"));sources.put(b.evidence.sourceUri,source("DCIM/Camera/","ABCD-1234"));
        CleanupReviewModel m=new CleanupReviewModel(new CleanupReviewAssessment("ui",Arrays.asList(a,b)),sources);
        assertEquals(2,m.groups(false).size());assertTrue(m.source(a).location().contains("내장"));assertTrue(m.source(b).location().contains("외장"));
    }
}

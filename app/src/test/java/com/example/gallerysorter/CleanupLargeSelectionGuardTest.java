package com.example.gallerysorter;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupLargeSelectionGuardTest {
    static final class CountingSet extends AbstractSet<String> {
        final Set<String> values = new HashSet<>();
        int visited;
        CountingSet(int count, String prefix) { for(int n=0;n<count;n++)values.add(prefix+n); }
        @Override public int size() {return values.size();}
        @Override public boolean contains(Object value) {return values.contains(value);}
        @Override public Iterator<String> iterator() {
            Iterator<String> it=values.iterator();
            return new Iterator<String>() {
                public boolean hasNext(){return it.hasNext();}
                public String next(){visited++;return it.next();}
            };
        }
    }
    @Test public void tinyDateSectionDoesNotWalkTheEntirePhotoSelectionInEitherArgumentOrder() {
        CountingSet tiny=new CountingSet(2,"date"), selected=new CountingSet(10000,"source");
        assertFalse(MemoryDependencyGuard.overlaps(tiny,selected));
        assertFalse(MemoryDependencyGuard.overlaps(selected,tiny));
        assertTrue("Unrelated selected photos must not be scanned for each date",selected.visited<100);
        assertTrue(tiny.visited<100);
    }
    @Test(timeout=5000) public void threeThousandDateSectionsAndOriginalsPreserveTheWholeSelection() {
        List<CleanupEvidenceRecord> records=new ArrayList<>();
        Map<String,Set<String>> beforeDates=new HashMap<>(),afterDates=new HashMap<>(),originalDates=new HashMap<>(),members=new HashMap<>();
        for(int n=1;n<=3000;n++) {
            String source="content://media/external/images/media/"+n,target="content://media/external/images/media/"+(10000+n),date="date"+n;
            records.add(new CleanupEvidenceRecord("large",source,target,"Pictures/Test/",1,4,1));
            Set<String> before=new HashSet<>();before.add(source);before.add(target);
            beforeDates.put(date,before);afterDates.put(date,Collections.singleton(target));
            originalDates.put(source,Collections.singleton(date));members.put(source,Collections.singleton("mem_large"));
        }
        MemoryDependencyGuard.Memory before=new MemoryDependencyGuard.Memory(beforeDates,originalDates);
        MemoryDependencyGuard.Memory after=new MemoryDependencyGuard.Memory(afterDates,originalDates);
        MemoryDependencyGuard.Snapshot a=new MemoryDependencyGuard.Snapshot(true,Collections.singletonMap("mem_large",before),members,
                Collections.emptyMap(),Collections.emptyMap(),Collections.emptySet());
        MemoryDependencyGuard.Snapshot b=new MemoryDependencyGuard.Snapshot(true,Collections.singletonMap("mem_large",after),members,
                Collections.emptyMap(),Collections.emptyMap(),Collections.emptySet());
        assertEquals(MemoryDependencyGuard.Status.PRESERVED,MemoryDependencyGuard.check(records,a,b));
        // A single remaining source reference still rejects the entire affected Memory.
        afterDates.put("date1",Collections.singleton(records.get(0).sourceUri));
        b=new MemoryDependencyGuard.Snapshot(true,Collections.singletonMap("mem_large",new MemoryDependencyGuard.Memory(afterDates,originalDates)),members,
                Collections.emptyMap(),Collections.emptyMap(),Collections.emptySet());
        assertEquals(MemoryDependencyGuard.Status.SOURCE_ONLY,MemoryDependencyGuard.check(records,a,b));
    }
}

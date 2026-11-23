package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class MemoryManagementUiPolicyTest {
    private MemoryPlaceChoice choice(String key,String name,String id,long date) {
        DiscoveryMemoryGroup g=new DiscoveryMemoryGroup(key,key,name,"","한국","경기","주소",3,3,0,date,date,"",Collections.emptyList(),0,1);
        return new MemoryPlaceChoice(MemoryRepository.fromDiscoveryGroup(g),id);
    }
    @Test public void singlePhotoRouteNeverShowsUnlocatedPlacementEvenBeforeLoading() {
        MemoryManagementRoute r=MemoryManagementRoute.of(MemoryManagementRoute.SINGLE,"p","content://media/external/images/media/1","성남");
        assertFalse(r.addsPlace());assertEquals("사진 정리",r.title);assertEquals("p",r.key);
    }
    @Test public void recordRouteKeepsItsOwnTitleAndNeverOffersPlacement() {
        MemoryManagementRoute r=MemoryManagementRoute.of(MemoryManagementRoute.RECORD,"p","","성남");
        assertEquals("성남",r.title);assertFalse(r.addsPlace());
    }
    @Test public void lostTrashSourceNeverSilentlyBecomesAddUnlocatedScreen() {
        MemoryManagementRoute r=MemoryManagementRoute.of(MemoryManagementRoute.SINGLE,"","","");
        assertFalse(r.addsPlace());assertEquals("사진 정리",r.title);
    }
    @Test public void unlocatedEntryRemainsAnExplicitIndependentAddFlow() {
        assertTrue(MemoryManagementRoute.of(MemoryManagementRoute.ADD,"","","").addsPlace());
    }
    @Test public void viewerOriginDoesNotReuseUnrelatedMemoryWhenOpenedFromAnotherContext() {
        assertEquals("p",MemoryManagementRoute.viewerKey(2,2,"p"));
        assertEquals("",MemoryManagementRoute.viewerKey(3,2,"p"));
    }
    @Test public void pickerSearchAndSortingPreserveOriginalRecordIdentity() {
        MemoryPlaceChoice a=choice("a","Fukuoka","mem_a",10),b=choice("b","성남","mem_b",20);
        List<MemoryPlaceChoice> list=Arrays.asList(a,b);
        assertSame(a,MemoryPlaceChoice.filter(list,"FUKU",false).get(0));
        assertSame(b,MemoryPlaceChoice.filter(list,"경기",false).get(0));
        assertSame(a,MemoryPlaceChoice.filter(list,"",true).get(0));
        assertSame(b,MemoryPlaceChoice.filter(list,"",false).get(0));
    }
    @Test public void pickerRecentChoicesIgnoreDeletedIdentitiesAndDuplicates() {
        MemoryPlaceChoice a=choice("a","A","mem_a",10),b=choice("b","B","mem_b",20);
        List<MemoryPlaceChoice> recent=MemoryPlaceChoice.recent(Arrays.asList(a,b),Arrays.asList("mem_deleted","mem_a","mem_a","mem_b"));
        assertEquals(Arrays.asList(a,b),recent);
    }
    @Test public void userPlaceNamesAreNotTrimmedLikeGeneratedFolderSuffixes() {
        MemoryPlaceChoice c=choice("a","내 마음에서","mem_a",10);
        assertEquals("내 마음에서",c.title);assertEquals(10,c.record.endDateMillis);
    }
}

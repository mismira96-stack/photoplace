package com.example.gallerysorter;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class DiscoverySortStartFeedbackTest {
    @Test public void successFeedbackIsScopedToDiscoveryAndItsActualSubject() {
        List<String> events=new ArrayList<>();
        DiscoverySortStartFeedback.present(false,null,()->events.add("result"),events::add);
        assertTrue(events.isEmpty());
        DiscoverySortStartFeedback.present(true,null,()->events.add("result"),events::add);
        assertEquals(Arrays.asList("result"),events);events.clear();
        OrganizationRequest request=new OrganizationRequest("r",OrganizationLink.SubjectType.MEMORY,"mem_x","a","Pictures/a/");
        DiscoverySortStartFeedback.present(true,request,()->events.add("result"),events::add);
        assertEquals(Arrays.asList("이 장소의 앨범을 만들고 있어요."),events);
    }
}

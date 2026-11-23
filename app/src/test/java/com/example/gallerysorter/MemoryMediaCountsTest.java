package com.example.gallerysorter;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryMediaCountsTest {
    private DiscoveryPhotoRef ref(String uri, MediaKind kind, boolean stale, long taken) {
        return new DiscoveryPhotoRef(uri, 1L, kind,
                kind == MediaKind.VIDEO ? "video/mp4" : "image/jpeg", "file", taken,
                "성남", "성남", "KR", "대한민국", "경기", "", "Pictures/Camera/", 1L, 1L, stale);
    }

    @Test public void videosAreSeparateAndStaleMissingAndDuplicateUrisAreExcluded() {
        java.util.List<DiscoveryPhotoRef> refs = Arrays.asList(
                ref("content://p/1", MediaKind.PHOTO, false, 100L),
                ref("content://p/1", MediaKind.PHOTO, false, 86400100L),
                ref("content://v/1", MediaKind.VIDEO, false, 100L),
                ref("content://p/stale", MediaKind.PHOTO, true, 100L),
                ref("", MediaKind.PHOTO, false, 100L), null);
        MemoryMediaCounts counts = MemoryMediaCounts.fromRefs(refs);
        assertEquals(1, counts.photos);
        assertEquals(1, counts.videos);
        assertEquals("사진 1장 · 동영상 1개", counts.text());
        java.util.List<MemoryPhotoSection> sections = MemoryPhotoSection.fromDiscoveryRefs(refs);
        assertEquals(counts.text(), MemoryMediaCounts.fromSections(sections).text());
        assertEquals(2, MemoryPhotoPage.totalItemCount(sections));
    }

    @Test public void videoOnlyNeverClaimsToContainPhotos() {
        assertEquals("동영상 1개", MemoryMediaCounts.fromRefs(Collections.singletonList(
                ref("content://v/1", MediaKind.VIDEO, false, 100L))).text());
        assertEquals("사진 0장", MemoryMediaCounts.fromRefs(null).text());
    }

    @Test public void discoveryCardUsesRefsInsteadOfInflatedStoredAggregate() {
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup("discovery:성남", "성남", "성남",
                "KR", "대한민국", "경기", "", 999, 999, 0, 100L, 100L, "content://p/1",
                Arrays.asList(ref("content://p/1", MediaKind.PHOTO, false, 100L),
                        ref("content://v/1", MediaKind.VIDEO, false, 100L)), 0, 1L);
        assertEquals("사진 1장 · 동영상 1개", MemoryMediaCounts.forRecord(
                MemoryRepository.fromDiscoveryGroup(group)));
    }

    @Test public void reported2107ItemsRemainCompleteWhileFirstGridPageIsBounded() {
        java.util.List<DiscoveryPhotoRef> refs = new java.util.ArrayList<>();
        for (int i = 0; i < 2107; i++) refs.add(ref("content://p/" + i, MediaKind.PHOTO, false, 1000L + i));
        java.util.List<MemoryPhotoSection> sections = MemoryPhotoSection.fromDiscoveryRefs(refs);
        assertEquals(2107, MemoryPhotoPage.totalItemCount(sections));
        assertEquals("사진 2107장", MemoryMediaCounts.fromSections(sections).text());
        assertEquals(48, MemoryPhotoPage.from(sections, 0, 48).itemCount);
        assertEquals(48, MemoryPhotoPage.from(sections, 48, 48).itemCount);
        assertEquals(43, MemoryPhotoPage.from(sections, 2064, 48).itemCount);
        assertEquals("content://p/2106", sections.get(0).photos.get(0).sourceUri);
    }
}

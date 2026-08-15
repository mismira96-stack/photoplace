package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class MemoryPhotoSectionTest {
    @Test
    public void groupsRefsByDateNewestFirst() {
        List<MemoryPhotoSection> sections = MemoryPhotoSection.fromDiscoveryRefs(Arrays.asList(
                ref("content://photo/old", at(2026, Calendar.AUGUST, 10), "수원"),
                ref("content://photo/new-1", at(2026, Calendar.AUGUST, 13), "수원"),
                ref("content://photo/new-2", at(2026, Calendar.AUGUST, 13), "수원")));

        assertEquals(2, sections.size());
        assertEquals("8월 13일", sections.get(0).dateText);
        assertEquals("수원", sections.get(0).placeText);
        assertEquals(2, sections.get(0).photos.size());
        assertEquals("content://photo/new-1", sections.get(0).photos.get(0).sourceUri);
        assertEquals("content://photo/new-2", sections.get(0).photos.get(1).sourceUri);
        assertEquals("8월 10일", sections.get(1).dateText);
    }

    @Test
    public void filtersStaleAndEmptySourceUris() {
        List<MemoryPhotoSection> sections = MemoryPhotoSection.fromDiscoveryRefs(Arrays.asList(
                ref("", at(2026, Calendar.AUGUST, 13), "수원"),
                staleRef("content://photo/stale", at(2026, Calendar.AUGUST, 13), "수원"),
                ref("content://photo/live", at(2026, Calendar.AUGUST, 13), "수원")));

        assertEquals(1, sections.size());
        assertEquals(1, sections.get(0).photos.size());
        assertEquals("content://photo/live", sections.get(0).photos.get(0).sourceUri);
    }

    @Test
    public void sourceUrisFlattensSectionsInDisplayOrder() {
        List<MemoryPhotoSection> sections = MemoryPhotoSection.fromDiscoveryRefs(Arrays.asList(
                ref("content://photo/old", at(2026, Calendar.AUGUST, 10), "수원"),
                ref("content://photo/new", at(2026, Calendar.AUGUST, 13), "수원")));

        List<String> uris = MemoryPhotoSection.sourceUris(sections);

        assertEquals(Arrays.asList("content://photo/new", "content://photo/old"), uris);
    }

    @Test
    public void includesYearForOlderSections() {
        List<MemoryPhotoSection> sections = MemoryPhotoSection.fromDiscoveryRefs(Arrays.asList(
                ref("content://photo/old", at(2025, Calendar.AUGUST, 13), "수원")));

        assertEquals("2025년 8월 13일", sections.get(0).dateText);
    }

    @Test
    public void emptyOrInvalidRefsReturnEmptySections() {
        assertTrue(MemoryPhotoSection.fromDiscoveryRefs(null).isEmpty());
        assertTrue(MemoryPhotoSection.fromDiscoveryRefs(Arrays.asList(
                staleRef("content://photo/stale", at(2026, Calendar.AUGUST, 13), "수원"),
                ref("", at(2026, Calendar.AUGUST, 13), "수원"))).isEmpty());
    }

    private static DiscoveryPhotoRef ref(String uri, long takenAtMillis, String placeName) {
        return new DiscoveryPhotoRef(
                uri,
                DiscoveryPhotoRef.UNKNOWN_ID,
                MediaKind.PHOTO,
                "image/jpeg",
                "IMG.jpg",
                takenAtMillis,
                placeName,
                placeName,
                "KR",
                "대한민국",
                "경기도",
                "",
                "",
                1L,
                1L,
                false);
    }

    private static DiscoveryPhotoRef staleRef(String uri, long takenAtMillis, String placeName) {
        return new DiscoveryPhotoRef(
                uri,
                DiscoveryPhotoRef.UNKNOWN_ID,
                MediaKind.PHOTO,
                "image/jpeg",
                "IMG.jpg",
                takenAtMillis,
                placeName,
                placeName,
                "KR",
                "대한민국",
                "경기도",
                "",
                "",
                1L,
                1L,
                true);
    }

    private static long at(int year, int month, int day) {
        return new GregorianCalendar(year, month, day, 12, 0, 0).getTimeInMillis();
    }
}

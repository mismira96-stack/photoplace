package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class MemoryPhotoPageTest {
    @Test
    public void slicesAcrossDateSectionsWithoutRepeatingPhotos() {
        MemoryPhotoPage first = MemoryPhotoPage.from(Arrays.asList(section("1", 3), section("2", 3)), 0, 4);
        MemoryPhotoPage second = MemoryPhotoPage.from(Arrays.asList(section("1", 3), section("2", 3)), 4, 4);

        assertEquals(4, first.itemCount);
        assertEquals(2, first.slices.size());
        assertEquals(0, first.slices.get(0).startIndex);
        assertEquals(3, first.slices.get(0).itemCount);
        assertEquals(0, first.slices.get(1).startIndex);
        assertEquals(1, first.slices.get(1).itemCount);
        assertEquals(2, second.itemCount);
        assertEquals(1, second.slices.size());
        assertEquals(1, second.slices.get(0).startIndex);
        assertEquals(2, second.slices.get(0).itemCount);
    }

    @Test
    public void totalCountIgnoresEmptySections() {
        assertEquals(3, MemoryPhotoPage.totalItemCount(Arrays.asList(section("1", 3), section("2", 0))));
        assertEquals(0, MemoryPhotoPage.totalItemCount(Collections.<MemoryPhotoSection>emptyList()));
    }

    @Test
    public void returnsEmptyPageWhenOffsetIsPastAllPhotos() {
        MemoryPhotoPage page = MemoryPhotoPage.from(Collections.singletonList(section("1", 3)), 20, 48);

        assertEquals(0, page.itemCount);
        assertTrue(page.slices.isEmpty());
    }

    private static MemoryPhotoSection section(String key, int count) {
        java.util.ArrayList<DiscoveryPhotoRef> refs = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            refs.add(new DiscoveryPhotoRef(
                    "content://media/" + key + "/" + index,
                    index,
                    MediaKind.PHOTO,
                    "image/jpeg",
                    "IMG.jpg",
                    1785600000000L + index,
                    "성남",
                    "성남",
                    "KR",
                    "대한민국",
                    "",
                    "",
                    "",
                    1L,
                    1L,
                    false));
        }
        return MemoryPhotoSection.fromDiscoveryRefs(refs).isEmpty()
                ? nullSection()
                : MemoryPhotoSection.fromDiscoveryRefs(refs).get(0);
    }

    private static MemoryPhotoSection nullSection() {
        return MemoryPhotoSection.fromDiscoveryRefs(Collections.<DiscoveryPhotoRef>emptyList()).isEmpty()
                ? null
                : null;
    }
}

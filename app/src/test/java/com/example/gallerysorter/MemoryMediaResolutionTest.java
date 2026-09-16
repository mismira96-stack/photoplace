package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class MemoryMediaResolutionTest {
    @Test
    public void galleryResultIsCopiedAndKeptAsSingleSource() {
        List<DiscoveryPhotoRef> input = new ArrayList<>();
        input.add(ref("content://gallery/1"));

        MemoryMediaResolution resolution = MemoryMediaResolution.fromGallery(input);
        input.clear();

        assertEquals(MemoryMediaSourcePolicy.Source.GALLERY_OUTPUT, resolution.source);
        assertEquals(1, resolution.refs.size());
        assertNotSame(input, resolution.refs);
    }

    @Test
    public void emptyDiscoveryDoesNotPretendToHaveMedia() {
        MemoryMediaResolution resolution = MemoryMediaResolution.fromDiscovery(
                Collections.<DiscoveryPhotoRef>emptyList());

        assertEquals(MemoryMediaSourcePolicy.Source.EMPTY, resolution.source);
        assertEquals(0, resolution.refs.size());
    }

    @Test
    public void unavailableDoesNotFallbackOrExposePartialRefs() {
        MemoryMediaResolution resolution = MemoryMediaResolution.unavailable();

        assertEquals(MemoryMediaSourcePolicy.Source.UNAVAILABLE, resolution.source);
        assertEquals(0, resolution.refs.size());
    }

    @Test
    public void relativePathNormalizationMatchesOrganizationLinkPaths() {
        assertEquals("Pictures/Songpa",
                MemoryMediaStoreReader.normalizePath("Pictures\\Songpa///"));
        assertEquals("", MemoryMediaStoreReader.normalizePath("///"));
    }

    private static DiscoveryPhotoRef ref(String uri) {
        return new DiscoveryPhotoRef(uri, 1L, MediaKind.PHOTO, "image/jpeg",
                "photo.jpg", 100L, "KR|Songpa", "송파구", "KR", "대한민국",
                "서울", "", "Pictures/Songpa", 0L, 0L, false);
    }
}

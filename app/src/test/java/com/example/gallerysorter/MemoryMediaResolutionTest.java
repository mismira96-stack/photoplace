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

    @Test
    public void relativePathQueryAcceptsBothMediaStoreRepresentations() {
        assertEquals(java.util.Arrays.asList("Pictures/Songpa", "Pictures/Songpa/"),
                MemoryMediaStoreReader.relativePathCandidates("Pictures/Songpa/"));
    }

    @Test
    public void galleryOutputKeepsNewDiscoveryMediaWithoutDuplicateFilename() {
        DiscoveryPhotoRef existing = ref("content://discovery/existing", "photo.jpg", 100L);
        DiscoveryPhotoRef newer = ref("content://discovery/new", "new-photo.jpg", 300L);
        MemoryRecord memory = MemoryRepository.fromDiscoveryGroup(new DiscoveryMemoryGroup(
                "discovery:songpa", "songpa", "송파구", "KR", "대한민국", "서울", "송파구",
                2, 2, 0, 100L, 300L, newer.sourceUri,
                java.util.Arrays.asList(existing, newer), 0, 1L));
        OrganizationLink link = new OrganizationLink(
                "link-songpa", OrganizationLink.SubjectType.MEMORY, "mem_songpa",
                "request-songpa", "송파구에서", "Pictures/송파구에서/", 1L,
                OrganizationLink.Status.SUCCESS, 1, 0, 0);

        MemoryMediaResolution resolution = MemoryMediaResolver.resolve(memory, link,
                new MemoryMediaResolver.GalleryReader() {
                    @Override
                    public MemoryMediaStoreReader.Result read(String relativePath, MemoryRecord value) {
                        return MemoryMediaStoreReader.Result.found(
                                Collections.singletonList(ref("content://gallery/existing", "photo.jpg", 100L)));
                    }
                });

        assertEquals(2, resolution.refs.size());
        assertEquals("content://discovery/new", resolution.refs.get(0).sourceUri);
        assertEquals("content://gallery/existing", resolution.refs.get(1).sourceUri);
    }

    private static DiscoveryPhotoRef ref(String uri) {
        return ref(uri, "photo.jpg", 100L);
    }

    private static DiscoveryPhotoRef ref(String uri, String displayName, long takenAtMillis) {
        return new DiscoveryPhotoRef(uri, 1L, MediaKind.PHOTO, "image/jpeg",
                displayName, takenAtMillis, "KR|Songpa", "송파구", "KR", "대한민국",
                "서울", "", "Pictures/Songpa", 0L, 0L, false);
    }
}

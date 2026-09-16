package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Chooses one authoritative media source for a Memory. It never unions sources. */
final class MemoryMediaResolver {
    interface GalleryReader {
        MemoryMediaStoreReader.Result read(String relativePath, MemoryRecord memory);
    }

    private MemoryMediaResolver() {
    }

    static MemoryMediaResolution resolve(MemoryRecord memory,
                                         OrganizationLink exactMemoryLink,
                                         GalleryReader galleryReader) {
        if (memory == null) {
            return MemoryMediaResolution.empty();
        }
        List<DiscoveryPhotoRef> discoveryRefs = liveRefs(memory.discoveryGroup == null
                ? Collections.<DiscoveryPhotoRef>emptyList()
                : memory.discoveryGroup.photoRefs);
        MemoryMediaStoreReader.Result gallery = null;
        if (galleryReader != null && exactMemoryLink != null
                && exactMemoryLink.subjectType == OrganizationLink.SubjectType.MEMORY) {
            gallery = galleryReader.read(exactMemoryLink.relativePath, memory);
        }
        MemoryMediaSourcePolicy.GalleryLookup lookup = gallery == null
                ? MemoryMediaSourcePolicy.GalleryLookup.UNKNOWN
                : gallery.lookup;
        MemoryMediaSourcePolicy.Source source = MemoryMediaSourcePolicy.choose(
                exactMemoryLink, lookup, !discoveryRefs.isEmpty());
        if (source == MemoryMediaSourcePolicy.Source.GALLERY_OUTPUT) {
            return MemoryMediaResolution.fromGallery(gallery.refs);
        }
        if (source == MemoryMediaSourcePolicy.Source.DISCOVERY) {
            return MemoryMediaResolution.fromDiscovery(discoveryRefs);
        }
        if (source == MemoryMediaSourcePolicy.Source.UNAVAILABLE) {
            return MemoryMediaResolution.unavailable();
        }
        return MemoryMediaResolution.empty();
    }

    private static List<DiscoveryPhotoRef> liveRefs(List<DiscoveryPhotoRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<DiscoveryPhotoRef> live = new ArrayList<>();
        for (DiscoveryPhotoRef ref : refs) {
            if (ref != null && !ref.stale && ref.sourceUri != null
                    && !ref.sourceUri.trim().isEmpty()) {
                live.add(ref);
            }
        }
        return Collections.unmodifiableList(live);
    }
}

package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result of choosing one Memory media source. */
final class MemoryMediaResolution {
    final MemoryMediaSourcePolicy.Source source;
    final List<DiscoveryPhotoRef> refs;

    private MemoryMediaResolution(MemoryMediaSourcePolicy.Source source,
                                  List<DiscoveryPhotoRef> refs) {
        this.source = source;
        this.refs = refs == null || refs.isEmpty()
                ? Collections.<DiscoveryPhotoRef>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(refs));
    }

    static MemoryMediaResolution fromGallery(List<DiscoveryPhotoRef> refs) {
        return new MemoryMediaResolution(
                MemoryMediaSourcePolicy.Source.GALLERY_OUTPUT, refs);
    }

    static MemoryMediaResolution fromDiscovery(List<DiscoveryPhotoRef> refs) {
        return new MemoryMediaResolution(
                refs == null || refs.isEmpty()
                        ? MemoryMediaSourcePolicy.Source.EMPTY
                        : MemoryMediaSourcePolicy.Source.DISCOVERY,
                refs);
    }

    static MemoryMediaResolution unavailable() {
        return new MemoryMediaResolution(
                MemoryMediaSourcePolicy.Source.UNAVAILABLE,
                Collections.<DiscoveryPhotoRef>emptyList());
    }

    static MemoryMediaResolution empty() {
        return new MemoryMediaResolution(
                MemoryMediaSourcePolicy.Source.EMPTY,
                Collections.<DiscoveryPhotoRef>emptyList());
    }
}

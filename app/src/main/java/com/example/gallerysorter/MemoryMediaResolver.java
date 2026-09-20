package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves the authoritative Gallery output and appends only newly discovered media. */
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
            return MemoryMediaResolution.fromGallery(withNewDiscovery(gallery.refs, discoveryRefs));
        }
        if (source == MemoryMediaSourcePolicy.Source.DISCOVERY) {
            return MemoryMediaResolution.fromDiscovery(discoveryRefs);
        }
        if (source == MemoryMediaSourcePolicy.Source.UNAVAILABLE) {
            return MemoryMediaResolution.unavailable();
        }
        return MemoryMediaResolution.empty();
    }

    private static List<DiscoveryPhotoRef> withNewDiscovery(List<DiscoveryPhotoRef> galleryRefs,
                                                             List<DiscoveryPhotoRef> discoveryRefs) {
        ArrayList<DiscoveryPhotoRef> merged = new ArrayList<>();
        Set<String> seenUris = new HashSet<>();
        Set<String> knownFiles = new HashSet<>();
        addRefs(galleryRefs, merged, seenUris, knownFiles, false);
        addRefs(discoveryRefs, merged, seenUris, knownFiles, true);
        Collections.sort(merged, new Comparator<DiscoveryPhotoRef>() {
            @Override
            public int compare(DiscoveryPhotoRef left, DiscoveryPhotoRef right) {
                return Long.compare(sortTime(right), sortTime(left));
            }
        });
        return merged;
    }

    private static void addRefs(List<DiscoveryPhotoRef> refs,
                                List<DiscoveryPhotoRef> output,
                                Set<String> seenUris,
                                Set<String> knownFiles,
                                boolean skipKnownFiles) {
        if (refs == null) {
            return;
        }
        for (DiscoveryPhotoRef ref : refs) {
            if (ref == null || ref.stale || ref.sourceUri == null || ref.sourceUri.trim().isEmpty()) {
                continue;
            }
            String uri = ref.sourceUri;
            String fileKey = mediaFileKey(ref);
            if (!seenUris.add(uri) || (skipKnownFiles && !fileKey.isEmpty() && knownFiles.contains(fileKey))) {
                continue;
            }
            output.add(ref);
            if (!fileKey.isEmpty()) {
                knownFiles.add(fileKey);
            }
        }
    }

    private static String mediaFileKey(DiscoveryPhotoRef ref) {
        String name = MediaStoreAlbumLookup.fileSignature(ref.displayName);
        if (name.isEmpty()) {
            return "";
        }
        return ref.mediaKind.name() + ":" + name;
    }

    private static long sortTime(DiscoveryPhotoRef ref) {
        return ref == null || ref.takenAtMillis <= 0L ? 0L : ref.takenAtMillis;
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

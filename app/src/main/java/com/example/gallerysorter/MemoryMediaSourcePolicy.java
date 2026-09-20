package com.example.gallerysorter;

/** Chooses the primary source for a Memory. New Discovery media may extend Gallery output. */
final class MemoryMediaSourcePolicy {
    enum GalleryLookup {
        FOUND,
        MISSING,
        FAILED,
        UNKNOWN
    }

    enum Source {
        GALLERY_OUTPUT,
        DISCOVERY,
        EMPTY,
        UNAVAILABLE
    }

    private MemoryMediaSourcePolicy() {
    }

    static Source choose(OrganizationLink exactMemoryLink,
                         GalleryLookup galleryLookup,
                         boolean hasLiveDiscoveryMedia) {
        if (hasUsableMemoryLink(exactMemoryLink)) {
            if (galleryLookup == GalleryLookup.FOUND) {
                return Source.GALLERY_OUTPUT;
            }
            if (galleryLookup == null || galleryLookup == GalleryLookup.FAILED
                    || galleryLookup == GalleryLookup.UNKNOWN) {
                return Source.UNAVAILABLE;
            }
        }
        return hasLiveDiscoveryMedia ? Source.DISCOVERY : Source.EMPTY;
    }

    private static boolean hasUsableMemoryLink(OrganizationLink link) {
        return link != null
                && link.isValid()
                && link.subjectType == OrganizationLink.SubjectType.MEMORY
                && (link.status == OrganizationLink.Status.SUCCESS
                || link.status == OrganizationLink.Status.PARTIAL);
    }
}

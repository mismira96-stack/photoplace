package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MemoryPhotoPage {
    final List<SectionSlice> slices;
    final int itemCount;

    private MemoryPhotoPage(List<SectionSlice> slices, int itemCount) {
        this.slices = Collections.unmodifiableList(new ArrayList<>(slices));
        this.itemCount = Math.max(0, itemCount);
    }

    static MemoryPhotoPage from(List<MemoryPhotoSection> sections, int offset, int limit) {
        if (sections == null || sections.isEmpty() || limit <= 0) {
            return new MemoryPhotoPage(Collections.<SectionSlice>emptyList(), 0);
        }
        int skipped = 0;
        int remaining = limit;
        ArrayList<SectionSlice> slices = new ArrayList<>();
        for (MemoryPhotoSection section : sections) {
            int sectionSize = section == null || section.photos == null ? 0 : section.photos.size();
            if (sectionSize <= 0) {
                continue;
            }
            if (skipped + sectionSize <= offset) {
                skipped += sectionSize;
                continue;
            }
            int startIndex = Math.max(0, offset - skipped);
            int count = Math.min(sectionSize - startIndex, remaining);
            if (count > 0) {
                slices.add(new SectionSlice(section, startIndex, count));
                remaining -= count;
            }
            skipped += sectionSize;
            if (remaining <= 0) {
                break;
            }
        }
        return new MemoryPhotoPage(slices, limit - remaining);
    }

    static int totalItemCount(List<MemoryPhotoSection> sections) {
        int total = 0;
        if (sections != null) {
            for (MemoryPhotoSection section : sections) {
                total += section == null || section.photos == null ? 0 : section.photos.size();
            }
        }
        return total;
    }

    static final class SectionSlice {
        final MemoryPhotoSection section;
        final int startIndex;
        final int itemCount;

        SectionSlice(MemoryPhotoSection section, int startIndex, int itemCount) {
            this.section = section;
            this.startIndex = Math.max(0, startIndex);
            this.itemCount = Math.max(0, itemCount);
        }
    }
}

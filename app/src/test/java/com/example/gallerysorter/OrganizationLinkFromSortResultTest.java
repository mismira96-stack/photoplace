package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Collections;

public class OrganizationLinkFromSortResultTest {
    @Test
    public void confirmedCompleteOutputCreatesSuccessLink() {
        SortResultStore.Snapshot result = result(1, 0, 0, false, "Pictures/Place/");

        OrganizationLink link = OrganizationLinkFromSortResult.create(
                request(), result, 500L);

        assertEquals(OrganizationLink.Status.SUCCESS, link.status);
        assertEquals("mem_place", link.subjectId);
        assertEquals("request-1", link.requestId);
        assertEquals(1, link.copiedCount);
    }

    @Test
    public void failedOrCanceledOutputIsPartialAndZeroCopyCreatesNoLink() {
        OrganizationLink failed = OrganizationLinkFromSortResult.create(
                request(), result(1, 2, 1, false, "Pictures/Place/"), 500L);
        OrganizationLink canceled = OrganizationLinkFromSortResult.create(
                request(), result(1, 2, 0, true, "Pictures/Place/"), 500L);

        assertEquals(OrganizationLink.Status.PARTIAL, failed.status);
        assertEquals(OrganizationLink.Status.PARTIAL, canceled.status);
        assertNull(OrganizationLinkFromSortResult.create(
                request(), result(0, 3, 0, false, "Pictures/Place/"), 500L));
    }

    @Test
    public void refusesResultWhoseCopiedItemsTargetAnotherAlbum() {
        assertNull(OrganizationLinkFromSortResult.create(
                request(), result(1, 0, 0, false, "Pictures/Other/"), 500L));
    }

    private static OrganizationRequest request() {
        return new OrganizationRequest("request-1", OrganizationLink.SubjectType.MEMORY,
                "mem_place", "Place", "Pictures/Place/");
    }

    private static SortResultStore.Snapshot result(int copied, int skipped, int failed,
                                                   boolean canceled, String targetPath) {
        PhotoItem item = new PhotoItem(null, "photo.jpg", "image/jpeg",
                null, "Place", false, true, false, targetPath, false, "", "", "", "");
        return new SortResultStore.Snapshot(400L, copied, skipped, failed, canceled,
                Collections.emptyList(), copied == 0
                ? Collections.<PhotoItem>emptyList() : Collections.singletonList(item),
                Collections.emptyList(), "", request());
    }
}

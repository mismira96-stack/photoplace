package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class OrganizationLinkTest {
    @Test
    public void jsonRoundTripPreservesMemoryOutputAndPartialCounts() throws Exception {
        OrganizationLink original = new OrganizationLink(
                "link-1",
                OrganizationLink.SubjectType.MEMORY,
                "mem_place1",
                "request-1",
                "송파에서",
                "Pictures/송파에서/",
                1234L,
                OrganizationLink.Status.PARTIAL,
                8,
                2,
                1);

        OrganizationLink restored = OrganizationLink.fromJson(original.toJson());

        assertNotNull(restored);
        assertEquals(original.linkId, restored.linkId);
        assertEquals(original.subjectType, restored.subjectType);
        assertEquals(original.subjectId, restored.subjectId);
        assertEquals(original.requestId, restored.requestId);
        assertEquals(original.albumName, restored.albumName);
        assertEquals(original.relativePath, restored.relativePath);
        assertEquals(original.organizedAtMillis, restored.organizedAtMillis);
        assertEquals(original.status, restored.status);
        assertEquals(original.copiedCount, restored.copiedCount);
        assertEquals(original.skippedCount, restored.skippedCount);
        assertEquals(original.failedCount, restored.failedCount);
    }

    @Test
    public void collectionSubjectAndMissingOutputAreValid() throws Exception {
        OrganizationLink link = new OrganizationLink(
                "link-2",
                OrganizationLink.SubjectType.COLLECTION,
                "group_trip1",
                "request-2",
                "2026 일본 여행",
                "Pictures/2026 일본 여행/",
                5678L,
                OrganizationLink.Status.MISSING,
                20,
                0,
                0);

        assertTrue(link.isValid());
        assertEquals(OrganizationLink.SubjectType.COLLECTION,
                OrganizationLink.fromJson(link.toJson()).subjectType);
    }

    @Test
    public void invalidIdentityStatusAndCountsAreRejected() throws Exception {
        assertNull(OrganizationLink.fromJson(json("MEMORY", "group_wrong", "SUCCESS", 0)));
        assertNull(OrganizationLink.fromJson(json("COLLECTION", "mem_wrong", "SUCCESS", 0)));
        assertNull(OrganizationLink.fromJson(json("MEMORY", "mem_ok", "UNKNOWN", 0)));
        assertNull(OrganizationLink.fromJson(json("MEMORY", "mem_ok", "SUCCESS", -1)));
    }

    private static JSONObject json(String type, String subjectId, String status, int copiedCount)
            throws Exception {
        JSONObject json = new JSONObject();
        json.put("linkId", "link-1");
        json.put("subjectType", type);
        json.put("subjectId", subjectId);
        json.put("requestId", "request-1");
        json.put("albumName", "장소에서");
        json.put("relativePath", "Pictures/장소에서/");
        json.put("organizedAtMillis", 123L);
        json.put("status", status);
        json.put("copiedCount", copiedCount);
        json.put("skippedCount", 0);
        json.put("failedCount", 0);
        return json;
    }
}

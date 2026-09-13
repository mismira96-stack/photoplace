package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.json.JSONObject;
import org.junit.Test;

public class OrganizationRequestTest {
    @Test
    public void jsonRoundTripPreservesStableRequestContext() throws Exception {
        OrganizationRequest original = request("request-1", "mem_place");

        OrganizationRequest restored = OrganizationRequest.fromJson(original.toJson());

        assertNotNull(restored);
        assertEquals(original.requestId, restored.requestId);
        assertEquals(original.subjectType, restored.subjectType);
        assertEquals(original.subjectId, restored.subjectId);
        assertEquals(original.albumName, restored.albumName);
        assertEquals(original.relativePath, restored.relativePath);
    }

    @Test
    public void rejectsInvalidOrIncompleteRequestContext() throws Exception {
        assertNull(OrganizationRequest.fromJson(null));
        assertNull(OrganizationRequest.fromJson(new JSONObject()
                .put("requestId", "request-1")
                .put("subjectType", "MEMORY")
                .put("subjectId", "mem_")
                .put("albumName", "Place")
                .put("relativePath", "Pictures/Place/")));
    }

    private static OrganizationRequest request(String requestId, String subjectId) {
        return new OrganizationRequest(requestId, OrganizationLink.SubjectType.MEMORY,
                subjectId, "Place", "Pictures/Place/");
    }
}

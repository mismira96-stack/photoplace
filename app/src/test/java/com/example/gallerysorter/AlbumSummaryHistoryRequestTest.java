package com.example.gallerysorter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class AlbumSummaryHistoryRequestTest {
    @Test
    public void recognizesPreviouslyCommittedRequestAndLeavesLegacySessionsDistinct() throws Exception {
        JSONArray sessions = new JSONArray();
        sessions.put(new JSONObject().put("createdAtMillis", 10L));
        sessions.put(new JSONObject().put("requestId", "request-1"));

        assertTrue(AlbumSummaryHistoryStore.containsRequestId(sessions, "request-1"));
        assertFalse(AlbumSummaryHistoryStore.containsRequestId(sessions, "request-2"));
        assertFalse(AlbumSummaryHistoryStore.containsRequestId(sessions, " "));
        assertFalse(AlbumSummaryHistoryStore.containsRequestId(null, "request-1"));
    }
}

package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupEvidenceRecordTest {
    private CleanupEvidenceRecord record(String job, String source, String target) {
        return new CleanupEvidenceRecord(job, source, target, "Pictures/Test/", 100L, 123L, 200L);
    }

    @Test public void replayKeepsIdentityAndDifferentTargetsAndJobsRemainSeparate() {
        String source = "content://media/external/images/media/1";
        String target = "content://media/external_primary/images/media/2";
        CleanupEvidenceRecord a = record("job1", source, target);
        assertEquals(a.recordId, record("job1", source, target).recordId);
        assertNotEquals(a.recordId, record("job2", source, target).recordId);
        assertNotEquals(a.recordId, record("job1", source, "content://media/external/images/media/3").recordId);
        assertEquals(64, a.recordId.length());
        assertEquals("COPY_OBSERVED_UNVERIFIED", CleanupEvidenceRecord.STATE);
        assertEquals(123L, a.copiedBytes);
    }

    @Test public void rejectsNonLocalVideosSameUriAndInvalidObservations() {
        String source = "content://media/external/images/media/1";
        for (String target : new String[]{source, "file:///photo.jpg", "content://cloud/images/media/2",
                "content://media/external/video/media/2", "content://media/internal/images/media/2",
                "content://media/external/images/media/2?edited=1", "", null}) {
            try { record("job", source, target); fail("Accepted " + target); }
            catch (IllegalArgumentException expected) { }
        }
        try { new CleanupEvidenceRecord("job", source, "content://media/external/images/media/2",
                "Pictures/Test/", 0, -1, 1); fail(); }
        catch (IllegalArgumentException expected) { }
    }
}

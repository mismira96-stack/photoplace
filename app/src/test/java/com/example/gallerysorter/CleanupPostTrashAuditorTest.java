package com.example.gallerysorter;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class CleanupPostTrashAuditorTest {

    private static final class FakeAccess implements CleanupPairVerifier.Access {
        final Map<String, CleanupPairVerifier.Metadata> metadataMap = new HashMap<>();
        final Map<String, byte[]> streamMap = new HashMap<>();

        @Override
        public CleanupPairVerifier.Metadata metadata(String uri) {
            return metadataMap.get(uri);
        }

        @Override
        public InputStream open(String uri) {
            byte[] bytes = streamMap.get(uri);
            return bytes != null ? new ByteArrayInputStream(bytes) : null;
        }
    }

    private static final class FakeJournal implements CleanupPostTrashAuditor.Journal {
        String lastRecordedStatus;
        CleanupEvidenceStore.JournalEntry entry;

        FakeJournal(CleanupEvidenceStore.JournalEntry entry) {
            this.entry = entry;
        }

        @Override
        public void updateJournalStatus(String reqId, String status) {
            this.lastRecordedStatus = status;
        }

        @Override
        public CleanupEvidenceStore.JournalEntry readJournal(String reqId) {
            return entry;
        }
    }

    @Test
    public void auditWithUnknownRequestReturnsUnresolved() {
        CleanupPostTrashAuditor auditor = new CleanupPostTrashAuditor(
                new FakeJournal(null),
                new FakeAccess());

        CleanupPostTrashAuditor.Result result = auditor.audit("unknown", () -> false);
        assertEquals(CleanupPostTrashAuditor.Status.UNRESOLVED, result.status);
    }

    @Test
    public void auditWithCanceledReturnsCanceled() {
        CleanupEvidenceStore.JournalEntry entry = new CleanupEvidenceStore.JournalEntry(
                "req_1", "batch_1",
                Collections.singletonList("content://media/external/images/media/1"),
                Collections.singletonList("content://media/external/images/media/2"),
                "REQUESTED", 1000L, 1000L);

        FakeJournal journal = new FakeJournal(entry);
        CleanupPostTrashAuditor auditor = new CleanupPostTrashAuditor(journal, new FakeAccess());

        CleanupPostTrashAuditor.Result result = auditor.audit("req_1", () -> true);
        assertEquals(CleanupPostTrashAuditor.Status.CANCELED, result.status);
        assertEquals("AUDIT_PENDING", journal.lastRecordedStatus);
    }

    @Test
    public void auditWithMissingTargetFailsNeedsRecovery() {
        CleanupEvidenceStore.JournalEntry entry = new CleanupEvidenceStore.JournalEntry(
                "req_1", "batch_1",
                Collections.singletonList("content://media/external/images/media/1"),
                Collections.singletonList("content://media/external/images/media/2"),
                "REQUESTED", 1000L, 1000L);

        FakeJournal journal = new FakeJournal(entry);
        FakeAccess access = new FakeAccess(); // no target registered

        CleanupPostTrashAuditor auditor = new CleanupPostTrashAuditor(journal, access);

        CleanupPostTrashAuditor.Result result = auditor.audit("req_1", () -> false);
        assertEquals(CleanupPostTrashAuditor.Status.UNRESOLVED, result.status);
        assertEquals("UNRESOLVED", journal.lastRecordedStatus);
        assertTrue(result.message.contains("복구해 주세요"));
    }

    @Test
    public void legacyRequestWithoutExpectedHashCannotPass() {
        String targetUri = "content://media/external/images/media/2";
        CleanupEvidenceStore.JournalEntry entry = new CleanupEvidenceStore.JournalEntry(
                "req_1", "batch_1",
                Collections.singletonList("content://media/external/images/media/1"),
                Collections.singletonList(targetUri),
                "REQUESTED", 1000L, 1000L);

        FakeJournal journal = new FakeJournal(entry);
        FakeAccess access = new FakeAccess();
        access.metadataMap.put(targetUri, new CleanupPairVerifier.Metadata(
                4, "id_target", "Pictures/성남에서/photo.jpg", true));
        access.streamMap.put(targetUri, new byte[]{1, 2, 3, 4});

        CleanupPostTrashAuditor auditor = new CleanupPostTrashAuditor(journal, access);

        CleanupPostTrashAuditor.Result result = auditor.audit("req_1", () -> false);
        assertEquals(CleanupPostTrashAuditor.Status.UNRESOLVED, result.status);
        assertEquals("UNRESOLVED", journal.lastRecordedStatus);
        assertEquals(0, result.auditedCount);
        assertTrue(result.message.contains("대조 증거가 없어요"));
    }
}

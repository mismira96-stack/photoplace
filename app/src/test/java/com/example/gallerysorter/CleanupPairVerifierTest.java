package com.example.gallerysorter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupPairVerifierTest {
    private static final String SOURCE = "content://media/external/images/media/1";
    private static final String TARGET = "content://media/external/images/media/2";
    private static final String PATH = "Pictures/Test/";
    static class Access implements CleanupPairVerifier.Access {
        byte[] source = new byte[200000], target = new byte[200000];
        int closed, opened, metadataCalls;
        boolean changed, missing, denied, nullStream, pending;
        long metadataSize = 200000;
        String path = PATH;
        @Override public CleanupPairVerifier.Metadata metadata(String uri) {
            if (denied) throw new SecurityException("Permission revoked");
            if (missing) return null;
            metadataCalls++;
            return new CleanupPairVerifier.Metadata(metadataSize,
                    uri + (changed && metadataCalls > 2 ? "|generation2" : "|generation1"),
                    uri.equals(TARGET) ? path : "DCIM/Camera/", !pending);
        }
        @Override public InputStream open(String uri) {
            if (nullStream) return null;
            opened++;
            return new ByteArrayInputStream(uri.equals(SOURCE) ? source : target) {
                @Override public void close() { closed++; }
            };
        }
    }
    private CleanupEvidenceRecord record() {
        return new CleanupEvidenceRecord("job", SOURCE, TARGET, PATH, 1, 200000, 1);
    }
    private CleanupPairVerifier.Result verify(Access access) {
        return new CleanupPairVerifier(access).verify(record(), () -> false);
    }

    @Test public void fullMatchingStreamsReturnShaAndCloseBothHandles() throws Exception {
        Access access = new Access();
        Arrays.fill(access.source, (byte) 31); Arrays.fill(access.target, (byte) 31);
        CleanupPairVerifier.Result result = verify(access);
        assertEquals(CleanupPairVerifier.Status.MATCH, result.status);
        assertEquals(CleanupPairVerifier.hex(java.security.MessageDigest.getInstance("SHA-256").digest(access.source)), result.sha256);
        assertEquals(2, access.closed); assertEquals(4, access.metadataCalls);
    }
    @Test public void equalSizeAndPrefixWithDifferentLastByteCannotMatch() {
        Access access = new Access(); access.target[199999] = 1;
        assertEquals(CleanupPairVerifier.Status.CONTENT_MISMATCH, verify(access).status);
        assertEquals(2, access.closed);
    }
    @Test public void unavailableOriginalBytesCannotFallBackToARedactedStream() {
        Access access = new Access() {
            @Override public InputStream open(String uri) {
                opened++;
                throw new UnsupportedOperationException("Original bytes cannot be provided");
            }
        };
        CleanupPairVerifier.Result result = verify(access);
        assertEquals(CleanupPairVerifier.Status.UNREADABLE, result.status);
        assertEquals("", result.sha256);
        assertEquals(1, access.opened);
    }
    @Test public void wrongMetadataSizeDoesNotOpenStreams() {
        Access access = new Access(); access.metadataSize = 1;
        assertEquals(CleanupPairVerifier.Status.SIZE_MISMATCH, verify(access).status);
        assertEquals(0, access.opened);
    }
    @Test public void truncatedStreamCannotPassEvenWithMatchingMetadataSize() {
        Access access = new Access(); access.target = new byte[199999];
        assertEquals(CleanupPairVerifier.Status.SIZE_MISMATCH, verify(access).status);
        assertEquals(2, access.closed);
    }
    @Test public void oversizedStreamStopsAndClosesWithoutAcceptingPrefix() {
        Access access = new Access(); access.source = new byte[200001];
        assertEquals(CleanupPairVerifier.Status.SIZE_MISMATCH, verify(access).status);
        assertEquals(1, access.closed);
    }
    @Test public void changedGenerationInvalidatesMatchingBytes() {
        Access access = new Access(); access.changed = true;
        assertEquals(CleanupPairVerifier.Status.CHANGED, verify(access).status);
    }
    @Test public void movedTargetInvalidatesRecordedDestination() {
        Access access = new Access(); access.path = "Pictures/Other/";
        assertEquals(CleanupPairVerifier.Status.CHANGED, verify(access).status);
        assertEquals(0, access.opened);
    }
    @Test public void missingDeniedNullAndPendingAllStayUnknown() {
        for (int i = 0; i < 4; i++) {
            Access access = new Access();
            access.missing = i == 0; access.denied = i == 1;
            access.nullStream = i == 2; access.pending = i == 3;
            CleanupPairVerifier.Result result = verify(access);
            assertEquals(CleanupPairVerifier.Status.UNREADABLE, result.status);
            assertEquals("", result.sha256);
        }
    }
    @Test public void cancellationDuringFullReadClosesTheOpenStream() {
        Access access = new Access(); AtomicInteger checks = new AtomicInteger();
        CleanupPairVerifier.Result result = new CleanupPairVerifier(access).verify(record(),
                () -> checks.incrementAndGet() > 2);
        assertEquals(CleanupPairVerifier.Status.CANCELED, result.status);
        assertEquals(1, access.closed); assertEquals("", result.sha256);
    }
    @Test public void preCanceledRequestDoesNotReadAnything() {
        Access access = new Access();
        assertEquals(CleanupPairVerifier.Status.CANCELED,
                new CleanupPairVerifier(access).verify(record(), () -> true).status);
        assertEquals(0, access.metadataCalls); assertEquals(0, access.opened);
    }
}

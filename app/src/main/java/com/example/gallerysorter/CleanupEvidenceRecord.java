package com.example.gallerysorter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** An observed copy, never authorization to trash. Content and Memory checks are deferred. */
final class CleanupEvidenceRecord {
    private static final java.util.regex.Pattern LOCAL_PHOTO = java.util.regex.Pattern.compile(
            "content://media/(?!internal/)[^/]+/images/media/[0-9]+");
    static final String STATE = "COPY_OBSERVED_UNVERIFIED";
    final String recordId, jobId, sourceUri, targetUri, targetRelativePath;
    final long takenAtMillis, copiedBytes, observedAtMillis;

    CleanupEvidenceRecord(String jobId, String sourceUri, String targetUri,
                          String targetRelativePath, long takenAtMillis, long copiedBytes,
                          long observedAtMillis) {
        if (jobId == null || jobId.isEmpty() || !localPhoto(sourceUri) || !localPhoto(targetUri)
                || sourceUri.equals(targetUri) || targetRelativePath == null
                || targetRelativePath.isEmpty() || copiedBytes < 0 || observedAtMillis <= 0) {
            throw new IllegalArgumentException("Invalid local photo copy observation");
        }
        this.jobId = jobId;
        this.sourceUri = sourceUri;
        this.targetUri = targetUri;
        this.targetRelativePath = targetRelativePath;
        this.takenAtMillis = takenAtMillis;
        this.copiedBytes = copiedBytes;
        this.observedAtMillis = observedAtMillis;
        this.recordId = identity(jobId, sourceUri, targetUri);
    }

    private static boolean localPhoto(String value) {
        return value != null && LOCAL_PHOTO.matcher(value).matches();
    }

    private static String identity(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                // Length framing prevents ambiguous job/source/target concatenations.
                digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
                digest.update(bytes);
            }
            StringBuilder text = new StringBuilder();
            for (byte value : digest.digest()) {
                text.append(Character.forDigit((value >>> 4) & 15, 16));
                text.append(Character.forDigit(value & 15, 16));
            }
            return text.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}

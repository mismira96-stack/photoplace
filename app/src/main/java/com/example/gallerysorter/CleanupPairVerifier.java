package com.example.gallerysorter;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.function.BooleanSupplier;

/** Reads both complete streams off the UI/copy threads. A result is an observation, not permission. */
final class CleanupPairVerifier {
    enum Status { MATCH, SIZE_MISMATCH, CONTENT_MISMATCH, CHANGED, UNREADABLE, CANCELED }
    interface Access {
        Metadata metadata(String uri) throws Exception;
        InputStream open(String uri) throws Exception;
    }
    static final class Metadata {
        final long size;
        final String identity, path;
        final boolean readablePhoto;
        Metadata(long size, String identity, String path, boolean readablePhoto) {
            this.size = size; this.identity = identity; this.path = path; this.readablePhoto = readablePhoto;
        }
        boolean same(Metadata other) {
            return other != null && readablePhoto && other.readablePhoto && size == other.size
                    && identity != null && !identity.isEmpty() && identity.equals(other.identity)
                    && path != null && path.equals(other.path);
        }
    }
    static final class Result {
        final Status status;
        final String sha256;
        final long checkedAtMillis;
        Result(Status status, String sha256) {
            this.status = status; this.sha256 = sha256;
            this.checkedAtMillis = System.currentTimeMillis();
        }
    }
    private final Access access;
    CleanupPairVerifier(Access access) { this.access = access; }

    Result verify(CleanupEvidenceRecord record, BooleanSupplier canceled) {
        try {
            if (canceled.getAsBoolean()) return result(Status.CANCELED);
            Metadata source = access.metadata(record.sourceUri), target = access.metadata(record.targetUri);
            if (source == null || target == null || !source.readablePhoto || !target.readablePhoto)
                return result(Status.UNREADABLE);
            if (source.size != record.copiedBytes || target.size != record.copiedBytes)
                return result(Status.SIZE_MISMATCH);
            if (!path(target.path).equals(path(record.targetRelativePath))) return result(Status.CHANGED);
            byte[] a = hash(record.sourceUri, record.copiedBytes, canceled);
            byte[] b = hash(record.targetUri, record.copiedBytes, canceled);
            if (!source.same(access.metadata(record.sourceUri)) || !target.same(access.metadata(record.targetUri)))
                return result(Status.CHANGED);
            if (canceled.getAsBoolean()) return result(Status.CANCELED);
            if (!MessageDigest.isEqual(a, b)) return result(Status.CONTENT_MISMATCH);
            return new Result(Status.MATCH, hex(a));
        } catch (Canceled stopped) { return result(Status.CANCELED);
        } catch (WrongSize mismatch) { return result(Status.SIZE_MISMATCH);
        } catch (Exception unknown) { return result(Status.UNREADABLE); }
    }

    private byte[] hash(String uri, long expected, BooleanSupplier canceled) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long total = 0;
        try (InputStream stream = access.open(uri)) {
            if (stream == null) throw new java.io.IOException("No readable stream");
            byte[] buffer = new byte[65536];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (canceled.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new Canceled();
                if (count == 0) continue;
                total += count;
                if (total > expected) throw new WrongSize();
                digest.update(buffer, 0, count);
            }
        }
        if (total != expected) throw new WrongSize();
        return digest.digest();
    }
    static String path(String value) {
        String result = value == null ? "" : value.trim().replace('\\', '/');
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
    static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte value : bytes) {
            text.append(Character.forDigit((value >>> 4) & 15, 16));
            text.append(Character.forDigit(value & 15, 16));
        }
        return text.toString();
    }
    private static Result result(Status status) { return new Result(status, ""); }
    private static final class Canceled extends Exception { }
    private static final class WrongSize extends Exception { }
}

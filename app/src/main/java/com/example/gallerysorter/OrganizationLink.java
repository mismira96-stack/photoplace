package com.example.gallerysorter;

import org.json.JSONObject;

/** Durable description of a Gallery output produced for a Memory or collection. */
final class OrganizationLink {
    enum SubjectType {
        MEMORY,
        COLLECTION
    }

    enum Status {
        SUCCESS,
        PARTIAL,
        MISSING
    }

    final String linkId;
    final SubjectType subjectType;
    final String subjectId;
    final String requestId;
    final String albumName;
    final String relativePath;
    final long organizedAtMillis;
    final Status status;
    final int copiedCount;
    final int skippedCount;
    final int failedCount;

    OrganizationLink(String linkId,
                     SubjectType subjectType,
                     String subjectId,
                     String requestId,
                     String albumName,
                     String relativePath,
                     long organizedAtMillis,
                     Status status,
                     int copiedCount,
                     int skippedCount,
                     int failedCount) {
        this.linkId = clean(linkId);
        this.subjectType = subjectType;
        this.subjectId = clean(subjectId);
        this.requestId = clean(requestId);
        this.albumName = clean(albumName);
        this.relativePath = normalizeRelativePath(relativePath);
        this.organizedAtMillis = organizedAtMillis;
        this.status = status;
        this.copiedCount = copiedCount;
        this.skippedCount = skippedCount;
        this.failedCount = failedCount;
    }

    boolean isValid() {
        return !linkId.isEmpty()
                && subjectType != null
                && hasSubjectPrefix(subjectType, subjectId)
                && !requestId.isEmpty()
                && !albumName.isEmpty()
                && !relativePath.isEmpty()
                && organizedAtMillis > 0L
                && status != null
                && copiedCount >= 0
                && skippedCount >= 0
                && failedCount >= 0;
    }

    JSONObject toJson() throws Exception {
        if (!isValid()) {
            throw new IllegalStateException("Invalid organization link");
        }
        JSONObject json = new JSONObject();
        json.put("linkId", linkId);
        json.put("subjectType", subjectType.name());
        json.put("subjectId", subjectId);
        json.put("requestId", requestId);
        json.put("albumName", albumName);
        json.put("relativePath", relativePath);
        json.put("organizedAtMillis", organizedAtMillis);
        json.put("status", status.name());
        json.put("copiedCount", copiedCount);
        json.put("skippedCount", skippedCount);
        json.put("failedCount", failedCount);
        return json;
    }

    static OrganizationLink fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        try {
            OrganizationLink link = new OrganizationLink(
                    json.optString("linkId", ""),
                    SubjectType.valueOf(json.optString("subjectType", "")),
                    json.optString("subjectId", ""),
                    json.optString("requestId", ""),
                    json.optString("albumName", ""),
                    json.optString("relativePath", ""),
                    json.optLong("organizedAtMillis", 0L),
                    Status.valueOf(json.optString("status", "")),
                    json.optInt("copiedCount", -1),
                    json.optInt("skippedCount", -1),
                    json.optInt("failedCount", -1));
            return link.isValid() ? link : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean hasSubjectPrefix(SubjectType type, String id) {
        return type == SubjectType.MEMORY
                ? id.startsWith("mem_") && id.length() > "mem_".length()
                : type == SubjectType.COLLECTION
                && id.startsWith("group_") && id.length() > "group_".length();
    }

    private static String normalizeRelativePath(String value) {
        String path = clean(value).replace('\\', '/');
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? "" : path + "/";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.example.gallerysorter;

import org.json.JSONObject;

import java.util.UUID;

/** Stable subject context persisted across a background sort operation. */
final class OrganizationRequest {
    final String requestId;
    final OrganizationLink.SubjectType subjectType;
    final String subjectId;
    final String albumName;
    final String relativePath;

    OrganizationRequest(String requestId,
                        OrganizationLink.SubjectType subjectType,
                        String subjectId,
                        String albumName,
                        String relativePath) {
        this.requestId = clean(requestId);
        this.subjectType = subjectType;
        this.subjectId = clean(subjectId);
        this.albumName = clean(albumName);
        this.relativePath = clean(relativePath).replace('\\', '/');
    }

    static OrganizationRequest create(OrganizationLink.SubjectType subjectType,
                                      String subjectId,
                                      String albumName,
                                      String relativePath) {
        String requestId = "org_" + UUID.randomUUID().toString().replace("-", "");
        return new OrganizationRequest(requestId, subjectType, subjectId, albumName, relativePath);
    }

    boolean isValid() {
        if (requestId.isEmpty() || subjectType == null || albumName.isEmpty() || relativePath.isEmpty()) {
            return false;
        }
        return subjectType == OrganizationLink.SubjectType.MEMORY
                ? subjectId.startsWith("mem_") && subjectId.length() > "mem_".length()
                : subjectType == OrganizationLink.SubjectType.COLLECTION
                && subjectId.startsWith("group_") && subjectId.length() > "group_".length();
    }

    JSONObject toJson() throws Exception {
        if (!isValid()) {
            throw new IllegalStateException("Invalid organization request");
        }
        JSONObject json = new JSONObject();
        json.put("requestId", requestId);
        json.put("subjectType", subjectType.name());
        json.put("subjectId", subjectId);
        json.put("albumName", albumName);
        json.put("relativePath", relativePath);
        return json;
    }

    static OrganizationRequest fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        try {
            OrganizationRequest request = new OrganizationRequest(
                    json.optString("requestId", ""),
                    OrganizationLink.SubjectType.valueOf(json.optString("subjectType", "")),
                    json.optString("subjectId", ""),
                    json.optString("albumName", ""),
                    json.optString("relativePath", ""));
            return request.isValid() ? request : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

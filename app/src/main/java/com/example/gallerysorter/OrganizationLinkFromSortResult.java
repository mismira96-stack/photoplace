package com.example.gallerysorter;

import java.util.UUID;

/** Creates a link only when the persisted worker result confirms output for one subject. */
final class OrganizationLinkFromSortResult {
    private OrganizationLinkFromSortResult() {
    }

    static OrganizationLink create(OrganizationRequest request,
                                   SortResultStore.Snapshot result,
                                   long completedAtMillis) {
        if (request == null || !request.isValid() || result == null
                || result.copiedCount <= 0 || result.sortedItems.size() != result.copiedCount
                || completedAtMillis <= 0L) {
            return null;
        }
        String expectedPath = normalizePath(request.relativePath);
        for (PhotoItem item : result.sortedItems) {
            if (item == null || !expectedPath.equals(normalizePath(item.targetRelativePath))) {
                return null;
            }
        }
        OrganizationLink.Status status = result.canceled || result.failedCount > 0
                ? OrganizationLink.Status.PARTIAL
                : OrganizationLink.Status.SUCCESS;
        return new OrganizationLink(
                "link_" + UUID.randomUUID().toString().replace("-", ""),
                request.subjectType,
                request.subjectId,
                request.requestId,
                request.albumName,
                request.relativePath,
                completedAtMillis,
                status,
                result.copiedCount,
                result.skippedCount,
                result.failedCount);
    }

    private static String normalizePath(String value) {
        String path = value == null ? "" : value.trim().replace('\\', '/');
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}

package com.example.gallerysorter;

/** Prevents moving Memory source videos before the Memory media resolver can read Gallery output. */
final class OrganizationMediaPolicy {
    private OrganizationMediaPolicy() {
    }

    static boolean shouldMoveVideos(OrganizationRequest request, boolean userPreference) {
        if (request != null && request.subjectType == OrganizationLink.SubjectType.MEMORY) {
            return false;
        }
        return userPreference;
    }

    /** Discovery refs must remain available to Memory until Gallery-output resolution exists. */
    static boolean shouldMoveDiscoveryVideos(boolean userPreference) {
        return false;
    }
}

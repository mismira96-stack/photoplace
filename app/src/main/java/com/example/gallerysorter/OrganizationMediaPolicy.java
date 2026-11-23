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
    static boolean shouldMoveDiscoveryVideos(boolean userPreference,java.util.List<MemoryRecord> records) {
        boolean manualVideo=false;
        for(MemoryRecord record:records)if(record.discoveryGroup!=null)for(DiscoveryPhotoRef ref:record.discoveryGroup.photoRefs)
            if(ref.mediaKind==MediaKind.VIDEO){if(!ref.manualAssigned)return false;manualVideo=true;}
        return userPreference&&manualVideo;
    }
    static boolean shouldMoveDiscoveryVideos(boolean userPreference,DiscoveryAlbumOrganizer.Preparation preparation) {
        boolean manualVideo=false;
        for(DiscoveryAlbumOrganizer.PreparedItem item:preparation.items)if(item.video){if(!item.manualAssigned)return false;manualVideo=true;}
        return userPreference&&manualVideo;
    }
    static boolean shouldMoveVideos(OrganizationRequest request,boolean userPreference,java.util.List<PhotoItem> items) {
        if(request==null||request.subjectType!=OrganizationLink.SubjectType.MEMORY)return userPreference;
        boolean manualVideo=false;
        for(PhotoItem item:items)if(item.video){if(!item.manualAssigned)return false;manualVideo=true;}
        return userPreference&&manualVideo;
    }
}

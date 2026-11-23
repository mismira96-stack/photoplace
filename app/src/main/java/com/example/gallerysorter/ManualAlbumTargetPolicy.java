package com.example.gallerysorter;

/** New manually named places must not silently adopt an unrelated same-name Gallery folder. */
final class ManualAlbumTargetPolicy {
    static String resolve(MemoryRecord record,OrganizePlaceService.Plan plan,DiscoveryAlbumOrganizer.AlbumLookup lookup) throws Exception {
        boolean manual=false;for(DiscoveryPhotoRef ref:plan.refs)manual|=ref.manualAssigned;
        if(!manual||record.organizedAlbum!=null)return lookup.resolveTargetRelativePath(plan.placeName,plan.targetRelativePath);
        String proposed=plan.targetRelativePath;
        if(!exists(proposed,lookup))return proposed;
        String base=proposed.substring(0,proposed.length()-1);
        for(int i=0;i<20;i++) {
            String path=base+"_"+java.util.UUID.randomUUID().toString().substring(0,8)+"/";
            if(!exists(path,lookup))return path;
        }
        throw new IllegalStateException("새 앨범 경로를 정하지 못했어요. 다시 시도해 주세요.");
    }
    private static boolean exists(String path,DiscoveryAlbumOrganizer.AlbumLookup lookup) throws Exception {
        DiscoveryAlbumOrganizer.Match photos=lookup.find(path,"",false),videos=lookup.find(path,"",true);
        if(photos==null||videos==null)throw new IllegalStateException("앨범 경로를 확인하지 못했어요.");
        return photos.albumExists||videos.albumExists;
    }
}

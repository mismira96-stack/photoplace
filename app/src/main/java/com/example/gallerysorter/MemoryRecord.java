package com.example.gallerysorter;

final class MemoryRecord {
    final String memoryKey;
    final String placeKey;
    final String title;
    final String canonicalPlaceName;
    final String displayName;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;
    final int itemCount;
    final int photoCount;
    final int videoCount;
    final long startDateMillis;
    final long endDateMillis;
    final String coverUri;
    final MemorySourceType sourceType;
    final DiscoveryMemoryGroup discoveryGroup;
    final OrganizedAlbumRef organizedAlbum;
    final int staleCount;
    final int availableCount;
    final boolean canOpenPhotos;
    final boolean canOpenGalleryAlbum;
    final boolean canOrganize;
    final boolean canAddNewItems;
    final ManualGalleryPolicy galleryPolicy;

    MemoryRecord(String memoryKey,
                 String placeKey,
                 String title,
                 String canonicalPlaceName,
                 String displayName,
                 String countryCode,
                 String countryName,
                 String adminArea,
                 String addressLine,
                 int itemCount,
                 int photoCount,
                 int videoCount,
                 long startDateMillis,
                 long endDateMillis,
                 String coverUri,
                 MemorySourceType sourceType,
                 DiscoveryMemoryGroup discoveryGroup,
                 OrganizedAlbumRef organizedAlbum,
                 int staleCount,
                 int availableCount,
                 boolean canOpenPhotos,
                 boolean canOpenGalleryAlbum,
                 boolean canOrganize,
                 boolean canAddNewItems) {
        this(memoryKey,placeKey,title,canonicalPlaceName,displayName,countryCode,countryName,adminArea,addressLine,
                itemCount,photoCount,videoCount,startDateMillis,endDateMillis,coverUri,sourceType,discoveryGroup,organizedAlbum,
                staleCount,availableCount,canOpenPhotos,canOpenGalleryAlbum,canOrganize,canAddNewItems,null);
    }
    private MemoryRecord(String memoryKey,String placeKey,String title,String canonicalPlaceName,String displayName,
                 String countryCode,String countryName,String adminArea,String addressLine,int itemCount,int photoCount,int videoCount,
                 long startDateMillis,long endDateMillis,String coverUri,MemorySourceType sourceType,DiscoveryMemoryGroup discoveryGroup,
                 OrganizedAlbumRef organizedAlbum,int staleCount,int availableCount,boolean canOpenPhotos,boolean canOpenGalleryAlbum,
                 boolean canOrganize,boolean canAddNewItems,ManualGalleryPolicy galleryPolicy) {
        this.memoryKey = clean(memoryKey);
        this.placeKey = clean(placeKey);
        this.title = clean(title);
        this.canonicalPlaceName = clean(canonicalPlaceName);
        this.displayName = clean(displayName);
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.adminArea = clean(adminArea);
        this.addressLine = clean(addressLine);
        this.itemCount = Math.max(0, itemCount);
        this.photoCount = Math.max(0, photoCount);
        this.videoCount = Math.max(0, videoCount);
        this.startDateMillis = startDateMillis;
        this.endDateMillis = endDateMillis;
        this.coverUri = clean(coverUri);
        this.discoveryGroup = discoveryGroup;
        this.organizedAlbum = organizedAlbum;
        this.sourceType = resolveSourceType(sourceType, discoveryGroup, organizedAlbum);
        this.staleCount = Math.max(0, staleCount);
        this.availableCount = Math.max(0, availableCount);
        this.canOpenPhotos = canOpenPhotos;
        this.canOpenGalleryAlbum = canOpenGalleryAlbum;
        this.canOrganize = canOrganize;
        this.canAddNewItems = canAddNewItems;
        this.galleryPolicy=galleryPolicy;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
    MemoryRecord withKnownOutput(OrganizationLink link) {
        if(organizedAlbum!=null||link==null||link.status==OrganizationLink.Status.MISSING)return this;
        return new MemoryRecord(memoryKey,placeKey,title,canonicalPlaceName,displayName,countryCode,countryName,adminArea,
                addressLine,itemCount,photoCount,videoCount,startDateMillis,endDateMillis,coverUri,MemorySourceType.MIXED,
                discoveryGroup,new OrganizedAlbumRef(link.relativePath,link.albumName,itemCount,coverUri,countryCode,
                countryName,startDateMillis,endDateMillis),staleCount,availableCount,canOpenPhotos,true,canOrganize,true,galleryPolicy);
    }
    MemoryRecord withGalleryPolicy(ManualGalleryPolicy policy) {
        return new MemoryRecord(memoryKey,placeKey,title,canonicalPlaceName,displayName,countryCode,countryName,adminArea,
                addressLine,itemCount,photoCount,videoCount,startDateMillis,endDateMillis,coverUri,sourceType,discoveryGroup,
                organizedAlbum,staleCount,availableCount,canOpenPhotos,canOpenGalleryAlbum,canOrganize,canAddNewItems,policy);
    }

    private static MemorySourceType resolveSourceType(MemorySourceType sourceType,
                                                      DiscoveryMemoryGroup discoveryGroup,
                                                      OrganizedAlbumRef organizedAlbum) {
        if (sourceType != null) {
            return sourceType;
        }
        if (discoveryGroup != null && organizedAlbum != null) {
            return MemorySourceType.MIXED;
        }
        if (organizedAlbum != null) {
            return MemorySourceType.ORGANIZED_ALBUM;
        }
        return MemorySourceType.DISCOVERED_ONLY;
    }
}

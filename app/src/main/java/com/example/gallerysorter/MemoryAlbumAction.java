package com.example.gallerysorter;

import java.util.Collections;

/** Single-memory album action; keeps additions separate from global discovery organization. */
final class MemoryAlbumAction {
    private MemoryAlbumAction() { }

    static boolean canPrepare(MemoryRecord record) {
        if (record == null || record.discoveryGroup == null) return false;
        if (!record.canOrganize && !canAdd(record)) return false;
        for (DiscoveryPhotoRef ref : record.discoveryGroup.photoRefs) {
            if (ref != null && !ref.stale && (ref.mediaKind==MediaKind.PHOTO||ref.manualAssigned)
                    && ref.sourceUri != null && !ref.sourceUri.trim().isEmpty()) return true;
        }
        return false;
    }

    /** Reuse the detail's resolved media; copied originals omitted by that projection aren't additions. */
    static boolean canPrepare(MemoryRecord record, java.util.List<DiscoveryPhotoRef> resolvedRefs) {
        if (!canPrepare(record) || resolvedRefs == null) return false;
        java.util.Set<String> originals = new java.util.HashSet<>();
        for (DiscoveryPhotoRef ref : record.discoveryGroup.photoRefs) {
            if (ref != null && !ref.stale && (ref.mediaKind==MediaKind.PHOTO||ref.manualAssigned)
                    && !ref.sourceUri.isEmpty()) originals.add(ref.sourceUri);
        }
        String target = record.organizedAlbum == null ? "" : normalizedPath(record.organizedAlbum.relativePath);
        for (DiscoveryPhotoRef ref : resolvedRefs) {
            if (ref == null || ref.stale || !(ref.mediaKind==MediaKind.PHOTO||ref.manualAssigned)
                    || !originals.contains(ref.sourceUri)) continue;
            if (!target.isEmpty() && target.equals(normalizedPath(ref.sourceRelativePath))) continue;
            return true;
        }
        return false;
    }

    private static String normalizedPath(String value) {
        String path = value == null ? "" : value.trim().replace('\\', '/');
        while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }

    static String label(MemoryRecord record) {
        return canAdd(record) ? "앨범에 추가" : "앨범 만들기";
    }

    static DiscoveryAlbumOrganizer.Preparation prepare(MemoryRecord record,
                                                       DiscoveryAlbumOrganizer.AlbumLookup lookup) throws Exception {
        if (!canPrepare(record) || lookup == null) {
            throw new IllegalStateException("이 장소에서 정리할 수 있는 사진을 찾지 못했어요.");
        }
        if (!canAdd(record)) {
            return DiscoveryAlbumOrganizer.prepare(Collections.singletonList(record), lookup);
        }
        final String target = record.organizedAlbum.relativePath.trim();
        DiscoveryAlbumOrganizer.Match photos = lookup.find(target, "", false);
        if (photos == null) throw new IllegalStateException("기존 위치 앨범을 확인하지 못했어요.");
        if (!photos.albumExists) {
            DiscoveryAlbumOrganizer.Match videos = lookup.find(target, "", true);
            if (videos == null || !videos.albumExists) {
                throw new IllegalStateException("기존 위치 앨범을 확인하지 못했어요. 사진은 변경하지 않았어요.");
            }
        }
        // A known association is authoritative; never select a different folder by its name.
        return DiscoveryAlbumOrganizer.prepare(Collections.singletonList(record),
                new DiscoveryAlbumOrganizer.AlbumLookup() {
                    @Override public String resolveTargetRelativePath(String place, String proposed) {
                        return target;
                    }
                    @Override public DiscoveryAlbumOrganizer.Match find(String path, String name,
                                                                         boolean video) throws Exception {
                        DiscoveryAlbumOrganizer.Match match = lookup.find(path, name, video);
                        if (match == null) {
                            throw new IllegalStateException("기존 위치 앨범을 확인하지 못했어요. 사진은 변경하지 않았어요.");
                        }
                        return new DiscoveryAlbumOrganizer.Match(true, match.duplicateExists);
                    }
                });
    }

    static String confirmationTitle(DiscoveryAlbumOrganizer.Preparation preparation) {
        if (hasExistingTarget(preparation)) {
            return "기존 위치 앨범에 사진을 추가할까요?";
        }
        return "이 장소를 위치 앨범으로 만들까요?";
    }

    static String confirmationLabel(DiscoveryAlbumOrganizer.Preparation preparation) {
        return hasExistingTarget(preparation) ? "사진 추가하기" : "이 장소만 만들기";
    }

    private static boolean hasExistingTarget(DiscoveryAlbumOrganizer.Preparation preparation) {
        return preparation != null && !preparation.items.isEmpty()
                && preparation.items.get(0).targetExists;
    }

    private static boolean canAdd(MemoryRecord record) {
        if (record == null || record.sourceType != MemorySourceType.MIXED
                || !record.canAddNewItems || record.organizedAlbum == null) return false;
        String path = record.organizedAlbum.relativePath.trim();
        if (!path.startsWith("Pictures/") || path.length() <= "Pictures/".length()
                || path.indexOf('\\') >= 0) return false;
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
        }
        return true;
    }
}

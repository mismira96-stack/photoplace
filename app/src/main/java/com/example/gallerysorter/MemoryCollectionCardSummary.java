package com.example.gallerysorter;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Read-only collection summary. Build on a worker; the renderer never queries Gallery. */
final class MemoryCollectionCardSummary {
    final int photoCount, videoCount;
    final String coverUri, placeNames, dateRange;
    final boolean complete;
    final String countText;

    private MemoryCollectionCardSummary(int photos, int videos, String cover, String places,
                                        String dates, boolean complete) {
        this(photos, videos, cover, places, dates, complete,
                SingleAlbumCompletionResolver.formatMediaCount(photos, videos));
    }

    private MemoryCollectionCardSummary(int photos, int videos, String cover, String places,
                                        String dates, boolean complete, String countText) {
        photoCount = photos; videoCount = videos;
        coverUri = cover; placeNames = places; dateRange = dates;
        this.complete = complete;
        this.countText = countText;
    }

    static MemoryCollectionCardSummary forCountry(int total, String cover, String places, String dates, boolean complete) {
        return new MemoryCollectionCardSummary(0, 0, cover, places, dates, complete,
                String.format(Locale.KOREA, "사진·동영상 %,d개", total));
    }

    static Map<String, MemoryCollectionCardSummary> buildAll(List<MemoryCollection> collections,
            List<MemoryRecord> records, Map<String, String> aliases, MemoryRepository repository,
            MemoryMediaResolver.GalleryReader reader) {
        Map<String, MemoryCollectionCardSummary> result = new LinkedHashMap<>();
        if (collections != null) for (MemoryCollection collection : collections) {
            if (collection != null) result.put(collection.collectionId,
                    build(collection, records, aliases, repository, reader));
        }
        return Collections.unmodifiableMap(result);
    }

    static MemoryCollectionCardSummary build(MemoryCollection collection, List<MemoryRecord> records,
                              Map<String, String> aliases,
                              MemoryRepository repository,
                              MemoryMediaResolver.GalleryReader galleryReader) {
        Set<String> memberIds = new HashSet<>();
        for (MemoryCollection.Member member : collection.members) {
            if (member != null) {
                memberIds.add(member.stableMemoryId);
            }
        }
        Set<String> names = new LinkedHashSet<>();
        Set<String> uris = new HashSet<>();
        String cover = "";
        boolean complete = true;
        Set<String> resolvedMembers = new HashSet<>();
        int photos = 0;
        int videos = 0;
        long firstDate = Long.MAX_VALUE;
        long lastDate = 0L;
        if (records != null) {
            for (MemoryRecord record : records) {
                if (record == null || aliases == null
                        || !memberIds.contains(aliases.get(record.memoryKey))) {
                    continue;
                }
                resolvedMembers.add(aliases.get(record.memoryKey));
                MemoryBrowserItem item = MemoryBrowserItem.from(record);
                if (item != null) {
                    names.add(item.title);
                }
                firstDate = Math.min(firstDate,
                        record.startDateMillis > 0L ? record.startDateMillis : Long.MAX_VALUE);
                lastDate = Math.max(lastDate, record.endDateMillis);
                List<DiscoveryPhotoRef> refs;
                if (repository == null) {
                    refs = record.discoveryGroup == null
                            ? java.util.Collections.<DiscoveryPhotoRef>emptyList()
                            : record.discoveryGroup.photoRefs;
                } else {
                    MemoryMediaResolution resolution = MemoryMediaResolver.resolve(
                            record, repository.usableMemoryLink(record), galleryReader);
                    complete &= resolution != null && resolution.source != MemoryMediaSourcePolicy.Source.UNAVAILABLE;
                    refs = resolution == null ? java.util.Collections.<DiscoveryPhotoRef>emptyList()
                            : resolution.refs;
                }
                for (DiscoveryPhotoRef ref : refs) {
                    if (ref == null || ref.stale || ref.sourceUri == null || ref.sourceUri.trim().isEmpty() || !uris.add(ref.sourceUri)) {
                        continue;
                    }
                    if (ref.mediaKind == MediaKind.VIDEO) {
                        videos++;
                    } else {
                        photos++;
                    }
                    if (cover.isEmpty()) {
                        cover = ref.sourceUri;
                    }
                }
            }
        }
        String range = "";
        if (firstDate != Long.MAX_VALUE && lastDate > 0L) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
            String startText = formatter.format(new Date(firstDate));
            String endText = formatter.format(new Date(lastDate));
            range = startText.equals(endText) ? startText : startText + " ~ " + endText;
        }
        StringBuilder placeNames = new StringBuilder();
        int shownNameCount = 0;
        for (String name : names) {
            if (shownNameCount >= 3) {
                break;
            }
            if (placeNames.length() > 0) {
                placeNames.append(" · ");
            }
            placeNames.append(name);
            shownNameCount++;
        }
        int remainingNames = Math.max(0, collection.members.size() - shownNameCount);
        if (remainingNames > 0) {
            placeNames.append(" 외 ").append(remainingNames).append("곳");
        }
        return new MemoryCollectionCardSummary(photos, videos, cover, placeNames.toString(), range,
                complete && resolvedMembers.containsAll(memberIds));
    }

}

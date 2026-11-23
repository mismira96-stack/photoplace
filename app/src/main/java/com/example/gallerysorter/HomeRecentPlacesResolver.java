package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Read-only Home projection. Build on the Home worker, not the UI thread. */
final class HomeRecentPlacesResolver {
    static final class Card {
        final String memoryKey;
        final String title;
        final String coverUri;
        final String countText;
        final boolean includeOrganizedSources;
        final boolean complete;

        Card(String memoryKey, String title, String coverUri, String countText) {
            this(memoryKey, title, coverUri, countText, true);
        }

        Card(String memoryKey, String title, String coverUri, String countText, boolean complete) {
            this.memoryKey = memoryKey;
            this.title = title;
            this.coverUri = coverUri;
            this.countText = countText;
            this.includeOrganizedSources = true;
            this.complete = complete;
        }
    }

    private HomeRecentPlacesResolver() { }

    static List<Card> build(MemoryRepository repository,
                           MemoryMediaResolver.GalleryReader galleryReader) {
        List<MemoryRecord> discovered = new ArrayList<>();
        if (repository != null) {
            for (MemoryRecord record : repository.memories()) {
                if (record.discoveryGroup != null) {
                    discovered.add(record);
                }
            }
        }
        Collections.sort(discovered, new Comparator<MemoryRecord>() {
            @Override public int compare(MemoryRecord left, MemoryRecord right) {
                int date = Long.compare(right.endDateMillis, left.endDateMillis);
                return date != 0 ? date : left.memoryKey.compareTo(right.memoryKey);
            }
        });
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Math.min(4, discovered.size()); i++) {
            MemoryRecord record = discovered.get(i);
            MemoryMediaResolution media = MemoryMediaResolver.resolve(
                    record, repository.usableMemoryLink(record), galleryReader);
            DiscoveryPhotoRef newest = null;
            DiscoveryPhotoRef newestPhoto = null;
            for (DiscoveryPhotoRef ref : media.refs) {
                if (newest == null || ref.takenAtMillis > newest.takenAtMillis) newest = ref;
                if (ref.mediaKind != MediaKind.VIDEO
                        && (newestPhoto == null || ref.takenAtMillis > newestPhoto.takenAtMillis)) {
                    newestPhoto = ref;
                }
            }
            String count = media.source == MemoryMediaSourcePolicy.Source.UNAVAILABLE
                    ? "불러오기 필요" : MemoryMediaCounts.fromRefs(media.refs).text();
            String cover = newestPhoto != null ? newestPhoto.sourceUri
                    : newest != null ? newest.sourceUri : record.coverUri;
            cards.add(new Card(record.memoryKey, MemoryBrowserItem.from(record).title,
                    cover, count, media.source != MemoryMediaSourcePolicy.Source.UNAVAILABLE));
        }
        return Collections.unmodifiableList(cards);
    }

}

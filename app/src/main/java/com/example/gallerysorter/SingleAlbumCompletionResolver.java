package com.example.gallerysorter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Builds a compact result only when the completed work produced one album. */
final class SingleAlbumCompletionResolver {
    static final class Item {
        final String groupId;
        final String albumName;
        final Date takenAt;
        final String uri;
        final boolean video;

        Item(String groupId, String albumName, Date takenAt, String uri, boolean video) {
            this.groupId = groupId == null ? "" : groupId;
            this.albumName = albumName == null ? "" : albumName;
            this.takenAt = takenAt;
            this.uri = uri == null ? "" : uri;
            this.video = video;
        }
    }

    static final class Summary {
        final String albumName;
        final int itemCount;
        final int photoCount;
        final int videoCount;
        final String dateRange;
        final String coverUri;

        Summary(String albumName, int itemCount, int photoCount, int videoCount,
                String dateRange, String coverUri) {
            this.albumName = albumName;
            this.itemCount = itemCount;
            this.photoCount = photoCount;
            this.videoCount = videoCount;
            this.dateRange = dateRange;
            this.coverUri = coverUri;
        }

        String mediaCountText() {
            return formatMediaCount(photoCount, videoCount);
        }
    }

    Summary resolve(List<Item> items) {
        String groupId = null;
        String albumName = "";
        String coverUri = "";
        String fallbackCoverUri = "";
        Date start = null;
        Date end = null;
        int count = 0;
        int photoCount = 0;
        int videoCount = 0;

        if (items == null) {
            return null;
        }
        for (Item item : items) {
            if (item == null || item.groupId.isEmpty()) {
                continue;
            }
            if (groupId == null) {
                groupId = item.groupId;
                albumName = item.albumName;
            } else if (!groupId.equals(item.groupId)) {
                return null;
            }
            count++;
            if (item.video) {
                videoCount++;
            } else {
                photoCount++;
            }
            if (!item.uri.isEmpty()) {
                if (item.video && fallbackCoverUri.isEmpty()) {
                    fallbackCoverUri = item.uri;
                } else if (!item.video && coverUri.isEmpty()) {
                    coverUri = item.uri;
                }
            }
            if (item.takenAt != null) {
                if (start == null || item.takenAt.before(start)) {
                    start = item.takenAt;
                }
                if (end == null || item.takenAt.after(end)) {
                    end = item.takenAt;
                }
            }
        }
        if (count == 0) {
            return null;
        }
        String dateRange = formatDateRange(start, end);
        return new Summary(albumName, count, photoCount, videoCount, dateRange,
                coverUri.isEmpty() ? fallbackCoverUri : coverUri);
    }

    static String formatMediaCount(int photoCount, int videoCount) {
        int photos = Math.max(0, photoCount);
        int videos = Math.max(0, videoCount);
        if (photos > 0 && videos == 0) {
            return "사진 " + photos + "장";
        }
        if (videos > 0 && photos == 0) {
            return "동영상 " + videos + "개";
        }
        if (photos > 0) {
            return "사진 " + photos + "장 · 동영상 " + videos + "개";
        }
        return "미디어 0개";
    }

    private String formatDateRange(Date start, Date end) {
        if (start == null || end == null) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
        String first = format.format(start);
        String last = format.format(end);
        return first.equals(last) ? first : first + " ~ " + last;
    }
}

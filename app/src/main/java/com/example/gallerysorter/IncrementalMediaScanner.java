package com.example.gallerysorter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Lightweight inventory reconciliation; expensive metadata reads only for changed rows. */
final class IncrementalMediaScanner {
    interface MetadataReader {
        LocationResult read(Uri uri, String name, long modified, long added, long taken,
                            Double latitude, Double longitude, boolean video);
    }

    private final Context context;
    private final MediaAnalysisStore store;
    private final MetadataReader reader;
    private final MediaScanProgressListener progress;
    private final BooleanSupplier cancelled;
    private final Handler main = new Handler(Looper.getMainLooper());

    IncrementalMediaScanner(Context context, MediaAnalysisStore store, MetadataReader reader,
                            MediaScanProgressListener progress, BooleanSupplier cancelled) {
        this.context = context;
        this.store = store;
        this.reader = reader;
        this.progress = progress;
        this.cancelled = cancelled;
    }

    List<PhotoItem> scan(List<AlbumFolder> albums, List<String> paths) throws Exception {
        long started = SystemClock.elapsedRealtime();
        IncrementalAnalysisSession session = new IncrementalAnalysisSession(store.readAll());
        ArrayList<PhotoItem> items = new ArrayList<>();
        List<String> scanPaths = ManualPlacePaths.normalizeAll(paths);
        UnresolvedMediaCapture unresolved = new UnresolvedMediaCapture(context);
        unresolved.begin();
        try {
        MediaStoreAlbumLookup targets = new MediaStoreAlbumLookup(context.getContentResolver());
        String version = MediaStore.getVersion(context);
        readKind(false, albums, scanPaths, targets, version, session, items, unresolved);
        if (cancelled.getAsBoolean()) return items;
        readKind(true, albums, scanPaths, targets, version, session, items, unresolved);
        if (cancelled.getAsBoolean()) return items;
        if (!session.finish(true, store::saveAll)) throw new IllegalStateException("분석 결과를 저장하지 못했어요");
        unresolved.finish(scanPaths);
        items.sort(Comparator.comparingLong((PhotoItem item) ->
                item.takenAt == null ? 0L : item.takenAt.getTime()).reversed());
        Log.i("PhotoPlaceRefresh", "inventoryMs=" + (SystemClock.elapsedRealtime() - started)
                + " items=" + items.size() + " cached=" + session.hits + " analyzed=" + session.analyzed);
        return items;
        } finally { unresolved.close(); }
    }

    private void readKind(boolean video, List<AlbumFolder> albums, List<String> paths,
                          MediaStoreAlbumLookup targets, String version,
                          IncrementalAnalysisSession session, List<PhotoItem> items,
                          UnresolvedMediaCapture unresolved) throws Exception {
        if (paths.isEmpty()) return;
        Uri collection = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ArrayList<String> projection = new ArrayList<>(java.util.Arrays.asList("_id", "_display_name",
                "mime_type", "date_modified", "date_added", "datetaken", "latitude", "longitude",
                "relative_path", "_size", "volume_name"));
        if (video) projection.add("duration");
        if (Build.VERSION.SDK_INT >= 30) projection.add("generation_modified");
        String selection = "relative_path COLLATE BINARY IN (" + String.join(",", java.util.Collections.nCopies(paths.size(), "?"))
                + ") AND is_pending=0" + (Build.VERSION.SDK_INT >= 30 ? " AND is_trashed=0" : "");
        try (Cursor cursor = context.getContentResolver().query(collection, projection.toArray(new String[0]),
                selection, paths.toArray(new String[0]), "date_modified DESC")) {
            if (cursor == null) throw new IllegalStateException("사진 목록을 확인하지 못했어요");
            int total = cursor.getCount();
            int current = 0;
            while (cursor.moveToNext()) {
                if (cancelled.getAsBoolean()) return;
                Uri uri = Uri.withAppendedPath(collection, String.valueOf(number(cursor, "_id")));
                String rawName = text(cursor, "_display_name");
                String name = rawName.isEmpty() ? "media_" + number(cursor, "_id") : rawName.replace('/', '_');
                long modified = number(cursor, "date_modified");
                long added = number(cursor, "date_added");
                long taken = number(cursor, "datetaken");
                Double latitude = coordinate(cursor, "latitude");
                Double longitude = coordinate(cursor, "longitude");
                boolean gps = latitude != null && longitude != null
                        && (Math.abs(latitude) > 1.0E-4 || Math.abs(longitude) > 1.0E-4);
                String signature = MediaAnalysisSignature.build(uri, name, modified, added, taken,
                        video, text(cursor, "relative_path"));
                String fingerprint = version + ":" + number(cursor, "_size") + ":" + number(cursor, "duration")
                        + ":" + number(cursor, "generation_modified") + ":" + latitude + ":" + longitude;
                LocationResult location = session.resolve(signature, fingerprint, gps,
                        () -> reader.read(uri, name, modified, added, taken, latitude, longitude, video));
                PhotoItem item = PreviewItemProjection.build(uri, name, text(cursor, "mime_type"),
                        location, video, albums, targets);
                items.add(item);
                unresolved.observe(item, text(cursor, "volume_name"), text(cursor, "relative_path"),
                        number(cursor, "_id"), number(cursor, "_size"), modified);
                current++;
                if (progress != null && (current == 1 || current % 50 == 0 || current == total)) {
                    final int count = current;
                    main.post(() -> progress.onItemScanned(video, count, total, count, total, item));
                }
            }
        }
    }

    private static long number(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? 0L : cursor.getLong(index);
    }
    private static String text(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? "" : cursor.getString(index);
    }
    private static Double coordinate(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? null : cursor.getDouble(index);
    }
}

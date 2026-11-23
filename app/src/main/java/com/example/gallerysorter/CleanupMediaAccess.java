package com.example.gallerysorter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** MediaStore content-URI adapter. Access failure stays unknown; no raw file path guessing. */
final class CleanupMediaAccess implements CleanupExecutionAudit.Access {
    private final Context context;
    CleanupMediaAccess(Context context) { this.context = context.getApplicationContext(); }
    private android.os.Bundle queryArgs() {
        android.os.Bundle args = new android.os.Bundle();
        if (Build.VERSION.SDK_INT >= 30) args.putInt(android.provider.MediaStore.QUERY_ARG_MATCH_TRASHED,
                android.provider.MediaStore.MATCH_INCLUDE);
        return args;
    }
    @Override public String key(String value) throws Exception {
        try (Cursor c = context.getContentResolver().query(Uri.parse(value),
                new String[]{"_id", "volume_name"}, queryArgs(), null)) {
            if (c == null || !c.moveToFirst() || c.isNull(0) || c.isNull(1))
                throw new IllegalStateException("Media identity unavailable");
            String volume = c.getString(1);
            if (Build.VERSION.SDK_INT < 30) throw new IllegalStateException("Android 11 required");
            String version = android.provider.MediaStore.getVersion(context, volume);
            if (version == null || version.isEmpty()) throw new IllegalStateException("Provider version unavailable");
            return version + ":" + volume + ":" + c.getLong(0);
        }
    }
    @Override public boolean trashed(String value) throws Exception {
        if (Build.VERSION.SDK_INT < 30) throw new IllegalStateException("Android 11 required");
        try (Cursor c = context.getContentResolver().query(Uri.parse(value), new String[]{"is_trashed"}, queryArgs(), null)) {
            if (c == null || !c.moveToFirst() || c.isNull(0)) throw new IllegalStateException("Trash state unavailable");
            return c.getInt(0) == 1;
        }
    }
    @Override public InputStream open(String uri) throws Exception {
        // Exact underlying bytes are mandatory; never certify an EXIF-redacted substitute.
        return context.getContentResolver().openInputStream(android.provider.MediaStore.setRequireOriginal(Uri.parse(uri)));
    }
    @Override public CleanupPairVerifier.Metadata metadata(String value) throws Exception {
        List<String> columns = new ArrayList<>(Arrays.asList("_id", "_size", "mime_type", "relative_path",
                "date_modified", "datetaken", "is_pending", "volume_name"));
        if (Build.VERSION.SDK_INT >= 30) columns.addAll(Arrays.asList("is_trashed", "generation_modified"));
        try (Cursor cursor = context.getContentResolver().query(Uri.parse(value), columns.toArray(new String[0]),
                queryArgs(), null)) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            StringBuilder identity = new StringBuilder(value);
            for (String column : columns) {
                int i = cursor.getColumnIndexOrThrow(column);
                identity.append('|').append(column).append('=').append(
                        identityValue(column, cursor.isNull(i) ? null : cursor.getString(i)));
            }
            String mime = cursor.getString(cursor.getColumnIndexOrThrow("mime_type"));
            boolean photo = mime != null && mime.startsWith("image/")
                    && cursor.getInt(cursor.getColumnIndexOrThrow("is_pending")) == 0
                    && (Build.VERSION.SDK_INT < 30 || cursor.getInt(cursor.getColumnIndexOrThrow("is_trashed")) == 0);
            return new CleanupPairVerifier.Metadata(cursor.getLong(cursor.getColumnIndexOrThrow("_size")),
                    identity.toString(), cursor.getString(cursor.getColumnIndexOrThrow("relative_path")), photo);
        }
    }

    static String identityValue(String column, String value) {
        if (value != null) return value;
        if ("datetaken".equals(column)) return "<unknown>";
        throw new IllegalStateException("Unknown required media metadata");
    }
}

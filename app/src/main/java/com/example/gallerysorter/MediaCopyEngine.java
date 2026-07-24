package com.example.gallerysorter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class MediaCopyEngine {
    private static final int MIN_VALID_TAKEN_YEAR = 2000;
    private static final int MAX_VALID_TAKEN_YEAR = 2035;

    private final Context context;

    MediaCopyEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    Uri copy(PhotoItem item) throws Exception {
        if (item.video) {
            return moveVideoItem(item);
        }
        ContentValues values = new ContentValues();
        values.put("_display_name", safeName(item.name));
        values.put("mime_type", item.mimeType == null ? "image/jpeg" : item.mimeType);
        values.put("relative_path", item.targetRelativePath);
        putMediaOwner(values);
        putMediaDates(values, item.takenAt);
        values.put("is_pending", (Integer) 1);
        ContentResolver resolver = context.getContentResolver();
        Uri insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (insertedUri == null) {
            throw new IllegalStateException("사진 복사 위치를 만들 수 없습니다.");
        }
        try (InputStream input = resolver.openInputStream(item.uri);
             OutputStream output = resolver.openOutputStream(insertedUri)) {
            if (input == null || output == null) {
                throw new IllegalStateException("사진을 열 수 없습니다.");
            }
            byte[] buffer = new byte[65536];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }

        ContentValues doneValues = new ContentValues();
        doneValues.put("is_pending", (Integer) 0);
        putMediaOwner(doneValues);
        putMediaDates(doneValues, item.takenAt);
        resolver.update(insertedUri, doneValues, null, null);
        setCopiedFileModifiedTime(insertedUri, item.takenAt);
        return insertedUri;
    }

    private Uri moveVideoItem(PhotoItem item) throws Exception {
        ContentValues values = new ContentValues();
        values.put("relative_path", item.targetRelativePath);
        values.put("_display_name", safeName(item.name));
        putMediaOwner(values);
        if (item.mimeType != null) {
            values.put("mime_type", item.mimeType);
        }
        if (context.getContentResolver().update(item.uri, values, null, null) <= 0) {
            throw new IllegalStateException("동영상을 이동할 수 없습니다.");
        }
        return item.uri;
    }

    private void putMediaDates(ContentValues values, Date date) {
        Date safeDate = sanitizeTakenAt(date);
        if (safeDate == null) {
            return;
        }
        long time = safeDate.getTime();
        values.put("datetaken", Long.valueOf(time));
        long seconds = time / 1000L;
        values.put("date_added", Long.valueOf(seconds));
        values.put("date_modified", Long.valueOf(seconds));
    }

    private void setCopiedFileModifiedTime(Uri uri, Date takenAt) {
        Date safeDate = sanitizeTakenAt(takenAt);
        if (uri == null || safeDate == null) {
            return;
        }
        String path = resolveFilePathFromMediaUri(uri);
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        try {
            File file = new File(path);
            if (file.exists()) {
                file.setLastModified(safeDate.getTime());
                MediaScannerConnection.scanFile(context, new String[]{path}, null, null);
            }
        } catch (Exception ignored) {
        }
    }

    private String resolveFilePathFromMediaUri(Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{"relative_path", "_display_name"}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int pathIndex = cursor.getColumnIndex("relative_path");
                int nameIndex = cursor.getColumnIndex("_display_name");
                if (pathIndex >= 0 && nameIndex >= 0) {
                    String relativePath = cursor.getString(pathIndex);
                    String displayName = cursor.getString(nameIndex);
                    if (relativePath != null && displayName != null) {
                        return new File(Environment.getExternalStorageDirectory(), relativePath + displayName).getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Date sanitizeTakenAt(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance(Locale.KOREA);
        calendar.setTime(date);
        int year = calendar.get(1);
        if (year < MIN_VALID_TAKEN_YEAR || year > MAX_VALID_TAKEN_YEAR) {
            return null;
        }
        return date;
    }

    private void putMediaOwner(ContentValues values) {
        values.putNull("owner_package_name");
    }

    private String safeName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "photo_" + System.currentTimeMillis() + ".jpg";
        }
        return value.replace("/", "_");
    }
}

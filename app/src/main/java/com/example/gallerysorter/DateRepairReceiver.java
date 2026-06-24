package com.example.gallerysorter;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateRepairReceiver extends BroadcastReceiver {
    private static final String TAG = "AlbumDateRepair";

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pending = goAsync();
        new Thread(() -> {
            int scanned = 0;
            int updated = 0;
            int skipped = 0;
            int failed = 0;

            try {
                ContentResolver resolver = context.getContentResolver();
                Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String[] projection = new String[]{
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.RELATIVE_PATH,
                        MediaStore.Images.Media.DATE_TAKEN,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.DATE_MODIFIED
                };
                String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
                String[] selectionArgs = new String[]{"Pictures/%에서/%"};

                try (Cursor cursor = resolver.query(collection, projection, selection, selectionArgs, null)) {
                    if (cursor == null) {
                        return;
                    }
                    int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    int takenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                    while (cursor.moveToNext()) {
                        scanned++;
                        long id = cursor.getLong(idIndex);
                        String name = cursor.getString(nameIndex);
                        Date parsed = parseDateFromName(name);
                        if (parsed == null) {
                            skipped++;
                            continue;
                        }

                        long millis = parsed.getTime();
                        long currentTaken = readLong(cursor, takenIndex);

                        if (sameDayMillis(currentTaken, millis)) {
                            skipped++;
                            continue;
                        }

                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.DATE_TAKEN, millis);
                        Uri itemUri = ContentUris.withAppendedId(collection, id);
                        int result = resolver.update(itemUri, values, null, null);
                        if (result > 0) {
                            updated++;
                        } else {
                            failed++;
                            Log.w(TAG, "No rows updated for " + name + " / " + itemUri);
                        }
                    }
                }
            } catch (Exception e) {
                failed++;
                Log.e(TAG, "Date repair failed", e);
            } finally {
                String message = "날짜 복구 완료: 확인 " + scanned + "개, 수정 " + updated + "개, 건너뜀 " + skipped + "개, 실패 " + failed + "개";
                Log.i(TAG, message);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                pending.finish();
            }
        }).start();
    }

    private long readLong(Cursor cursor, int index) {
        if (index < 0 || cursor.isNull(index)) {
            return 0L;
        }
        return cursor.getLong(index);
    }

    private boolean sameDayMillis(long leftMillis, long rightMillis) {
        if (leftMillis <= 0 || rightMillis <= 0) {
            return false;
        }
        Calendar left = Calendar.getInstance(Locale.KOREA);
        Calendar right = Calendar.getInstance(Locale.KOREA);
        left.setTimeInMillis(leftMillis);
        right.setTimeInMillis(rightMillis);
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    private Date parseDateFromName(String name) {
        if (name == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("((?:19|20)\\d{2})[-_.]?(0[1-9]|1[0-2])[-_.]?([0-2]\\d|3[01])(?:[_\\-. ]?([01]\\d|2[0-3])([0-5]\\d)([0-5]\\d)?)?")
                .matcher(name);
        if (!matcher.find()) {
            return null;
        }

        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            int hour = matcher.group(4) == null ? 12 : Integer.parseInt(matcher.group(4));
            int minute = matcher.group(5) == null ? 0 : Integer.parseInt(matcher.group(5));
            int second = matcher.group(6) == null ? 0 : Integer.parseInt(matcher.group(6));

            Calendar calendar = Calendar.getInstance(Locale.KOREA);
            calendar.setLenient(false);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, second);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTime();
        } catch (Exception ignored) {
            return null;
        }
    }
}

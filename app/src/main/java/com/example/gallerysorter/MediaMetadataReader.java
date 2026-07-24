package com.example.gallerysorter;

import android.content.Context;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class MediaMetadataReader {
    private static final int MIN_VALID_TAKEN_YEAR = 2000;
    private static final int MAX_VALID_TAKEN_YEAR = 2035;

    private final Context context;

    MediaMetadataReader(Context context) {
        this.context = context.getApplicationContext();
    }

    ExifReadResult readExifData(Uri uri) {
        ExifReadResult result = new ExifReadResult();
        try (ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (descriptor != null) {
                readExifIntoResult(new ExifInterface(descriptor.getFileDescriptor()), result);
                if (hasUsableCoordinates(result.latitude, result.longitude)) {
                    return result;
                }
            }
        } catch (Exception ignored) {
        }
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input != null) {
                readExifIntoResult(new ExifInterface(input), result);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    VideoMetadataResult readVideoMetadata(Uri uri) {
        VideoMetadataResult result = new VideoMetadataResult();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            double[] location = parseIso6709Location(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION));
            if (location != null) {
                result.latitude = location[0];
                result.longitude = location[1];
            }
            Date videoDate = parseVideoDate(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
            if (videoDate != null) {
                result.takenAt = videoDate;
            }
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private void readExifIntoResult(ExifInterface exif, ExifReadResult result) throws Exception {
        String attribute = exif.getAttribute("DateTimeOriginal");
        if (attribute == null) {
            attribute = exif.getAttribute("DateTime");
        }
        if (attribute != null && result.takenAt == null) {
            result.takenAt = sanitizeTakenAt(new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(attribute));
        }
        float[] latLong = new float[2];
        if (exif.getLatLong(latLong) && hasUsableCoordinates(latLong[0], latLong[1])) {
            result.latitude = latLong[0];
            result.longitude = latLong[1];
        }
    }

    private double[] parseIso6709Location(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int splitIndex = -1;
        for (int i = 1; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '+' || c == '-') {
                splitIndex = i;
                break;
            }
        }
        if (splitIndex <= 0 || splitIndex >= trimmed.length() - 1) {
            return null;
        }
        try {
            double latitude = Double.parseDouble(trimmed.substring(0, splitIndex));
            double longitude = Double.parseDouble(trimmed.substring(splitIndex));
            if (hasUsableCoordinates(latitude, longitude)) {
                return new double[]{latitude, longitude};
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Date parseVideoDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        String[] patterns = {"yyyyMMdd'T'HHmmss.SSS'Z'", "yyyyMMdd'T'HHmmss'Z'", "yyyy:MM:dd HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                return sanitizeTakenAt(new SimpleDateFormat(pattern, Locale.US).parse(trimmed));
            } catch (Exception ignored) {
            }
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

    private boolean hasUsableCoordinates(double latitude, double longitude) {
        return Math.abs(latitude) > 1.0E-4d || Math.abs(longitude) > 1.0E-4d;
    }
}

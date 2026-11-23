package com.example.gallerysorter;

import android.content.Context;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

/** Reads representative GPS metadata only. No image decode, network Geocoder or media writes. */
final class RecoveredAlbumGeoReader implements RecoveredAlbumLinkResolver.GeoReader {
    private final Context appContext;
    private final android.content.ContentResolver resolver;
    private final PersonalPlaceGpsCacheStore cacheStore;

    RecoveredAlbumGeoReader(Context context){
        appContext=context.getApplicationContext();
        resolver=appContext.getContentResolver();
        cacheStore=new PersonalPlaceGpsCacheStore(appContext);
    }

    @Override public RecoveredAlbumLinkResolver.Point read(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return null;
        }
        try {
            PersonalPlaceGpsPoint cached = cacheStore.get(uri, "");
            if (cached != null && cached.hasCoordinates()
                    && PersonalPlace.isUsableCoordinate(cached.latitude, cached.longitude)) {
                return new RecoveredAlbumLinkResolver.Point(cached.latitude, cached.longitude);
            }
        } catch (Exception ignored) {
        }
        try {
            Uri original=MediaStore.setRequireOriginal(Uri.parse(uri));
            try(ParcelFileDescriptor file=resolver.openFileDescriptor(original,"r")) {
                if(file!=null) {
                    ExifInterface exif=new ExifInterface(file.getFileDescriptor());
                    float[] gps=new float[2];
                    if(exif.getLatLong(gps) && PersonalPlace.isUsableCoordinate(gps[0],gps[1])) {
                        return new RecoveredAlbumLinkResolver.Point(gps[0],gps[1]);
                    }
                    double[] linked=QuickBeautySourceLink.resolveCoordinates(
                            appContext,exif,new PersonalPlaceGpsReader(appContext).hasMediaLocationPermission());
                    if (linked != null && PersonalPlace.isUsableCoordinate(linked[0], linked[1])) {
                        return new RecoveredAlbumLinkResolver.Point(linked[0], linked[1]);
                    }
                }
            }
        } catch(Exception unavailable){
        }
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(appContext, Uri.parse(uri));
                String location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
                double[] parsed = parseIso6709(location);
                if (parsed != null && PersonalPlace.isUsableCoordinate(parsed[0], parsed[1])) {
                    return new RecoveredAlbumLinkResolver.Point(parsed[0], parsed[1]);
                }
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static double[] parseIso6709(String value) {
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
            double lat = Double.parseDouble(trimmed.substring(0, splitIndex));
            double lng = Double.parseDouble(trimmed.substring(splitIndex));
            if (PersonalPlace.isUsableCoordinate(lat, lng)) {
                return new double[]{lat, lng};
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

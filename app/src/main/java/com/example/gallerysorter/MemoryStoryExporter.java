package com.example.gallerysorter;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Size;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only Instagram Story export and launch pipeline.
 * - Decodes sampled bitmaps safely without OOM
 * - Writes temporary 9:16 Story PNG/JPEG into cacheDir/stories/
 * - Cleans up stale cache files automatically
 * - Launches com.instagram.share.ADD_TO_STORY via FileProvider, with safe chooser fallback
 */
final class MemoryStoryExporter {
    static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    static final String ACTION_ADD_TO_STORY = "com.instagram.share.ADD_TO_STORY";
    static final String CACHE_DIR_NAME = "stories";
    private static final long STALE_FILE_AGE_MS = 60L * 60L * 1000L; // 1 hour
    private static final int MAX_CACHED_FILES = 6;

    private MemoryStoryExporter() {
    }

    static List<Bitmap> decodeBitmaps(Context context, List<String> sourceUris, int maxSizePx) {
        List<Bitmap> list = new ArrayList<>();
        if (context == null || sourceUris == null) {
            return list;
        }
        ContentResolver resolver = context.getContentResolver();
        int limit = Math.min(MemoryStoryComposer.MAX_STRIP_PHOTOS, sourceUris.size());
        for (int i = 0; i < limit; i++) {
            String raw = sourceUris.get(i);
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            try {
                Bitmap bmp = decodeSingle(resolver, Uri.parse(raw), maxSizePx);
                if (bmp != null) {
                    list.add(bmp);
                }
            } catch (Throwable ignored) {
            }
        }
        return list;
    }

    static Bitmap decodeSingle(ContentResolver resolver, Uri uri, int maxSizePx) {
        return decodeSingle(resolver, uri, Math.max(256, maxSizePx), 1600);
    }

    private static Bitmap decodeSingle(ContentResolver resolver, Uri uri, int maxSizePx, int limit) {
        if (resolver == null || uri == null) {
            return null;
        }
        int size = Math.max(1, Math.min(limit, maxSizePx));
        // Preserve original image aspect ratio and EXIF orientation for user-controlled framing.
        try{return android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(resolver,uri),(decoder,info,source)->{
            android.util.Size original=info.getSize();float scale=Math.min(1f,size/(float)Math.max(original.getWidth(),original.getHeight()));
            decoder.setTargetSize(Math.max(1,Math.round(original.getWidth()*scale)),Math.max(1,Math.round(original.getHeight()*scale)));decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
        });}catch(Exception|OutOfMemoryError notAnImage){/* Preserve existing video/unavailable-image fallbacks. */}

        try {
            Bitmap thumb = resolver.loadThumbnail(uri, new Size(size, size), new CancellationSignal());
            if (thumb != null) {
                return thumb;
            }
        } catch (Throwable ignored) {
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in != null) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
        } catch (Throwable ignored) {
        }

        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / opts.inSampleSize > size) {
                opts.inSampleSize *= 2;
            }
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream in = resolver.openInputStream(uri)) {
                Bitmap decoded = in == null ? null : BitmapFactory.decodeStream(in, null, opts);
                if (decoded != null) {
                    return decoded;
                }
            } catch (OutOfMemoryError oom) {
                opts.inSampleSize = Math.max(2, opts.inSampleSize * 2);
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                try (InputStream retry = resolver.openInputStream(uri)) {
                    Bitmap fallback = retry == null ? null : BitmapFactory.decodeStream(retry, null, opts);
                    if (fallback != null) {
                        return fallback;
                    }
                } catch (Throwable ignored) {
                }
            } catch (Throwable ignored) {
            }
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r")) {
            if (pfd == null) {
                return null;
            }
            retriever.setDataSource(pfd.getFileDescriptor());
            if (Build.VERSION.SDK_INT >= 27) {
                Bitmap frame = retriever.getScaledFrameAtTime(
                        -1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, size, size);
                if (frame != null) {
                    return frame;
                }
            }
            return retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {
            }
        }
    }

    static List<Bitmap> decodeForExport(Context context, List<String> uris, MemoryStoryConfig config) {
        List<Bitmap> photos = new ArrayList<>();
        List<Bitmap> probes = decodeBitmaps(context, uris, 512);
        try {
            if (probes.isEmpty() || probes.size() != uris.size()) throw new IllegalStateException("사진을 불러오지 못했어요.");
            int count = probes.size();
            int[] widths = new int[count];
            int[] heights = new int[count];
            for (int i = 0; i < count; i++) {
                widths[i] = probes.get(i).getWidth();
                heights[i] = probes.get(i).getHeight();
            }
            boolean strip = config.layoutMode != MemoryStoryConfig.LayoutMode.SINGLE_FRAME && count >= 2;
            List<MemoryStoryComposer.Box> slots = strip
                    ? MemoryStoryComposer.computeStripLayout(widths, heights, config).slots
                    : java.util.Collections.singletonList(
                            MemoryStoryComposer.computeSinglePhotoBox(widths[0], heights[0], config));
            for (Bitmap probe : probes) probe.recycle();
            for (int i = 0; i < count; i++) {
                // Probe dimensions supply aspect ratio; the decoder obtains the actual original dimensions.
                int edge = MemoryStoryExportQuality.decodeEdge(widths[i], heights[i],
                        slots.get(Math.min(i, slots.size() - 1)), config.photoEdits.get(i).zoom, count);
                Bitmap photo = decodeSingle(context.getContentResolver(), Uri.parse(uris.get(i)), edge, 4096);
                if (photo == null) throw new IllegalStateException("사진을 불러오지 못했어요.");
                photos.add(photo);
            }
            return photos;
        } catch (RuntimeException | OutOfMemoryError failure) {
            for (Bitmap photo : photos) photo.recycle();
            throw new IllegalStateException("사진을 불러올 메모리가 부족하거나 사진을 읽지 못했어요.", failure);
        } finally {
            for (Bitmap probe : probes) if (!probe.isRecycled()) probe.recycle();
        }
    }

    static void cleanupStaleFiles(File directory, long nowMillis) {
        if (directory == null || !directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File f : files) {
            if (f != null && f.isFile() && nowMillis - f.lastModified() > STALE_FILE_AGE_MS) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        File[] remaining = directory.listFiles();
        if (remaining != null && remaining.length > MAX_CACHED_FILES) {
            Arrays.sort(remaining, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return Long.compare(a.lastModified(), b.lastModified());
                }
            });
            int toDelete = remaining.length - MAX_CACHED_FILES;
            for (int i = 0; i < toDelete; i++) {
                //noinspection ResultOfMethodCallIgnored
                remaining[i].delete();
            }
        }
    }

    static Uri exportToCacheUri(Context context, Bitmap storyBitmap) throws Exception {
        File dir = new File(context.getCacheDir(), CACHE_DIR_NAME);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        cleanupStaleFiles(dir, System.currentTimeMillis());
        File file = File.createTempFile("story-", ".png", dir);
        try (FileOutputStream out = new FileOutputStream(file)) {
            if (!storyBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IllegalStateException("스토리 이미지를 생성하지 못했어요.");
            }
            out.flush();
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".storyfiles", file);
    }

    static void launchInstagramStory(Context context, Uri storyUri) {
        if (context == null || storyUri == null) {
            return;
        }
        Intent story = instagramStoryIntent(storyUri);
        try {
            context.grantUriPermission(INSTAGRAM_PACKAGE, storyUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(story);
        } catch (ActivityNotFoundException | SecurityException e) {
            launchFallbackChooser(context, storyUri);
        }
    }

    /** A card sticker, with no 9:16 background asset attached. */
    static Intent instagramStoryIntent(Uri storyUri) {
        Intent story = new Intent(ACTION_ADD_TO_STORY);
        story.setPackage(INSTAGRAM_PACKAGE);
        story.setType("image/png");
        story.putExtra("interactive_asset_uri", storyUri);
        story.setClipData(ClipData.newRawUri("Story", storyUri));
        story.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return story;
    }

    private static void launchFallbackChooser(Context context, Uri storyUri) {
        Toast.makeText(context, "Instagram이 설치되어 있지 않아 공유 창을 열어요.", Toast.LENGTH_SHORT).show();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/png");
        send.putExtra(Intent.EXTRA_STREAM, storyUri);
        send.setClipData(ClipData.newRawUri("Story", storyUri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(send, "공유");
        context.startActivity(chooser);
    }
}

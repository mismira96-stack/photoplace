package com.example.gallerysorter;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.util.LruCache;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * Viewer decoding never waits behind the detail grid's thumbnail queue.
 * Decodes photos in ARGB_8888 within a strict <=2048px viewport bound, extracts video poster frames
 * via MediaMetadataRetriever when MediaStore thumbnails are unavailable, and prefetches adjacent items.
 */
final class MemoryViewerImageLoader {
    private static final int MAX_VIEWPORT_PX = 2048;

    private final ContentResolver resolver;
    private final LruCache<String, Bitmap> thumbnails;
    private final Supplier<Drawable> placeholder;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LatestImageLoadQueue<Bitmap> requests = new LatestImageLoadQueue<>(worker, main::post);
    private final LruCache<String, Bitmap> images = new LruCache<String, Bitmap>(computeCacheBudgetBytes()) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private ImageView active;
    private Object token;
    private boolean closed;

    private static int computeCacheBudgetBytes() {
        long maxHeap = Runtime.getRuntime().maxMemory();
        long eighth = maxHeap > 0 ? maxHeap / 8L : 24L * 1024L * 1024L;
        return (int) Math.max(16L * 1024L * 1024L, Math.min(32L * 1024L * 1024L, eighth));
    }

    MemoryViewerImageLoader(ContentResolver resolver, LruCache<String, Bitmap> thumbnails,
                            Supplier<Drawable> placeholder) {
        this.resolver = resolver;
        this.thumbnails = thumbnails;
        this.placeholder = placeholder;
    }

    void load(ImageView target, Uri uri, int requestedSize) {
        load(target, uri, requestedSize, null);
    }

    void load(ImageView target, Uri uri, int requestedSize, Runnable onLoaded) {
        if (closed) return;
        if (active != target) {
            if (active != null) active.removeOnAttachStateChangeListener(detachment);
            active = target;
            target.addOnAttachStateChangeListener(detachment);
        }
        requests.cancel();
        int size = clampSize(requestedSize);
        String key = uri + "#" + size;
        Object current = new Object();
        token = current;
        target.setContentDescription("Memory 사진");
        Bitmap cached = images.get(key);
        if (cached != null) {
            target.setImageBitmap(cached);
            if (onLoaded != null) onLoaded.run();
            return;
        }
        Bitmap preview = preview(uri.toString());
        ViewerImageTransition<Bitmap> transition = new ViewerImageTransition<>(
                target.getDrawable() instanceof BitmapDrawable, preview, target::setImageBitmap,
                () -> target.setImageDrawable(placeholder.get()));
        transition.pending();
        CancellationSignal cancellation = new CancellationSignal();
        long started = SystemClock.elapsedRealtime();
        try {
            requests.load(() -> decode(uri, size, cancellation), cancellation::cancel, bitmap -> {
                if (closed || token != current || active != target) return;
                if (bitmap != null) {
                    images.put(key, bitmap);
                }
                transition.ready(bitmap);
                if (bitmap == null && preview == null) target.setContentDescription("사진을 불러오지 못했어요");
                Log.i("PhotoPlaceViewer", "decodeAndQueueMs=" + (SystemClock.elapsedRealtime() - started)
                        + " size=" + size + " loaded=" + (bitmap != null));
                if (onLoaded != null) onLoaded.run();
            }, error -> {
                if (!closed && token == current && active == target) {
                    transition.failed();
                    if (preview == null) target.setContentDescription("사진을 불러오지 못했어요");
                    Log.w("PhotoPlaceViewer", "image unavailable", error);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Activity teardown can close the dedicated worker.
        }
    }

    void prefetch(Uri uri, int requestedSize) {
        if (closed || uri == null) return;
        int size = clampSize(requestedSize);
        String key = uri + "#" + size;
        if (images.get(key) != null) return;
        try {
            worker.execute(() -> {
                if (closed || images.get(key) != null) return;
                try {
                    Bitmap decoded = decode(uri, size, new CancellationSignal());
                    if (decoded != null && !closed) {
                        images.put(key, decoded);
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (RejectedExecutionException ignored) {
        }
    }

    static int clampSize(int requestedSize) {
        return Math.max(1, Math.min(requestedSize, MAX_VIEWPORT_PX));
    }

    void clear(ImageView target) {
        if (active == target) {
            requests.cancel();
            token = null;
        }
        target.setImageDrawable(null);
    }

    private final View.OnAttachStateChangeListener detachment = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View view) { }
        @Override public void onViewDetachedFromWindow(View view) {
            if (active == view) {
                requests.cancel();
                active.removeOnAttachStateChangeListener(this);
                active = null;
                token = null;
            }
        }
    };

    private Bitmap preview(String uri) {
        Bitmap best = null;
        for (Map.Entry<String, Bitmap> entry : thumbnails.snapshot().entrySet()) {
            if (entry.getKey().startsWith(uri + "#")) {
                Bitmap candidate = entry.getValue();
                if (best == null || candidate.getWidth() > best.getWidth()) best = candidate;
            }
        }
        return best;
    }

    private Bitmap decode(Uri uri, int size, CancellationSignal cancellation) throws Exception {
        cancellation.throwIfCanceled();
        // A provider thumbnail can be only a few hundred pixels even when a larger size is
        // requested. It is a fallback, never the final photo while original bytes are readable.
        try {
            Bitmap original = decodeOriginal(uri, size, cancellation);
            if (original != null) return original;
        } catch (OperationCanceledException cancelled) {
            throw cancelled;
        } catch (OutOfMemoryError memoryPressure) {
            images.evictAll();
            try {
                Bitmap smaller = decodeOriginal(uri, Math.max(1, size / 2), cancellation);
                if (smaller != null) return smaller;
            } catch (OperationCanceledException cancelled) {
                throw cancelled;
            } catch (Exception | OutOfMemoryError unavailable) {
                // Continue to the provider fallback with the same cancellation signal.
            }
        } catch (Exception unavailable) {
            // Videos, inaccessible originals and non-image providers may support thumbnails only.
        }
        cancellation.throwIfCanceled();
        try {
            Bitmap result = resolver.loadThumbnail(uri, new Size(size, size), cancellation);
            if (cancellation.isCanceled()) {
                if (result != null) result.recycle();
                cancellation.throwIfCanceled();
            }
            if (result != null) return result;
        } catch (OperationCanceledException cancelled) {
            throw cancelled;
        } catch (Exception | OutOfMemoryError ignored) {
        }

        // Video fallback: extract representative frame via MediaMetadataRetriever.
        cancellation.throwIfCanceled();
        return decodeVideoFrame(uri, size);
    }

    private Bitmap decodeOriginal(Uri uri, int size, CancellationSignal cancellation) throws Exception {
        cancellation.throwIfCanceled();
        Bitmap result = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri),
                (decoder, info, source) -> {
                    cancellation.throwIfCanceled();
                    int width = info.getSize().getWidth();
                    int height = info.getSize().getHeight();
                    double ratio = Math.min(1d, size / (double) Math.max(width, height));
                    decoder.setTargetSize(Math.max(1, (int) Math.round(width * ratio)),
                            Math.max(1, (int) Math.round(height * ratio)));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
        // ImageDecoder applies EXIF orientation. Do not rotate, rewrite or resave source media.
        if (cancellation.isCanceled()) {
            if (result != null) result.recycle();
            cancellation.throwIfCanceled();
        }
        return result;
    }

    private Bitmap decodeVideoFrame(Uri uri, int size) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r")) {
            if (pfd == null) return null;
            retriever.setDataSource(pfd.getFileDescriptor());
            if (Build.VERSION.SDK_INT >= 27) {
                Bitmap scaled = retriever.getScaledFrameAtTime(
                        -1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, size, size);
                if (scaled != null) return scaled;
            }
            return retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    void close() {
        closed = true;
        requests.cancel();
        if (active != null) active.removeOnAttachStateChangeListener(detachment);
        active = null;
        token = null;
        worker.shutdownNow();
        images.evictAll();
    }
}

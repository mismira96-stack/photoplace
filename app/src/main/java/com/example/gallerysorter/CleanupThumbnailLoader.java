package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded thumbnails only: never decode full photos on the review UI thread. */
final class CleanupThumbnailLoader implements AutoCloseable {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 15, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(24), new ThreadPoolExecutor.DiscardOldestPolicy());
    CleanupThumbnailLoader(Context context) { this.context = context.getApplicationContext(); }
    void load(ImageView image, String uri) {
        image.setTag(uri);
        image.setImageResource(android.R.drawable.ic_menu_gallery);
        WeakReference<ImageView> target = new WeakReference<>(image);
        if (closed.get()) return;
        executor.execute(() -> {
            if (closed.get()) return;
            Bitmap bitmap;
            try { bitmap = context.getContentResolver().loadThumbnail(Uri.parse(uri), new Size(160, 160), null); }
            catch (Exception unknown) { return; }
            main.post(() -> {
                ImageView view = target.get();
                if (!closed.get() && view != null && uri.equals(view.getTag())) view.setImageBitmap(bitmap);
            });
        });
    }
    @Override public void close() { closed.set(true); executor.shutdownNow(); }
}

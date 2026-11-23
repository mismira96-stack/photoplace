package com.example.gallerysorter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Cached summaries first; live projection only when its revision changed. UI-thread entrypoints. */
final class HomeDashboardController {
    private final Executor worker;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HomeDashboardStore store;
    private final HomeRevisionReader revisions;
    private final Supplier<List<StoredAlbumSummary>> albums;
    private final Function<List<StoredAlbumSummary>, List<StoredAlbumSummary>> liveAlbums;
    private final Supplier<DiscoverySnapshotController> discovery;
    private final Runnable invalidate;
    private HomeDashboardSnapshot cached;
    private LinearLayout target;
    private Consumer<HomeDashboardSnapshot> renderer;
    private String shownKey;
    private boolean loaded, closed;
    private final HomeRefreshRequests requests = new HomeRefreshRequests();
    private long entryStarted;

    HomeDashboardController(Context context, Executor worker, Supplier<List<StoredAlbumSummary>> albums,
                            Function<List<StoredAlbumSummary>, List<StoredAlbumSummary>> liveAlbums,
                            Supplier<DiscoverySnapshotController> discovery, Runnable invalidate) {
        this.worker = worker; this.albums = albums; this.liveAlbums = liveAlbums;
        this.discovery = discovery; this.invalidate = invalidate;
        store = new HomeDashboardStore(context.getApplicationContext().getFilesDir());
        revisions = new HomeRevisionReader(context.getApplicationContext());
    }

    void beginEntry() { entryStarted = SystemClock.elapsedRealtime(); }

    void load(LinearLayout container, Consumer<HomeDashboardSnapshot> render) {
        if (closed) return;
        if (target != container) shownKey = null;
        target = container; renderer = render;
        final long started = entryStarted > 0 ? entryStarted : SystemClock.elapsedRealtime();
        if (!revisions.fullAccess()) {
            target.removeAllViews();
            shownKey = null;
            return;
        }
        if (!loaded) {
            long readStarted = SystemClock.elapsedRealtime();
            cached = store.read(); loaded = true;
            Log.i("PhotoPlaceHome", "cacheReadMs=" + (SystemClock.elapsedRealtime() - readStarted));
        }
        renderIfNeeded(cached, started, "cached");
        container.post(() -> { if (target == container) renderIfNeeded(cached, started, "cached"); });
        if (!requests.begin()) return;
        final HomeDashboardSnapshot previous = cached;
        worker.execute(() -> {
            String before = revisions.read();
            if (HomeDashboardPolicy.canSkip(previous, before)) {
                main.post(() -> {
                    Log.i("PhotoPlaceHome", "unchanged=true checkMs=" + (SystemClock.elapsedRealtime() - started));
                    finishRequest();
                });
                return;
            }
            main.post(() -> {
                if (closed) { finishRequest(); return; }
                // Invalidate the old 30-second Activity caches on their owning UI thread.
                invalidate.run();
                worker.execute(() -> refresh(before, started));
            });
        });
    }

    private void refresh(String before, long started) {
        try {
            long queryStarted = SystemClock.elapsedRealtime();
            Source source = readSource();
            long sourceReady = SystemClock.elapsedRealtime();
            HomeDashboardSnapshot snapshot = HomeDashboardSnapshot.build("", source.repository,
                    discovery.get().galleryReader(), source.albums);
            String after = revisions.read();
            HomeDashboardSnapshot finished = snapshot.withRevision(snapshot.complete()
                    ? HomeDashboardPolicy.stableRevision(before, after) : "");
            boolean saved = store.save(finished);
            Log.i("PhotoPlaceHome", "sourceMs=" + (sourceReady - queryStarted)
                    + " cardsMs=" + (SystemClock.elapsedRealtime() - sourceReady) + " cacheSaved=" + saved
                    + " stable=" + !finished.revision.isEmpty());
            main.post(() -> {
                try {
                    if (closed) return;
                    if (!revisions.fullAccess()) {
                        if (target != null && target.isAttachedToWindow()) target.removeAllViews();
                        shownKey = null;
                        return;
                    }
                    cached = finished;
                    renderIfNeeded(finished, started, "refreshed");
                } finally {
                    finishRequest();
                }
            });
        } catch (Exception error) {
            main.post(() -> { Log.w("PhotoPlaceHome", "refresh failed; retaining last summary", error); finishRequest(); });
        }
    }

    private Source readSource() {
        List<StoredAlbumSummary> currentAlbums = liveAlbums.apply(albums.get());
        return new Source(currentAlbums, discovery.get().repository(currentAlbums));
    }

    private void renderIfNeeded(HomeDashboardSnapshot snapshot, long started, String phase) {
        if (closed || target == null || !target.isAttachedToWindow()
                || !HomeDashboardPolicy.shouldRender(shownKey, snapshot)) return;
        shownKey = snapshot.displayKey();
        renderer.accept(snapshot);
        final LinearLayout rendered = target;
        Log.i("PhotoPlaceHome", phase + "BindMs=" + (SystemClock.elapsedRealtime() - started));
        rendered.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                rendered.getViewTreeObserver().removeOnPreDrawListener(this);
                Log.i("PhotoPlaceHome", phase + "PreDrawMs=" + (SystemClock.elapsedRealtime() - started));
                return true;
            }
        });
    }

    void refreshIfVisible() {
        if (target != null && target.isAttachedToWindow()) {
            entryStarted = SystemClock.elapsedRealtime();
            load(target, renderer);
        }
    }

    private void finishRequest() {
        if (requests.complete()) refreshIfVisible();
    }

    void openCountry(String code, Consumer<OverseasCountryProjection> opened, Runnable missing) {
        final LinearLayout origin = target;
        invalidate.run();
        worker.execute(() -> {
            try {
                Source source = readSource();
                OverseasCountryProjection selected = null;
                for (OverseasCountryProjection country : OverseasCountryProjection.build(source.repository.discoveryMemories(), source.albums)) {
                    if (country.countryCode.equals(code)) { selected = country; break; }
                }
                final OverseasCountryProjection result = selected;
                main.post(() -> {
                    if (closed || origin == null || !origin.isAttachedToWindow()) return;
                    if (result == null) missing.run(); else opened.accept(result);
                });
            } catch (Exception error) {
                main.post(() -> { if (!closed && origin != null && origin.isAttachedToWindow()) missing.run(); });
            }
        });
    }

    void close() { closed = true; target = null; renderer = null; }

    private static final class Source {
        final List<StoredAlbumSummary> albums;
        final MemoryRepository repository;
        Source(List<StoredAlbumSummary> albums, MemoryRepository repository) { this.albums = albums; this.repository = repository; }
    }
}

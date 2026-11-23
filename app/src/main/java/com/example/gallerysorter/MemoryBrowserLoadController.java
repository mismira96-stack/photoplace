package com.example.gallerysorter;

import android.app.Activity;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Load discovery records and every collection summary once, away from the UI thread. */
final class MemoryBrowserLoadController {
    private final Activity activity;
    private final MemoryScreenLoadController<MemoryBrowserEntry> loader;
    private final MemorySeenStore seen;
    private final Supplier<MemoryRepository> repository;
    private final Supplier<List<MemoryCollection>> collections;
    private final Supplier<Map<String, String>> aliases;
    private final Supplier<MemoryMediaResolver.GalleryReader> reader;
    private boolean requestedAlbums;
    private boolean quietRefresh;
    private Consumer<String> requestedError;
    private MemoryBrowserEntry lastAlbumsEntry;
    private MemoryBrowserEntry lastDiscoveryEntry;

    MemoryBrowserEntry getLastEntry(boolean includeAlbums) {
        return includeAlbums ? lastAlbumsEntry : lastDiscoveryEntry;
    }

    private void setLastEntry(boolean includeAlbums, MemoryBrowserEntry entry) {
        if (includeAlbums) {
            lastAlbumsEntry = entry;
        } else {
            lastDiscoveryEntry = entry;
        }
    }

    MemoryBrowserLoadController(Activity activity, Supplier<MemoryRepository> repository,
            Supplier<List<MemoryCollection>> collections, Supplier<Map<String, String>> aliases,
            Supplier<MemoryMediaResolver.GalleryReader> reader) {
        this.activity = activity;
        this.repository = repository; this.collections = collections;
        this.seen = new MemorySeenStore(activity);
        this.aliases = aliases; this.reader = reader;
        loader = new MemoryScreenLoadController<>(activity, entry -> entry != null && entry.complete(),
                "발견 기록을 불러오는 중이에요", "PhotoPlaceBrowser");
    }

    void preload(boolean includeAlbums) {
        MemoryBrowserEntry last = getLastEntry(includeAlbums);
        if (loader.isPending() || (last != null && last.complete())) return;
        quietRefresh = true;
        loader.preload(key(includeAlbums), () -> read(includeAlbums), (entry, revision) -> {
            setLastEntry(includeAlbums, entry.withRevision(revision));
        });
    }

    void load(boolean includeAlbums, Consumer<MemoryBrowserEntry> ready, Consumer<String> error) {
        requestedAlbums = includeAlbums;
        requestedError = error;
        quietRefresh = false;
        final MemoryBrowserEntry cached = getLastEntry(includeAlbums);
        if (cached != null) {
            // Immediate response: publish cached entry instantly with 0ms delay!
            ready.accept(cached.withSeenVersions(seen::version));
            quietRefresh = true;
            loader.loadQuietly(key(includeAlbums), () -> read(includeAlbums), (entry, revision) -> {
                setLastEntry(includeAlbums, entry.withRevision(revision));
                if (!cached.matches(entry)) {
                    ready.accept(entry.withSeenVersions(seen::version));
                }
            });
            return;
        }
        String requestKey = key(includeAlbums);
        BiConsumer<MemoryBrowserEntry, String> bind = (entry, revision) -> {
            setLastEntry(includeAlbums, entry.withRevision(revision));
            ready.accept(entry.withSeenVersions(seen::version));
        };
        if (!loader.promotePreload(requestKey, bind, error)) {
            loader.load(requestKey, () -> read(includeAlbums), bind, error);
        }
    }

    void refresh(boolean visible, boolean visibleAlbums, BooleanSupplier detailPending,
                 Runnable capturePosition, BiConsumer<MemoryBrowserEntry, Boolean> ready) {
        if (detailPending.getAsBoolean()) return;
        boolean pending = isPending();
        if (!visible && !pending) return;
        boolean includeAlbums = pending ? requestedAlbums : visibleAlbums;
        BiConsumer<MemoryBrowserEntry, String> bind = (entry, revision) -> {
            setLastEntry(includeAlbums, entry.withRevision(revision));
            if (detailPending.getAsBoolean()) return;
            if (visible) capturePosition.run();
            ready.accept(entry.withSeenVersions(seen::version), includeAlbums);
        };
        quietRefresh = !pending;
        if (pending) loader.load(key(includeAlbums), () -> read(includeAlbums), bind, requestedError);
        else loader.loadQuietly(key(includeAlbums), () -> read(includeAlbums), bind);
    }

    private MemoryBrowserEntry read(boolean includeAlbums) {
        return MemoryBrowserEntry.build(repository.get(), includeAlbums,
                collections.get(), aliases.get(), reader.get())
                .withUnresolvedCount(MemoryManagementEntry.unresolvedCount(activity));
    }

    private static String key(boolean includeAlbums) {
        return includeAlbums ? "browser|albums" : "browser|discovery";
    }

    void cancel() { loader.cancel(); }
    void cancelUnlessPreloading() { if (!loader.isPreloading()) cancel(); }
    boolean isPending() { return !quietRefresh && loader.isPending(); }
    void close() { loader.close(); }
}

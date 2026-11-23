package com.example.gallerysorter;

import android.app.Activity;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Detail-specific source/completeness policy; scheduling is shared with the browser. */
final class MemoryDetailLoadController {
    private final MemoryScreenLoadController<MemoryBrowserDetail> loader;
    private final MemorySeenStore seen;
    private final Supplier<List<StoredAlbumSummary>> albums;
    private final Supplier<DiscoverySnapshotController> discovery;
    private StoredAlbumSummary activeAlbum;
    private String activeAlbumKey = "";
    private MemoryBrowserEntry activeAlbumEntry;

    MemoryDetailLoadController(Activity activity, Supplier<List<StoredAlbumSummary>> albums,
                               Supplier<DiscoverySnapshotController> discovery) {
        this.albums = albums;
        this.seen = new MemorySeenStore(activity);
        this.discovery = discovery;
        loader = new MemoryScreenLoadController<>(activity, detail -> detail.mediaResolved,
                "사진을 불러오는 중이에요", "PhotoPlaceDetail",
                20, 50000, detail -> detail.sourceUris.size());
    }

    void load(String key, boolean includeAlbums, MemoryBrowserEntry browserEntry,
              BiConsumer<MemoryBrowserDetail, String> ready,
              Consumer<String> error) {
        clearAlbum();
        String requestKey = (includeAlbums ? "albums|" : "discovery|") + key;
        MemoryBrowserDetail cached = loader.getCached(requestKey);
        if (cached != null) {
            ready.accept(cached, "");
            seen.acknowledge(cached);
            return;
        }
        loader.load(requestKey,
                revision -> {
                    DiscoverySnapshotController controller = discovery.get();
                    if (browserEntry != null) {
                        MemoryBrowserDetail detail = controller.loadBrowserCardDetail(browserEntry, key, revision);
                        if (detail != null) return detail;
                    }
                    return controller.loadBrowserDetail(key, albums.get(), includeAlbums);
                },
                (detail, revision) -> {
                    ready.accept(detail, revision);
                    seen.acknowledge(detail);
                }, error);
    }

    void loadAlbum(StoredAlbumSummary album, MemoryBrowserEntry entry, BiConsumer<MemoryBrowserDetail, String> ready,
                   Consumer<String> error) {
        loader.load("physical-album|" + album.relativePath,
                () -> discovery.get().loadPhysicalAlbumDetail(album, entry),
                (detail, revision) -> {
                    activeAlbum = album;
                    activeAlbumKey = detail.item.memoryKey;
                    activeAlbumEntry = entry;
                    ready.accept(detail, revision);
                }, error);
    }

    void reload(String key, boolean includeAlbums, BiConsumer<MemoryBrowserDetail, String> ready,
                Consumer<String> error) {
        if (activeAlbum != null && activeAlbumKey.equals(key)) loadAlbum(activeAlbum, activeAlbumEntry, ready, error);
        else load(key, includeAlbums, null, ready, error);
    }

    void clearAlbum() { activeAlbum = null; activeAlbumKey = ""; activeAlbumEntry = null; }

    int detailDestination() {
        return activeAlbum == null ? DiscoveryTabDestinations.ALL : DiscoveryTabDestinations.ALBUMS;
    }

    void returnToSource(Runnable albums, Runnable other) {
        if (activeAlbum == null) other.run();
        else { clearAlbum(); albums.run(); }
    }

    void bindAlbumActions(android.widget.LinearLayout root,
                          Consumer<StoredAlbumSummary> gallery, Consumer<StoredAlbumSummary> manage) {
        if (activeAlbum == null) return;
        StoredAlbumSummary album = activeAlbum;
        PhysicalAlbumActionsRenderer.addTo(root, () -> gallery.accept(album), () -> manage.accept(album));
    }

    void captureAlbumScroll(android.widget.ScrollView list, java.util.function.IntConsumer capture) {
        if (list != null) capture.accept(list.getScrollY());
    }

    void cancel() { loader.cancel(); }
    boolean isPending() { return loader.isPending(); }
    void close() { loader.close(); }
}

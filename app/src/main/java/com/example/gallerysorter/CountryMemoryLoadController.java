package com.example.gallerysorter;

import android.app.Activity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shared async country-place screen for both Home and the discovery browser. */
final class CountryMemoryLoadController {
    static final class Entry {
        final String code;
        final MemoryBrowserEntry browser;
        final OverseasCountryProjection projection;
        Entry(String code, MemoryBrowserEntry browser) {
            this.code = code; this.browser = browser;
            projection = OverseasCountryProjection.fromUnified(code, browser.records);
        }
    }
    private final MemoryScreenLoadController<Entry> loader;
    private final Supplier<MemoryRepository> repository;
    private final Supplier<MemoryMediaResolver.GalleryReader> reader;
    private final Map<String, Integer> positions = new LinkedHashMap<>();
    private boolean returnsHome;
    private Entry current;

    CountryMemoryLoadController(Activity activity, Supplier<MemoryRepository> repository,
                                Supplier<MemoryMediaResolver.GalleryReader> reader) {
        this.repository = repository; this.reader = reader;
        loader = new MemoryScreenLoadController<>(activity, e -> e.browser.complete(),
                "국가별 기록을 불러오는 중이에요", "PhotoPlaceCountry", 3, 10000, e -> e.browser.records.size());
    }
    void openHome(String code, MemoryBrowserEntry cached, Consumer<Entry> ready, Consumer<String> error) {
        returnsHome = true; load(code, cached, ready, error);
    }
    void openDiscovery(String code, MemoryBrowserEntry cached, Consumer<Entry> ready, Consumer<String> error) {
        returnsHome = false; load(code, cached, ready, error);
    }
    void reopen(OverseasCountryProjection projection, MemoryBrowserEntry cached, Consumer<Entry> ready,
                Consumer<String> error) {
        if (projection == null) { error.accept("국가 기록을 찾지 못했어요."); return; }
        load(projection.countryCode, cached, ready, error);
    }
    private void load(String code, MemoryBrowserEntry cached, Consumer<Entry> ready, Consumer<String> error) {
        loader.load("country|" + code, revision -> {
            MemoryBrowserEntry entry = cached == null ? null : cached.countryForRevision(code, revision);
            if (entry == null) entry = MemoryBrowserEntry.buildCountry(repository.get(), code, reader.get());
            return entry.records.isEmpty() ? null : new Entry(code, entry);
        }, (entry, revision) -> {
            current = new Entry(entry.code, entry.browser.withRevision(revision));
            ready.accept(current);
        }, error);
    }
    MemoryBrowserEntry currentBrowserEntry() { return current == null ? null : current.browser; }
    int destination() { return returnsHome ? DiscoveryTabDestinations.HOME : DiscoveryTabDestinations.ALL; }
    int scrollY() { return current == null ? 0 : positions.getOrDefault(current.code, 0); }
    void capture(int scrollY) {
        if (current == null) return;
        positions.put(current.code, Math.max(0, scrollY));
        if (positions.size() > 8) positions.remove(positions.keySet().iterator().next());
    }
    void back(OverseasCountryProjection active, Runnable home, Runnable browser) {
        cancel();
        if (active == null || returnsHome) home.run(); else browser.run();
    }
    void cancel() { loader.cancel(); }
    boolean isPending() { return loader.isPending(); }
    void close() { loader.close(); positions.clear(); }
}

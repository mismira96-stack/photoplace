package com.example.gallerysorter;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Objects;

/** Bounded stable results, invalidated by source changes; only the latest request may publish. */
final class MemoryDetailLoadSession<T> {
    interface RevisionSource<T> {
        T read(String revision) throws Exception;
    }

    private final AtomicLong generation = new AtomicLong();
    private final LinkedHashMap<String, Cached<T>> cache = new LinkedHashMap<>(4, 0.75f, true);
    private final Predicate<T> reusable;
    private final int maxEntries;
    private final long maxWeight;
    private final ToLongFunction<T> weight;
    private long cachedWeight;

    MemoryDetailLoadSession() { this(value -> true); }
    MemoryDetailLoadSession(Predicate<T> reusable) { this(reusable, 1, 1, value -> 1); }
    MemoryDetailLoadSession(Predicate<T> reusable, int maxEntries, long maxWeight,
                            ToLongFunction<T> weight) {
        if (maxEntries < 1 || maxWeight < 1) throw new IllegalArgumentException("Invalid cache bounds");
        this.reusable = reusable;
        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        this.weight = weight;
    }

    long begin() { return generation.incrementAndGet(); }
    boolean current(long request) { return generation.get() == request; }
    void cancel() { generation.incrementAndGet(); }
    synchronized void clear() { cancel(); cache.clear(); cachedWeight = 0; }

    synchronized T get(String key) {
        Cached<T> entry = cache.get(key);
        return entry != null && reusable.test(entry.value) ? entry.value : null;
    }

    Result<T> read(long request, String key, Supplier<String> revision, Callable<T> source) throws Exception {
        return read(request, key, revision, ignored -> source.call());
    }

    Result<T> read(long request, String key, Supplier<String> revision, RevisionSource<T> source) throws Exception {
        if (!current(request)) return null;
        String before = revision.get();
        synchronized (this) {
            if (!current(request)) return null;
            // Unknown or changed revision invalidates every place, including entries not requested now.
            Iterator<Cached<T>> entries = cache.values().iterator();
            while (entries.hasNext()) {
                Cached<T> entry = entries.next();
                if (before == null || before.isEmpty() || !before.equals(entry.revision)) {
                    cachedWeight -= entry.weight;
                    entries.remove();
                }
            }
            Cached<T> previous = cache.get(key);
            if (previous != null) return new Result<>(previous.value, before, true, false);
        }
        T value = source.read(before);
        String after = revision.get();
        String stable = HomeDashboardPolicy.stableRevision(before, after);
        if (value == null || !reusable.test(value)) stable = "";
        synchronized (this) {
            if (!current(request)) return null;
            if (value != null && !stable.isEmpty()) {
                long cost = Math.max(1, weight.applyAsLong(value));
                if (cost <= maxWeight) {
                    Cached<T> replaced = cache.remove(key);
                    if (replaced != null) cachedWeight -= replaced.weight;
                    while (!cache.isEmpty() && (cache.size() >= maxEntries || cachedWeight > maxWeight - cost)) {
                        Iterator<Cached<T>> entries = cache.values().iterator();
                        cachedWeight -= entries.next().weight;
                        entries.remove();
                    }
                    cache.put(key, new Cached<>(stable, value, cost));
                    cachedWeight += cost;
                }
            }
        }
        return new Result<>(value, stable, false, !Objects.equals(before, after));
    }

    static final class Result<T> {
        final T value;
        final String revision;
        final boolean reused;
        final boolean sourceChanged;
        Result(T value, String revision, boolean reused, boolean sourceChanged) {
            this.value = value; this.revision = revision; this.reused = reused;
            this.sourceChanged = sourceChanged;
        }
    }

    private static final class Cached<T> {
        final String revision;
        final T value;
        final long weight;
        Cached(String revision, T value, long weight) {
            this.revision = revision; this.value = value; this.weight = weight;
        }
    }
}

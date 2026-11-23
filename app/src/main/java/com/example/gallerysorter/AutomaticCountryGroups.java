package com.example.gallerysorter;

import java.text.SimpleDateFormat;
import java.util.*;

/** Automatic, read-only country containers. Never writes user collection membership or aliases. */
final class AutomaticCountryGroups {
    static final class Group {
        final String code, key, title;
        final List<MemoryRecord> records;
        final MemoryCollectionCardSummary summary;
        final long latest;
        Group(OverseasCountryProjection projection, MemoryBrowserMediaProjection media) {
            code = projection.countryCode;
            key = "country:" + code;
            title = projection.countryName;
            records = projection.discoveryRecords;
            MemoryBrowserSummary totals = media.summary(records);
            long first = Long.MAX_VALUE, last = 0;
            String cover = "";
            LinkedHashSet<String> names = new LinkedHashSet<>();
            for (MemoryRecord record : records) {
                long start = record.startDateMillis > 0 ? record.startDateMillis : record.endDateMillis;
                if (start > 0) first = Math.min(first, start);
                long end = Math.max(record.startDateMillis, record.endDateMillis);
                if (cover.isEmpty() || end > last) cover = record.coverUri;
                last = Math.max(last, end);
                MemoryBrowserItem item = MemoryBrowserItem.from(record);
                if (item != null) names.add(item.title);
            }
            latest = last;
            List<String> shown = new ArrayList<>(names);
            String places = String.join(" · ", shown.subList(0, Math.min(3, shown.size())));
            if (shown.size() > 3) places += " 외 " + (shown.size()-3) + "곳";
            String dates = "";
            if (first != Long.MAX_VALUE && last > 0) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
                String a = formatter.format(new Date(first)), b = formatter.format(new Date(last));
                dates = a.equals(b) ? a : a + " ~ " + b;
            }
            boolean complete = true;
            for (MemoryRecord record : records) complete &= !"불러오기 필요".equals(media.counts.get(record.memoryKey));
            summary = MemoryCollectionCardSummary.forCountry(totals.photoCount, cover, places, dates, complete);
        }
    }

    final List<Group> groups;
    final String signature;
    private AutomaticCountryGroups(List<Group> groups) {
        this.groups = Collections.unmodifiableList(groups);
        StringBuilder key = new StringBuilder();
        for (Group group : groups) {
            key.append(group.code).append('\0').append(group.summary.countText).append('\0')
                    .append(group.summary.coverUri).append('\0').append(group.summary.dateRange)
                    .append(group.summary.complete).append('\0');
            for (MemoryRecord record : group.records) key.append(record.memoryKey).append('\0');
        }
        signature = key.toString();
    }

    static AutomaticCountryGroups build(List<MemoryRecord> records, MemoryBrowserMediaProjection media) {
        List<Group> groups = new ArrayList<>();
        for (OverseasCountryProjection country : OverseasCountryProjection.build(records, Collections.emptyList()))
            groups.add(new Group(country, media));
        groups.sort((a,b) -> { int recent = Long.compare(b.latest,a.latest); return recent != 0 ? recent : a.title.compareTo(b.title); });
        return new AutomaticCountryGroups(groups);
    }

    static List<MemoryRecord> recordsForCountry(List<MemoryRecord> records, String country) {
        List<MemoryRecord> result = new ArrayList<>();
        for (MemoryRecord record : records) if (record != null && country.equals(code(record))) result.add(record);
        return Collections.unmodifiableList(result);
    }

    static List<MemoryRecord> domesticAndUnknown(List<MemoryRecord> records) {
        List<MemoryRecord> result = new ArrayList<>();
        for (MemoryRecord record : records) {
            if (record == null) continue;
            String code = code(record);
            if (code.isEmpty() || CountryIdentityNormalizer.isKorea(code, record.countryName)) result.add(record);
        }
        return result;
    }

    private static String code(MemoryRecord record) {
        return CountryIdentityNormalizer.countryCode(record.countryCode, record.countryName);
    }
}

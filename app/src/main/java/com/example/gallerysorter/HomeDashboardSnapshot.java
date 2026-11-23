package com.example.gallerysorter;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Disposable Home summary only. Never a source for detail, identity, or organization. */
final class HomeDashboardSnapshot {
    static final int SCHEMA = 1;
    final String revision;
    final List<HomeRecentPlacesResolver.Card> places;
    final List<CountryCard> countries;

    HomeDashboardSnapshot(String revision, List<HomeRecentPlacesResolver.Card> places, List<CountryCard> countries) {
        this.revision = revision == null ? "" : revision;
        this.places = Collections.unmodifiableList(new ArrayList<>(places));
        this.countries = Collections.unmodifiableList(new ArrayList<>(countries));
    }

    HomeDashboardSnapshot withRevision(String value) { return new HomeDashboardSnapshot(value, places, countries); }

    boolean complete() {
        for (HomeRecentPlacesResolver.Card card : places) if (!card.complete) return false;
        return true;
    }

    static HomeDashboardSnapshot build(String revision, MemoryRepository repository,
                                        MemoryMediaResolver.GalleryReader reader,
                                        List<StoredAlbumSummary> albums) {
        ArrayList<CountryCard> countries = new ArrayList<>();
        for (OverseasCountryProjection country : OverseasCountryProjection.build(repository.discoveryMemories(), albums)) {
            countries.add(new CountryCard(country.countryCode, country.countryName, cover(country),
                    country.discoveryRecords.size(), country.organizedAlbums.size()));
        }
        return new HomeDashboardSnapshot(revision, HomeRecentPlacesResolver.build(repository, reader), countries);
    }

    private static String cover(OverseasCountryProjection country) {
        for (MemoryRecord record : country.discoveryRecords) if (!record.coverUri.isEmpty()) return record.coverUri;
        for (StoredAlbumSummary album : country.organizedAlbums) if (!album.thumbnailUri.isEmpty()) return album.thumbnailUri;
        return "";
    }

    JSONObject toJson() throws Exception {
        JSONObject root = new JSONObject().put("schema", SCHEMA).put("revision", revision);
        JSONArray items = new JSONArray();
        for (HomeRecentPlacesResolver.Card card : places) items.put(new JSONObject()
                .put("key", card.memoryKey).put("title", card.title).put("cover", card.coverUri)
                .put("count", card.countText).put("complete", card.complete));
        root.put("places", items);
        JSONArray abroad = new JSONArray();
        for (CountryCard card : countries) abroad.put(new JSONObject().put("code", card.code).put("name", card.name)
                .put("cover", card.coverUri).put("discovered", card.discoveryCount).put("albums", card.albumCount));
        return root.put("countries", abroad);
    }

    String displayKey() {
        try { return toJson().put("revision", "").toString(); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }

    static HomeDashboardSnapshot fromJson(JSONObject root) throws Exception {
        if (root.getInt("schema") != SCHEMA) throw new IllegalArgumentException("Unsupported Home cache");
        JSONArray items = root.getJSONArray("places");
        JSONArray abroad = root.getJSONArray("countries");
        if (items.length() > 4 || abroad.length() > 64) throw new IllegalArgumentException("Oversized Home cache");
        List<HomeRecentPlacesResolver.Card> places = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject card = items.getJSONObject(i);
            String key = card.getString("key");
            if (key.isEmpty()) throw new IllegalArgumentException("Missing Home destination");
            places.add(new HomeRecentPlacesResolver.Card(key, card.getString("title"), card.getString("cover"),
                    card.getString("count"), card.optBoolean("complete", true)));
        }
        List<CountryCard> countries = new ArrayList<>();
        for (int i = 0; i < abroad.length(); i++) {
            JSONObject card = abroad.getJSONObject(i);
            countries.add(new CountryCard(card.getString("code"), card.getString("name"), card.getString("cover"),
                    card.getInt("discovered"), card.getInt("albums")));
        }
        return new HomeDashboardSnapshot(root.getString("revision"), places, countries);
    }

    static final class CountryCard {
        final String code, name, coverUri;
        final int discoveryCount, albumCount;
        CountryCard(String code, String name, String coverUri, int discoveryCount, int albumCount) {
            if (code.isEmpty() || discoveryCount < 0 || albumCount < 0) throw new IllegalArgumentException("Invalid country card");
            this.code = code; this.name = name; this.coverUri = coverUri;
            this.discoveryCount = discoveryCount; this.albumCount = albumCount;
        }
        String summary() {
            if (discoveryCount > 0 && albumCount > 0) return "발견 " + discoveryCount + "곳\n위치 앨범 " + albumCount + "개";
            return discoveryCount > 0 ? "발견 기록 " + discoveryCount + "곳" : "위치 앨범 " + albumCount + "개";
        }
    }
}

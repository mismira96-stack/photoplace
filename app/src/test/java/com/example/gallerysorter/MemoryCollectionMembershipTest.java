package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MemoryCollectionMembershipTest {
    @Test
    public void groupedMembersAreHiddenFromBaseProjectionButUnknownAliasesRemainVisible() {
        MemoryRecord sapporo = record("discovery:sapporo", "삿포로");
        MemoryRecord otaru = record("discovery:otaru", "오타루");
        MemoryRecord unknown = record("discovery:unknown", "알 수 없는 장소");
        List<MemoryRecord> source = Arrays.asList(sapporo, otaru, unknown);
        MemoryCollection collection = new MemoryCollection("group_trip", "홋카이도", Arrays.asList(
                new MemoryCollection.Member("mem_sapporo", "discovery:sapporo"),
                new MemoryCollection.Member("mem_otaru", "discovery:otaru")), 1L, 1L);
        Map<String, String> aliases = new HashMap<>();
        aliases.put("discovery:sapporo", "mem_sapporo");
        aliases.put("discovery:otaru", "mem_otaru");

        Set<String> memberIds = MemoryCollectionMembership.stableIds(Collections.singletonList(collection));
        List<MemoryRecord> visible = MemoryCollectionMembership.ungroupedRecords(source, memberIds, aliases);

        assertEquals(1, visible.size());
        assertSame(unknown, visible.get(0));
        assertEquals(3, source.size());
    }

    @Test
    public void missingAliasMapDoesNotHideCanonicalRecords() {
        MemoryRecord record = record("discovery:sapporo", "삿포로");
        MemoryCollection collection = new MemoryCollection("group_trip", "홋카이도", Arrays.asList(
                new MemoryCollection.Member("mem_sapporo", "discovery:sapporo"),
                new MemoryCollection.Member("mem_otaru", "discovery:otaru")), 1L, 1L);

        List<MemoryRecord> visible = MemoryCollectionMembership.ungroupedRecords(
                Collections.singletonList(record),
                MemoryCollectionMembership.stableIds(Collections.singletonList(collection)),
                Collections.<String, String>emptyMap());

        assertEquals(1, visible.size());
        assertSame(record, visible.get(0));
    }

    @Test
    public void hidesMembersAcrossMultipleCollections() {
        MemoryRecord sapporo = record("discovery:sapporo", "삿포로");
        MemoryRecord otaru = record("discovery:otaru", "오타루");
        MemoryRecord tokyo = record("discovery:tokyo", "도쿄");
        MemoryRecord kyoto = record("discovery:kyoto", "교토");
        MemoryRecord ungrouped = record("discovery:nara", "나라");
        MemoryCollection sapporoTrip = new MemoryCollection("group_sapporo", "삿포로", Arrays.asList(
                new MemoryCollection.Member("mem_sapporo", "discovery:sapporo"),
                new MemoryCollection.Member("mem_otaru", "discovery:otaru")), 1L, 1L);
        MemoryCollection tokyoTrip = new MemoryCollection("group_tokyo", "도쿄", Arrays.asList(
                new MemoryCollection.Member("mem_tokyo", "discovery:tokyo"),
                new MemoryCollection.Member("mem_kyoto", "discovery:kyoto")), 2L, 2L);
        Map<String, String> aliases = new HashMap<>();
        aliases.put("discovery:sapporo", "mem_sapporo");
        aliases.put("discovery:otaru", "mem_otaru");
        aliases.put("discovery:tokyo", "mem_tokyo");
        aliases.put("discovery:kyoto", "mem_kyoto");

        List<MemoryRecord> visible = MemoryCollectionMembership.ungroupedRecords(
                Arrays.asList(sapporo, otaru, tokyo, kyoto, ungrouped),
                MemoryCollectionMembership.stableIds(Arrays.asList(sapporoTrip, tokyoTrip)),
                aliases);

        assertEquals(1, visible.size());
        assertSame(ungrouped, visible.get(0));
    }

    private static MemoryRecord record(String key, String title) {
        DiscoveryPhotoRef ref = new DiscoveryPhotoRef("content://" + title, 1L, MediaKind.PHOTO,
                "image/jpeg", "photo.jpg", 1785888000000L, "", "", "JP", "Japan",
                "Hokkaido", "", "", 1L, 1L, false);
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(key, key, title,
                "JP", "Japan", "Hokkaido", title, 1, 1, 0,
                ref.takenAtMillis, ref.takenAtMillis, ref.sourceUri,
                Collections.singletonList(ref), 0, 1L);
        return MemoryRepository.fromDiscoveryGroup(group);
    }
}

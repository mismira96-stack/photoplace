package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;

public class DiscoverySnapshotMapperTest {
    @Test
    public void groupsLocatedItemsByLocationKey() {
        DiscoverySnapshotMapper.SourceItem sapporoPhoto = item(
                "content://media/external/images/media/101",
                "IMG_0101.jpg",
                "image/jpeg",
                1785600000000L,
                "삿포로",
                false,
                false,
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan");
        DiscoverySnapshotMapper.SourceItem sapporoVideo = item(
                "content://media/external/video/media/102",
                "VID_0102.mp4",
                "video/mp4",
                1785945600000L,
                "삿포로",
                false,
                true,
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan");
        DiscoverySnapshotMapper.SourceItem otaruPhoto = item(
                "content://media/external/images/media/201",
                "IMG_0201.jpg",
                "image/jpeg",
                1785772800000L,
                "오타루",
                false,
                false,
                "JP",
                "Japan",
                "Hokkaido",
                "Otaru, Hokkaido, Japan");

        DiscoverySnapshot snapshot = DiscoverySnapshotMapper.fromSourceItems(
                Arrays.asList(sapporoPhoto, sapporoVideo, otaruPhoto),
                3,
                11L,
                1786000000000L,
                "test-source",
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);

        assertEquals(3, snapshot.sourceItemCount);
        assertEquals(2, snapshot.groupCount());
        DiscoveryMemoryGroup sapporo = snapshot.groups.get(0);
        assertEquals("discovery:삿포로", sapporo.memoryKey);
        assertEquals("삿포로", sapporo.placeKey);
        assertEquals(2, sapporo.itemCount);
        assertEquals(1, sapporo.photoCount);
        assertEquals(1, sapporo.videoCount);
        assertEquals(1785600000000L, sapporo.startDateMillis);
        assertEquals(1785945600000L, sapporo.endDateMillis);
        assertEquals("content://media/external/images/media/101", sapporo.coverUri);
        assertEquals("JP", sapporo.countryCode);
        assertEquals("일본", sapporo.countryName);
        assertEquals("Hokkaido", sapporo.adminArea);
        assertEquals(2, sapporo.photoRefs.size());
        assertEquals(101L, sapporo.photoRefs.get(0).mediaStoreId);
        assertEquals(MediaKind.PHOTO, sapporo.photoRefs.get(0).mediaKind);
        assertEquals(MediaKind.VIDEO, sapporo.photoRefs.get(1).mediaKind);
        assertEquals("", sapporo.photoRefs.get(0).sourceRelativePath);

        DiscoveryMemoryGroup otaru = snapshot.groups.get(1);
        assertEquals("오타루", otaru.placeName);
        assertEquals(1, otaru.itemCount);
    }

    @Test
    public void skipsNoLocationAndInvalidItems() {
        DiscoverySnapshotMapper.SourceItem noLocation = item(
                "content://media/external/images/media/301",
                "IMG_0301.jpg",
                "image/jpeg",
                1785600000000L,
                PlaceNamePolicy.LOCATION_NONE,
                true,
                false,
                "",
                "",
                "",
                "");
        DiscoverySnapshotMapper.SourceItem locationNoneKey = item(
                "content://media/external/images/media/302",
                "IMG_0302.jpg",
                "image/jpeg",
                1785600000000L,
                PlaceNamePolicy.LOCATION_NONE,
                false,
                false,
                "",
                "",
                "",
                "");
        DiscoverySnapshotMapper.SourceItem emptyUri = item(
                "",
                "IMG_0303.jpg",
                "image/jpeg",
                1785600000000L,
                "송파구",
                false,
                false,
                "KR",
                "대한민국",
                "서울특별시",
                "서울특별시 송파구");
        DiscoverySnapshotMapper.SourceItem valid = item(
                "content://media/external/images/media/303",
                "IMG_0303.jpg",
                "image/jpeg",
                1785600000000L,
                "송파구",
                false,
                false,
                "KR",
                "대한민국",
                "서울특별시",
                "서울특별시 송파구");

        DiscoverySnapshot snapshot = DiscoverySnapshotMapper.fromSourceItems(
                Arrays.asList(noLocation, locationNoneKey, emptyUri, valid),
                4,
                12L,
                1786000000000L,
                "test-source",
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);

        assertEquals(4, snapshot.sourceItemCount);
        assertEquals(1, snapshot.groupCount());
        assertEquals("송파구", snapshot.groups.get(0).placeKey);
        assertEquals(1, snapshot.groups.get(0).itemCount);
    }

    @Test
    public void unknownMediaStoreIdFallsBackToSentinel() {
        DiscoverySnapshotMapper.SourceItem item = item(
                "content://media/external/images/media/not-a-number",
                "IMG_0401.jpg",
                "image/jpeg",
                1785600000000L,
                "수원",
                false,
                false,
                "KR",
                "대한민국",
                "경기도",
                "경기도 수원시");

        DiscoverySnapshot snapshot = DiscoverySnapshotMapper.fromSourceItems(
                Arrays.asList(item),
                1,
                13L,
                1786000000000L,
                "",
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);

        assertEquals(DiscoveryPhotoRef.UNKNOWN_ID, snapshot.groups.get(0).photoRefs.get(0).mediaStoreId);
        assertEquals(DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION, snapshot.analysisPolicyVersion);
        assertEquals(DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION, snapshot.countryIdentityPolicyVersion);
    }

    @Test
    public void fromPhotoItemsHandlesNullListAndNullItems() {
        DiscoverySnapshot empty = DiscoverySnapshotMapper.fromPhotoItems(
                null,
                14L,
                1786000000000L,
                "null-list");

        assertEquals(0, empty.sourceItemCount);
        assertEquals(0, empty.groupCount());

        DiscoverySnapshot onlyNullItems = DiscoverySnapshotMapper.fromPhotoItems(
                Arrays.asList(null, null),
                15L,
                1786000000000L,
                "null-items");

        assertEquals(2, onlyNullItems.sourceItemCount);
        assertEquals(0, onlyNullItems.groupCount());
    }

    @Test
    public void unknownTakenAtUsesSentinelTime() {
        DiscoverySnapshotMapper.SourceItem item = item(
                "content://media/external/images/media/501",
                "IMG_0501.jpg",
                "image/jpeg",
                DiscoveryPhotoRef.UNKNOWN_TIME,
                "성남",
                false,
                false,
                "KR",
                "대한민국",
                "경기도",
                "경기도 성남시");

        DiscoverySnapshot snapshot = DiscoverySnapshotMapper.fromSourceItems(
                Arrays.asList(item),
                1,
                16L,
                1786000000000L,
                "unknown-time",
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);

        DiscoveryMemoryGroup group = snapshot.groups.get(0);
        assertEquals(DiscoveryPhotoRef.UNKNOWN_TIME, group.photoRefs.get(0).takenAtMillis);
        assertEquals(0L, group.startDateMillis);
        assertEquals(0L, group.endDateMillis);
    }

    @Test
    public void trimsLocationKeyBeforeGrouping() {
        DiscoverySnapshotMapper.SourceItem spaced = item(
                "content://media/external/images/media/601",
                "IMG_0601.jpg",
                "image/jpeg",
                1785600000000L,
                " 송파구 ",
                false,
                false,
                "KR",
                "대한민국",
                "서울특별시",
                "서울특별시 송파구");
        DiscoverySnapshotMapper.SourceItem clean = item(
                "content://media/external/images/media/602",
                "IMG_0602.jpg",
                "image/jpeg",
                1785686400000L,
                "송파구",
                false,
                false,
                "KR",
                "대한민국",
                "서울특별시",
                "서울특별시 송파구");

        DiscoverySnapshot snapshot = DiscoverySnapshotMapper.fromSourceItems(
                Arrays.asList(spaced, clean),
                2,
                17L,
                1786000000000L,
                "trim-key",
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);

        assertEquals(1, snapshot.groupCount());
        assertEquals("송파구", snapshot.groups.get(0).placeKey);
        assertEquals(2, snapshot.groups.get(0).itemCount);
    }

    private static DiscoverySnapshotMapper.SourceItem item(String uri,
                                                           String name,
                                                           String mimeType,
                                                           long takenAtMillis,
                                                           String locationKey,
                                                           boolean noLocation,
                                                           boolean video,
                                                           String countryCode,
                                                           String countryName,
                                                           String adminArea,
                                                           String addressLine) {
        return new DiscoverySnapshotMapper.SourceItem(
                uri,
                name,
                mimeType,
                takenAtMillis,
                locationKey,
                noLocation,
                video,
                countryCode,
                countryName,
                adminArea,
                addressLine);
    }
}

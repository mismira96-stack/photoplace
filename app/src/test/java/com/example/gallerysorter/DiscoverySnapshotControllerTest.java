package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;

public class DiscoverySnapshotControllerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savePreviewItemsPersistsDiscoverySnapshot() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("snapshot"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));

        boolean saved = controller.saveSourceItems(
                Arrays.asList(sourceItem("content://media/external/images/media/101", "삿포로")),
                1,
                "selected-folders");

        DiscoverySnapshot snapshot = store.read();
        assertTrue(saved);
        assertEquals(1786000000000L, snapshot.snapshotVersion);
        assertEquals(1786000000000L, snapshot.createdAtMillis);
        assertEquals("selected-folders", snapshot.sourceSignature);
        assertEquals(1, snapshot.groupCount());
        assertEquals("discovery:삿포로", snapshot.groups.get(0).memoryKey);
        assertEquals("content://media/external/images/media/101", snapshot.groups.get(0).coverUri);
    }

    @Test
    public void loadBrowserStateShowsOnlyDiscoveryBackedMemories() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("browser"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));
        controller.saveSourceItems(
                Arrays.asList(sourceItem("content://media/external/images/media/101", "삿포로")),
                1,
                "selected-folders");

        MemoryBrowserState state = controller.loadBrowserState(Arrays.asList(organizedAlbum()));

        assertFalse(state.isEmpty());
        assertEquals(1, state.items.size());
        assertEquals("삿포로", state.items.get(0).title);
    }

    @Test
    public void loadBrowserDetailUsesDiscoveryKeyWhenAlbumAlsoExists() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("detail"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));
        controller.saveSourceItems(
                Arrays.asList(sourceItem("content://media/external/images/media/101", "삿포로")),
                1,
                "selected-folders");

        MemoryBrowserDetail detail = controller.loadBrowserDetail(
                "discovery:삿포로",
                Arrays.asList(matchingOrganizedAlbum()));

        assertNotNull(detail);
        assertEquals("discovery:삿포로", detail.item.memoryKey);
        assertEquals("사진 1장", detail.item.countText);
        assertFalse(detail.canOpenGalleryAlbum);
        assertEquals(1, detail.sourceUris.size());
    }

    @Test
    public void sequentialSourceSavesAccumulateDiscoveryItems() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("merge"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));
        controller.saveSourceItems(
                Arrays.asList(sourceItem("content://media/external/images/media/101", "삿포로")),
                1,
                "camera");
        controller.saveSourceItems(
                Arrays.asList(sourceItem("content://media/external/images/media/202", "오타루")),
                1,
                "download");

        DiscoverySnapshot snapshot = store.read();

        assertEquals(2, snapshot.groupCount());
        assertEquals(2, snapshot.sourceItemCount);
    }

    @Test
    public void updateCountsPlacesWithNewMediaSeparatelyFromDiscoveredFiles() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("update"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));

        DiscoverySnapshotUpdate first = controller.saveSourceItemsWithResult(
                Arrays.asList(
                        sourceItem("content://media/external/images/media/101", "삿포로"),
                        sourceItem("content://media/external/images/media/102", "삿포로")),
                2,
                "camera");
        DiscoverySnapshotUpdate second = controller.saveSourceItemsWithResult(
                Arrays.asList(
                        sourceItem("content://media/external/images/media/202", "삿포로"),
                        sourceItem("content://media/external/images/media/203", "오타루")),
                2,
                "download");

        assertTrue(first.saved);
        assertEquals(2, first.discoveredItemCount);
        assertEquals(1, first.discoveredPlaceCount);
        assertEquals(1, first.newPlaceCount);
        assertTrue(second.saved);
        assertEquals(2, second.discoveredItemCount);
        assertEquals(2, second.discoveredPlaceCount);
        assertEquals(2, second.newPlaceCount);
    }

    @Test
    public void updateDoesNotMarkSameMediaAsNewOnRepeatedAnalysis() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("repeat"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));
        java.util.List<DiscoverySnapshotMapper.SourceItem> items = Arrays.asList(
                sourceItem("content://media/external/images/media/101", "안성"));

        DiscoverySnapshotUpdate first = controller.saveSourceItemsWithResult(items, 1, "camera");
        DiscoverySnapshotUpdate repeated = controller.saveSourceItemsWithResult(items, 1, "camera");

        assertEquals(1, first.newPlaceCount);
        assertEquals(0, repeated.newPlaceCount);
    }

    @Test
    public void updateCountsOneExistingPlaceWhenOnlyOneNewUriWasAdded() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("existing-place-new-photo"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));
        DiscoverySnapshotMapper.SourceItem original = sourceItem("content://media/external/images/media/101", "안성");
        controller.saveSourceItems(Arrays.asList(original), 1, "band");

        DiscoverySnapshotUpdate update = controller.saveSourceItemsWithResult(Arrays.asList(
                original,
                sourceItem("content://media/external/images/media/102", "안성")), 2, "band");

        assertEquals(2, update.discoveredItemCount);
        assertEquals(1, update.discoveredPlaceCount);
        assertEquals(1, update.newPlaceCount);
    }

    @Test
    public void saveKeepsExistingDiscoveryRefWhenReanalysisMarksItDuplicate() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("duplicate"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));
        String uri = "content://media/external/images/media/101";
        controller.saveSourceItems(Arrays.asList(sourceItem(uri, "안성")), 1, "band");

        controller.saveSourceItems(Arrays.asList(new DiscoverySnapshotMapper.SourceItem(
                uri, "IMG.jpg", "image/jpeg", 1785600000000L, "안성", false, true,
                false, "KR", "대한민국", "경기도", "안성")), 1, "band");

        DiscoverySnapshot snapshot = store.read();
        assertEquals(1, snapshot.groupCount());
        assertEquals(1, snapshot.groups.get(0).itemCount);
    }

    private static DiscoverySnapshotController.Clock fixedClock(final long nowMillis) {
        return new DiscoverySnapshotController.Clock() {
            @Override
            public long nowMillis() {
                return nowMillis;
            }
        };
    }

    private static DiscoverySnapshotMapper.SourceItem sourceItem(String uri, String locationKey) {
        return new DiscoverySnapshotMapper.SourceItem(
                uri,
                "IMG.jpg",
                "image/jpeg",
                1785600000000L,
                locationKey,
                false,
                false,
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan");
    }

    private static StoredAlbumSummary organizedAlbum() {
        return new StoredAlbumSummary(
                "오타루에서",
                "Pictures/오타루에서/",
                3,
                "2026-08-03",
                "2026-08-03",
                "content://thumbnail",
                "2026-08-15 10:00:00",
                1786000000000L,
                "JP",
                "Japan",
                "Hokkaido",
                "Otaru, Hokkaido, Japan");
    }

    private static StoredAlbumSummary matchingOrganizedAlbum() {
        return new StoredAlbumSummary(
                "삿포로에서",
                "Pictures/삿포로에서/",
                300,
                "2026-08-01",
                "2026-08-10",
                "content://thumbnail",
                "2026-08-15 10:00:00",
                1786000000000L,
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan");
    }
}

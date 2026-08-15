package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void loadBrowserStateCombinesSavedDiscoveryWithOrganizedAlbums() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("browser"));
        DiscoverySnapshotController controller = new DiscoverySnapshotController(store, fixedClock(1786000000000L));
        controller.saveSourceItems(
                Arrays.asList(sourceItem("content://media/external/images/media/101", "삿포로")),
                1,
                "selected-folders");

        MemoryBrowserState state = controller.loadBrowserState(Arrays.asList(organizedAlbum()));

        assertFalse(state.isEmpty());
        assertEquals(2, state.items.size());
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
}

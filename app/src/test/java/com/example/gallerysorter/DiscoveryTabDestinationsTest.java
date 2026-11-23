package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class DiscoveryTabDestinationsTest {
    @Test public void settingsStillOpensSettingsInsteadOfLegacyAlbums() {
        assertEquals(3, DiscoveryTabDestinations.count());
        assertEquals(0, DiscoveryTabDestinations.destinationAt(0));
        assertEquals(1, DiscoveryTabDestinations.destinationAt(1));
        assertEquals(3, DiscoveryTabDestinations.destinationAt(2));
    }

    @Test public void albumFilterBelongsToDiscoveryWhileSettingsRemainsSeparate() {
        assertTrue(DiscoveryTabDestinations.selected(1, 2));
        assertFalse(DiscoveryTabDestinations.selected(2, 2));
        assertTrue(DiscoveryTabDestinations.selected(2, 3));
        assertFalse(DiscoveryTabDestinations.selected(1, 3));
        assertFalse(DiscoveryTabDestinations.selected(0, -1));
    }
}

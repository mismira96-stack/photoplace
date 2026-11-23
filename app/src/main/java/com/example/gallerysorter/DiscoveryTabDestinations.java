package com.example.gallerysorter;

/** Visible tab positions are independent of the existing navigation/back-stack IDs. */
final class DiscoveryTabDestinations {
    static final int HOME = 0;
    static final int ALL = 1;
    static final int ALBUMS = 2;
    static final int SETTINGS = 3;
    private static final int[] VISIBLE = {HOME, ALL, SETTINGS};

    static int count() { return VISIBLE.length; }
    static int destinationAt(int position) { return VISIBLE[position]; }
    // Existing detailBackTarget ID for returning from the retained album-management screen.
    static int albumManagementBackTarget() { return 1; }
    static boolean selected(int position, int destination) {
        return destinationAt(position) == (destination == ALBUMS ? ALL : destination);
    }
    private DiscoveryTabDestinations() { }
}

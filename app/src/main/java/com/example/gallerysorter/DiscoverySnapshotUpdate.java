package com.example.gallerysorter;

final class DiscoverySnapshotUpdate {
    final boolean saved;
    final int discoveredItemCount;
    final int discoveredPlaceCount;
    final int newPlaceCount;

    DiscoverySnapshotUpdate(boolean saved,
                            int discoveredItemCount,
                            int discoveredPlaceCount,
                            int newPlaceCount) {
        this.saved = saved;
        this.discoveredItemCount = Math.max(0, discoveredItemCount);
        this.discoveredPlaceCount = Math.max(0, discoveredPlaceCount);
        this.newPlaceCount = Math.max(0, newPlaceCount);
    }
}

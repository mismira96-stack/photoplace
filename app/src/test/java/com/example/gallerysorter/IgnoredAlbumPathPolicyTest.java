package com.example.gallerysorter;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IgnoredAlbumPathPolicyTest {

    @Test
    public void ignoresCameraQuickBeautyAndSystemSocialFolders() {
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath(null));
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("DCIM/Camera/"));
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("DCIM/QuickBeauty/"));
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("Pictures/quickbeauty/Edited/"));
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("DCIM/Screenshots/"));
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("Pictures/KakaoTalk/"));
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("Pictures/Instagram/"));
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("Pictures/Download/"));
        assertTrue(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("Pictures/사진정리/2026/"));
    }

    @Test
    public void allowsNormalDestinationPlaceAlbumFolders() {
        assertFalse(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("Pictures/도쿄에서/"));
        assertFalse(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("Pictures/부산에서/"));
        assertFalse(IgnoredAlbumPathPolicy.isIgnoredAlbumPath("DCIM/RestoredTrip/"));
    }
}

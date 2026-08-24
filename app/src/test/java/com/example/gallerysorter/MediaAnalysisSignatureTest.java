package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class MediaAnalysisSignatureTest {
    @Test
    public void sameMetadataBuildsSameSignature() {
        String uri = "content://media/external/images/media/10";

        String first = MediaAnalysisSignature.build(uri, "IMG_0001.jpg", 100L, 90L, 80L, false, "Downloads");
        String second = MediaAnalysisSignature.build(uri, "IMG_0001.jpg", 100L, 90L, 80L, false, "Downloads");

        assertEquals(first, second);
    }

    @Test
    public void fileModificationInvalidatesSignature() {
        String uri = "content://media/external/images/media/10";

        String before = MediaAnalysisSignature.build(uri, "IMG_0001.jpg", 100L, 90L, 80L, false, "Downloads");
        String after = MediaAnalysisSignature.build(uri, "IMG_0001.jpg", 101L, 90L, 80L, false, "Downloads");

        assertNotEquals(before, after);
    }

    @Test
    public void mediaTypeInvalidatesSignature() {
        String uri = "content://media/external/files/media/10";

        String image = MediaAnalysisSignature.build(uri, "MOV_0001.mp4", 100L, 90L, 80L, false, "Downloads");
        String video = MediaAnalysisSignature.build(uri, "MOV_0001.mp4", 100L, 90L, 80L, true, "Downloads");

        assertNotEquals(image, video);
    }

    @Test
    public void sourceFolderInvalidatesAnalysisSignature() {
        String uri = "content://media/external/images/media/10";

        String downloads = MediaAnalysisSignature.build(uri, "IMG_0001.jpg", 100L, 90L, 80L, false, "Downloads");
        String camera = MediaAnalysisSignature.build(uri, "IMG_0001.jpg", 100L, 90L, 80L, false, "Camera");

        assertNotEquals(downloads, camera);
    }

    @Test
    public void noLocationSignatureKeepsBackwardCompatiblePrefix() {
        String uri = "content://media/external/images/media/10";

        String signature = MediaAnalysisSignature.buildForNoLocation(uri, "IMG_0001.jpg", 100L, 90L, 80L, false);

        assertEquals("i|content://media/external/images/media/10|IMG_0001.jpg|100|90|80|", signature);
    }

    @Test
    public void detailedAnalysisSignatureInvalidatesForSizePathAndPolicy() {
        String uri = "content://media/external/images/media/10";
        String original = MediaAnalysisSignature.buildForMediaAnalysis(10L, null, "IMG_0001.jpg", 100L, 90L, 80L, 1000L, 0L, false, "DCIM/Camera/", 1);
        String resized = MediaAnalysisSignature.buildForMediaAnalysis(10L, null, "IMG_0001.jpg", 100L, 90L, 80L, 2000L, 0L, false, "DCIM/Camera/", 1);
        String moved = MediaAnalysisSignature.buildForMediaAnalysis(10L, null, "IMG_0001.jpg", 100L, 90L, 80L, 1000L, 0L, false, "Download/", 1);
        String policyChanged = MediaAnalysisSignature.buildForMediaAnalysis(10L, null, "IMG_0001.jpg", 100L, 90L, 80L, 1000L, 0L, false, "DCIM/Camera/", 2);

        assertNotEquals(original, resized);
        assertNotEquals(original, moved);
        assertNotEquals(original, policyChanged);
    }
}

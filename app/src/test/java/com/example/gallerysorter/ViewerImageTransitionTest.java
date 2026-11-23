package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ViewerImageTransitionTest {
    @Test public void uncachedNextPhotoKeepsCurrentImageUntilDecodeCompletesWithoutBlankFrame() {
        List<String> frames = new ArrayList<>(Arrays.asList("current"));
        ViewerImageTransition<String> transition = new ViewerImageTransition<>(true, null,
                frames::add, () -> frames.add("blank"));
        transition.pending();
        assertEquals(Arrays.asList("current"), frames);
        transition.ready("next");
        assertEquals(Arrays.asList("current", "next"), frames);
    }

    @Test public void navigationDoesNotFlashLowResolutionPreviewBetweenTwoFullImages() {
        List<String> frames = new ArrayList<>(Arrays.asList("current-full"));
        ViewerImageTransition<String> transition = new ViewerImageTransition<>(true, "next-small",
                frames::add, () -> frames.add("blank"));
        transition.pending();
        transition.ready("next-full");
        assertEquals(Arrays.asList("current-full", "next-full"), frames);
    }

    @Test public void firstEntryStillShowsItsOwnPreviewBeforeFullImage() {
        List<String> frames = new ArrayList<>();
        ViewerImageTransition<String> transition = new ViewerImageTransition<>(false, "selected-small",
                frames::add, () -> frames.add("blank"));
        transition.pending();
        transition.ready("selected-full");
        assertEquals(Arrays.asList("selected-small", "selected-full"), frames);
    }

    @Test public void failedOrNullDecodeNeverLeavesPreviousPhotoAsTheSelectedPhoto() {
        List<String> frames = new ArrayList<>(Arrays.asList("previous"));
        ViewerImageTransition<String> noPreview = new ViewerImageTransition<>(true, null,
                frames::add, () -> frames.add("unavailable"));
        noPreview.pending();
        noPreview.ready(null);
        assertEquals(Arrays.asList("previous", "unavailable"), frames);
        ViewerImageTransition<String> withPreview = new ViewerImageTransition<>(true, "selected-small",
                frames::add, () -> fail("Valid preview was discarded"));
        withPreview.pending();
        withPreview.failed();
        assertEquals("selected-small", frames.get(frames.size() - 1));
    }
}

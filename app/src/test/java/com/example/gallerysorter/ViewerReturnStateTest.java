package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class ViewerReturnStateTest {
    @Test public void sameRevisionReturnsExactScreenOnceKeepingItsExistingGridState() {
        ViewerReturnState<Object> state = new ViewerReturnState<>();
        Object originalScreen = new Object();
        state.remember(originalScreen, "media10-local20");
        assertTrue(state.hasScreen());
        assertSame(originalScreen, state.take("media10-local20"));
        assertNull(state.take("media10-local20"));
        assertFalse(state.hasScreen());
    }

    @Test public void mediaAndMetadataChangesOrUnknownRevisionRequireRebuild() {
        for (String changed : new String[]{"media11-local20", "media10-local21", "", null}) {
            ViewerReturnState<Object> state = new ViewerReturnState<>();
            state.remember(new Object(), "media10-local20");
            assertNull(state.take(changed));
            assertFalse(state.hasScreen());
        }
    }

    @Test public void backgroundOrConfigurationInvalidationReleasesOldScreen() {
        ViewerReturnState<Object> state = new ViewerReturnState<>();
        state.remember(new Object(), "revision");
        state.clear();
        assertFalse(state.hasScreen());
        assertNull(state.take("revision"));
    }

    @Test public void unknownCaptureDoesNotRetainOrReusePreviousScreen() {
        for (String unknown : new String[]{"", null}) {
            ViewerReturnState<Object> state = new ViewerReturnState<>();
            state.remember(new Object(), "old");
            state.remember(new Object(), unknown);
            assertFalse(state.hasScreen());
            assertNull(state.take("old"));
        }
    }

    @Test public void detailMustStayStableDuringBuildAndUntilReturnNotOnlySinceViewerOpened() {
        ViewerReturnState<Object> state = new ViewerReturnState<>();
        Object grid = new Object();
        state.remember(grid, HomeDashboardPolicy.stableRevision("before-build", "after-build"));
        assertFalse(state.hasScreen());
        state.remember(grid, HomeDashboardPolicy.stableRevision("rendered-v1", "rendered-v1"));
        // Media changed after the grid was shown, before the user opened the viewer.
        assertNull(state.take("viewer-open-v2"));
    }
}

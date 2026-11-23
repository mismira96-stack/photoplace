package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryLoadNavigationTest {
    @Test public void countryLoadCanBeCancelledByBackAndCannotBeOverriddenByResume() {
        boolean[] countryPending = {true}; int[] cancels = {0};
        MemoryLoadNavigation navigation = new MemoryLoadNavigation(() -> false, () -> { }, () -> false, () -> { },
                () -> countryPending[0], () -> { countryPending[0] = false; cancels[0]++; });
        navigation.whenIdle(() -> fail("Resume must not replace the pending country with Home"));
        navigation.back(() -> fail("Back must cancel pending country navigation first"));
        assertFalse(countryPending[0]); assertEquals(1, cancels[0]);
    }

    @Test public void returningToDiscoveryCancelsAnyPendingCountryScreen() {
        int[] countries = {0};
        MemoryLoadNavigation navigation = new MemoryLoadNavigation(() -> false, () -> { }, () -> false, () -> { },
                () -> true, () -> countries[0]++);
        navigation.switchTab(1, () -> { });
        assertEquals(1, countries[0]);
    }
    @Test public void discoveryTabCanKeepPreloadButOtherTabsCancelBothLoads() {
        int[] cancels = {0, 0, 0};
        MemoryLoadNavigation navigation = new MemoryLoadNavigation(() -> false, () -> cancels[0]++,
                () -> false, () -> cancels[1]++);

        navigation.switchTab(1, () -> cancels[2]++);
        assertArrayEquals(new int[]{0, 1, 1}, cancels);
        navigation.switchTab(2, () -> cancels[2]++);
        assertArrayEquals(new int[]{1, 2, 1}, cancels);
    }

    @Test public void onlyMatchingPreloadCanPublishToForegroundCallbacks() {
        MemoryScreenLoadController.PendingLoad<String> request = new MemoryScreenLoadController.PendingLoad<>(
                "browser|albums", (value, revision) -> fail("Quiet callback should be replaced"),
                message -> fail("Quiet error should be replaced"), false);
        String[] published = {null, null};
        assertFalse(request.promote("browser|discovery", (value, revision) -> fail(), message -> fail()));
        assertFalse(request.foreground);
        assertTrue(request.promote("browser|albums", (value, revision) -> published[0] = value,
                message -> published[1] = message));
        assertTrue(request.foreground);
        request.ready.accept("entry", "revision");
        request.error.accept("message");
        assertArrayEquals(new String[]{"entry", "message"}, published);
    }

    @Test public void resumeDoesNotRestoreHomeOverPendingIntentNavigation() {
        boolean[] pending={true};int[] restored={0};
        MemoryLoadNavigation navigation=new MemoryLoadNavigation(()->false,()->{},()->pending[0],()->{});
        navigation.whenIdle(()->restored[0]++);assertEquals(0,restored[0]);
        pending[0]=false;navigation.whenIdle(()->restored[0]++);assertEquals(1,restored[0]);
    }
    @Test public void backDuringBrowserOrDetailLoadingCancelsInsteadOfLeavingOriginScreen() {
        for (boolean browser : new boolean[]{true, false}) {
            int[] cancels = {0};
            MemoryLoadNavigation navigation = new MemoryLoadNavigation(() -> browser, () -> cancels[0]++,
                    () -> !browser, () -> cancels[0]++);
            navigation.back(() -> fail("Back exited the source screen instead of cancelling entry"));
            assertEquals(2, cancels[0]);
        }
    }

    @Test public void normalBackIsUnchangedWithoutPendingEntry() {
        int[] backs = {0};
        new MemoryLoadNavigation(() -> false, () -> fail(), () -> false, () -> fail())
                .back(() -> backs[0]++);
        assertEquals(1, backs[0]);
    }

    @Test public void selectingEvenTheAlreadyVisibleHomeTabCancelsBothPendingTargets() {
        boolean[] pending = {true, true};
        MemoryLoadNavigation navigation = new MemoryLoadNavigation(() -> pending[0], () -> pending[0] = false,
                () -> pending[1], () -> pending[1] = false);
        navigation.cancel();
        assertFalse(pending[0]);
        assertFalse(pending[1]);
    }
}

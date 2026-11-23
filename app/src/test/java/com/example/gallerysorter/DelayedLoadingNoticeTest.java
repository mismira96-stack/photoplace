package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class DelayedLoadingNoticeTest {
    private static final class Clock implements DelayedLoadingNotice.Scheduler {
        Runnable queued;
        long delay;
        @Override public void after(long millis, Runnable action) { delay = millis; queued = action; }
        @Override public void remove(Runnable action) { if (queued == action) queued = null; }
    }

    @Test public void fastCompletionOrNavigationCannotFlashNoticeEvenIfCallbackWasAlreadyQueued() {
        Clock clock = new Clock();
        DelayedLoadingNotice notice = new DelayedLoadingNotice(clock, 180);
        notice.start(() -> fail("Notice after completed/cancelled query"));
        Runnable alreadyQueued = clock.queued;
        notice.cancel();
        assertNull(clock.queued);
        alreadyQueued.run();
    }

    @Test public void slowRequestShowsAfterGracePeriodAndNewRequestSupersedesOldOne() {
        Clock clock = new Clock();
        DelayedLoadingNotice notice = new DelayedLoadingNotice(clock, 180);
        notice.start(() -> fail("Old request notice"));
        Runnable old = clock.queued;
        int[] shown = {0};
        notice.start(() -> shown[0]++);
        assertEquals(180, clock.delay);
        assertEquals(0, shown[0]);
        old.run();
        assertEquals(0, shown[0]);
        clock.queued.run();
        assertEquals(1, shown[0]);
        notice.cancel();
    }
}

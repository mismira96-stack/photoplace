package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorkingProgressPresentationTest {
    @Test public void idleScreenCanEnterPreparationThenShowNumericProgressAndHideOnCompletion() {
        assertFalse(state(false, 0, 0).visible);
        WorkingProgressPresentation preparing = state(true, 0, 0);
        assertTrue(preparing.visible);
        assertTrue(preparing.indeterminate);
        WorkingProgressPresentation running = state(true, 42, 200);
        assertTrue(running.visible);
        assertFalse(running.indeterminate);
        assertEquals(21, running.percent);
        assertEquals("42 / 200 완료 · 21%", running.detail);
        assertFalse(state(false, 200, 200).visible);
        assertTrue(state(true, 0, 0).indeterminate);
    }

    @Test public void backgroundReturnUsesCurrentWorkerNumbers() {
        WorkingProgressPresentation returned = state(true, 999, 2000);
        assertTrue(returned.visible);
        assertEquals(999, returned.current);
        assertEquals(2000, returned.total);
        assertEquals(50, returned.percent);
    }

    @Test public void invalidOrOvershootingValuesNeverProduceInvalidPercent() {
        assertEquals(0, state(true, -1, 10).percent);
        assertEquals(100, state(true, 11, 10).percent);
        assertTrue(state(true, 10, -1).indeterminate);
        assertEquals(100, state(true, Integer.MAX_VALUE, Integer.MAX_VALUE).percent);
    }

    @Test public void preparationRetainsWorkingMessageUntilWorkerHasALabel() {
        WorkingProgressPresentation preparing = WorkingProgressPresentation.from(true, "앨범으로 정리하는 중...", null, 0, 0);
        assertEquals("앨범으로 정리하는 중...", preparing.title);
        assertEquals("앨범으로 정리 중", state(true, 1, 2).title);
        assertEquals("작업 진행 중", WorkingProgressPresentation.from(true, null, " ", 0, 0).title);
    }

    private static WorkingProgressPresentation state(boolean active, int current, int total) {
        return WorkingProgressPresentation.from(active, "준비", "앨범으로 정리 중", current, total);
    }
}

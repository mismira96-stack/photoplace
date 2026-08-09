package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MemoryPersonalizationTest {
    @Test
    public void keyUsesRelativePathBeforeAlbumName() {
        StoredAlbumSummary summary = summary("삿포로에서", "Pictures/2026 삿포로/");

        assertEquals("path:Pictures/2026 삿포로/", MemoryPersonalizationKey.forSummary(summary));
    }

    @Test
    public void keyFallsBackToAlbumName() {
        StoredAlbumSummary summary = summary("삿포로에서", "");

        assertEquals("album:삿포로에서", MemoryPersonalizationKey.forSummary(summary));
    }

    @Test
    public void emptyPersonalizationHasNoUserFields() {
        MemoryPersonalization personalization = MemoryPersonalization.empty("path:Pictures/삿포로/");

        assertTrue(personalization.isEmpty());
        assertEquals("", personalization.displayName);
        assertEquals("", personalization.memo);
        assertEquals("", personalization.userCoverUri);
    }

    @Test
    public void partialUpdatesKeepOtherFields() {
        MemoryPersonalization original = new MemoryPersonalization(
                "path:Pictures/삿포로/",
                "2026 삿포로 여행",
                "여름 휴가",
                "content://cover",
                1L);

        MemoryPersonalization updated = original.withMemo("다음에는 겨울", 2L);

        assertEquals(original.displayName, updated.displayName);
        assertEquals("다음에는 겨울", updated.memo);
        assertEquals(original.userCoverUri, updated.userCoverUri);
        assertEquals(2L, updated.updatedAtMillis);
    }

    @Test
    public void updateWithoutJsonRecordStartsFromLegacyMemo() {
        MemoryPersonalization current = MemoryPersonalizationStore.personalizationForUpdate(
                "path:Pictures/삿포로/",
                null,
                false,
                "기존 메모");

        MemoryPersonalization updated = current.withDisplayName("2026 삿포로 여행", 2L);

        assertEquals("2026 삿포로 여행", updated.displayName);
        assertEquals("기존 메모", updated.memo);
    }

    @Test
    public void updateWithJsonRecordDoesNotReviveLegacyMemo() {
        MemoryPersonalization existing = new MemoryPersonalization(
                "path:Pictures/삿포로/",
                "2026 삿포로 여행",
                "",
                "",
                1L);

        MemoryPersonalization current = MemoryPersonalizationStore.personalizationForUpdate(
                existing.memoryKey,
                existing,
                true,
                "옛날 메모");

        assertEquals("2026 삿포로 여행", current.displayName);
        assertEquals("", current.memo);
    }

    private static StoredAlbumSummary summary(String albumName, String relativePath) {
        return new StoredAlbumSummary(
                albumName,
                relativePath,
                12,
                "2026-08-02",
                "2026-08-06",
                "content://thumbnail",
                null,
                123L,
                "일본",
                "홋카이도",
                "札幌市中央区");
    }
}

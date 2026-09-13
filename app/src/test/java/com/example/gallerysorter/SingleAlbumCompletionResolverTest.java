package com.example.gallerysorter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SingleAlbumCompletionResolverTest {
    private final SingleAlbumCompletionResolver resolver = new SingleAlbumCompletionResolver();

    @Test
    public void returnsSummaryForOneAlbumAndUsesPhotoCover() {
        SingleAlbumCompletionResolver.Summary summary = resolver.resolve(Arrays.asList(
                new SingleAlbumCompletionResolver.Item("place-a", "예술의전당에서",
                        new Date(1703376000000L), "content://photo/1", false),
                new SingleAlbumCompletionResolver.Item("place-a", "예술의전당에서",
                        new Date(1715472000000L), "content://video/1", true)));

        assertEquals("예술의전당에서", summary.albumName);
        assertEquals(2, summary.itemCount);
        assertEquals("2023.12.24 ~ 2024.05.12", summary.dateRange);
        assertEquals("content://photo/1", summary.coverUri);
    }

    @Test
    public void returnsNullForMultipleAlbums() {
        assertNull(resolver.resolve(Arrays.asList(
                new SingleAlbumCompletionResolver.Item("place-a", "성남에서", null, "a", false),
                new SingleAlbumCompletionResolver.Item("place-b", "송파에서", null, "b", false))));
    }

    @Test
    public void returnsNullForNoCompletedItems() {
        assertNull(resolver.resolve(Collections.<SingleAlbumCompletionResolver.Item>emptyList()));
        assertNull(resolver.resolve(null));
    }
}

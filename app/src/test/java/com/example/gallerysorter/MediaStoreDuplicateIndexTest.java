package com.example.gallerysorter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class MediaStoreDuplicateIndexTest {
    @Test
    public void matchesExactAndCopySuffixNames() {
        Set<String> names = new HashSet<>();
        names.add("e|img_0001.jpg");
        names.add("s|img_0001.jpg");

        assertTrue(MediaStoreDuplicateIndex.containsNames(names, "IMG_0001.jpg"));
        assertTrue(MediaStoreDuplicateIndex.containsNames(names, "IMG_0001 (2).jpg"));
        assertFalse(MediaStoreDuplicateIndex.containsNames(names, "IMG_0002.jpg"));
    }
}

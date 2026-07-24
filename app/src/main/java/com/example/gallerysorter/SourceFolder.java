package com.example.gallerysorter;

final class SourceFolder {
    final int count;
    final String displayName;
    final String relativePath;

    SourceFolder(String relativePath, String displayName, int count) {
        this.relativePath = relativePath;
        this.displayName = displayName;
        this.count = count;
    }
}

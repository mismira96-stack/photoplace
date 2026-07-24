package com.example.gallerysorter;

final class AlbumFolder {
    final String folderName;
    final String matchName;
    final String relativePath;

    AlbumFolder(String relativePath, String folderName, String matchName) {
        this.relativePath = relativePath;
        this.folderName = folderName;
        this.matchName = matchName;
    }
}

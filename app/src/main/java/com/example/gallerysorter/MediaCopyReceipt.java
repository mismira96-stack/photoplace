package com.example.gallerysorter;

import android.net.Uri;

/** Returned only after the existing copy/move completion path succeeds. */
final class MediaCopyReceipt {
    final Uri targetUri;
    final long copiedBytes;

    MediaCopyReceipt(Uri targetUri, long copiedBytes) {
        this.targetUri = targetUri;
        this.copiedBytes = copiedBytes;
    }
}

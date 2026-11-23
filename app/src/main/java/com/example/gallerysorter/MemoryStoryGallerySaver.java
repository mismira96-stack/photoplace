package com.example.gallerysorter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/** Explicit Save only. Creates a new composed image; never modifies source photos or their dates. */
final class MemoryStoryGallerySaver {
    static final String ALBUM_PATH = "Pictures/PhotoPlace Stories/";
    static Uri save(Context context, Bitmap card) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoPlace_" + UUID.randomUUID() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, ALBUM_PATH);
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri created = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (created == null) throw new IOException("저장 공간을 확인해 주세요.");
        try {
            try (OutputStream out = resolver.openOutputStream(created, "w")) {
                if (out == null || !card.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw new IOException("카드를 저장하지 못했어요.");
                }
                out.flush();
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (resolver.update(created, ready, null, null) != 1) throw new IOException("저장을 완료하지 못했어요.");
            return created;
        } catch (IOException | RuntimeException failure) {
            // Only the new pending result owned by this invocation, never a source URI.
            try { resolver.delete(created, null, null); } catch (RuntimeException ignored) { }
            throw failure;
        }
    }
}

package com.example.gallerysorter;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Keep the fixed Story header and caption inside the visible area while the IME is open. */
final class MemoryStoryEditorInsets {
    private MemoryStoryEditorInsets() { }
    static void bind(View root) {
        int left=root.getPaddingLeft(),top=root.getPaddingTop(),right=root.getPaddingRight(),bottom=root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom+Math.max(bars.bottom,ime.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}

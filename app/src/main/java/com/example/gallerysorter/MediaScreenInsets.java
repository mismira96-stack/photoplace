package com.example.gallerysorter;

import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/** Keeps media controls above system navigation/gesture areas, including edge-to-edge windows. */
final class MediaScreenInsets {
    static void bind(View root) {
        final int left=root.getPaddingLeft(),top=root.getPaddingTop(),right=root.getPaddingRight(),bottom=root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                view.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom+bars.bottom);
            } else view.setPadding(left+insets.getSystemWindowInsetLeft(),top+insets.getSystemWindowInsetTop(),
                    right+insets.getSystemWindowInsetRight(),bottom+insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
    }
}

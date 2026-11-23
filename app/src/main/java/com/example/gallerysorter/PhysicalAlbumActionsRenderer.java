package com.example.gallerysorter;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;

/** Keep external gallery and legacy album-management entry points beside the common viewer. */
final class PhysicalAlbumActionsRenderer {
    static void addTo(LinearLayout root, Runnable gallery, Runnable manage) {
        LinearLayout row = new LinearLayout(root.getContext());
        add(row, "갤러리에서 보기 ↗", gallery);
        add(row, "앨범 관리", manage);
        root.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }
    private static void add(LinearLayout row, String label, Runnable action) {
        TextView button = CleanupUiStyle.text(row.getContext(), label, 13, false, CleanupUiStyle.PURPLE);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(CleanupUiStyle.dp(row.getContext(), 48));
        button.setBackground(MemorySelectionStyle.surface(row.getContext(), 0xfff3effb, 14));
        button.setOnClickListener(v -> action.run());
        button.setContentDescription(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(0, CleanupUiStyle.dp(row.getContext(), 8), CleanupUiStyle.dp(row.getContext(), 6),
                CleanupUiStyle.dp(row.getContext(), 10));
        row.addView(button, params);
    }
    private PhysicalAlbumActionsRenderer() { }
}

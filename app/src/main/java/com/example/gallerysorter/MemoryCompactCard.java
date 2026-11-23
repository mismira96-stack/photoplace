package com.example.gallerysorter;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

final class MemoryCompactCard {
    static TextView title(Context context, String label) {
        TextView view = CleanupUiStyle.text(context, label, 12, true, CleanupUiStyle.INK);
        view.setSingleLine();
        view.setEllipsize(TextUtils.TruncateAt.END);
        int horizontal = CleanupUiStyle.dp(context, 5);
        int vertical = CleanupUiStyle.dp(context, 4);
        view.setPadding(horizontal, vertical, horizontal, vertical);
        return view;
    }

    static void addCaption(LinearLayout card, String name, String count) {
        Context context = card.getContext();
        TextView title = title(context, name);
        if (count == null || count.isEmpty()) {
            if (count == null) title.setGravity(Gravity.CENTER);
            card.addView(title, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        int inset = CleanupUiStyle.dp(context, 5);
        int vertical = CleanupUiStyle.dp(context, 4);
        LinearLayout caption = new LinearLayout(context);
        caption.setGravity(Gravity.CENTER_VERTICAL);
        caption.setPaddingRelative(inset, vertical, inset, vertical);
        title.setPadding(0, 0, 0, 0);
        caption.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView label = CleanupUiStyle.text(context, "· " + count, 11, false, CleanupUiStyle.PURPLE);
        label.setSingleLine();
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setMaxEms(5);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-2, -2);
        countParams.setMarginStart(CleanupUiStyle.dp(context, 3));
        caption.addView(label, countParams);
        card.addView(caption, new LinearLayout.LayoutParams(-1, -2));
    }

    static int coverHeight(Context context, boolean wide, int columns) {
        return CleanupUiStyle.dp(context, columns == 4 ? (wide ? 82 : 54) : (wide ? 122 : 96));
    }

    static int albumCoverHeight(Context context, boolean wide, int columns) {
        return columns == 2 ? CleanupUiStyle.dp(context, 136) : coverHeight(context, wide, columns);
    }
}

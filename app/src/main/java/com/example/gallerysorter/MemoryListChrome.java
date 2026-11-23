package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared surfaces for the two record lists. */
final class MemoryListChrome {
    static final int BACKGROUND = 0xfff8f7fc;
    private static final int BORDER = 0xffe9e5f2;
    static final int SIDE_DP = 14;
    static final int TOP_DP = 40;
    static final int COMPACT_RADIUS_DP = 13;

    static GradientDrawable compactCard(Context context) {
        return surface(context, Color.WHITE, COMPACT_RADIUS_DP);
    }

    static View summary(Context context, String title, String meta) {
        return summary(context, title, meta, null, null);
    }

    static View summary(Context context, String title, String meta, View addPlaceAction, View collectAction) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 16), dp(context, 8), dp(context, 12), dp(context, 8));
        row.setBackground(surface(context, 0xfff4f1fc, 18));
        row.setElevation(dp(context, 1));
        row.setMinimumHeight(dp(context, 64));

        LinearLayout text = new LinearLayout(context);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView heading = CleanupUiStyle.text(context, title, 16, false, CleanupUiStyle.INK);
        heading.setTypeface(Typeface.create(Typeface.DEFAULT, 500, false));
        text.addView(heading);
        TextView subtitle = CleanupUiStyle.text(context, meta, 12, false, CleanupUiStyle.MUTED);
        text.addView(subtitle);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));

        if (addPlaceAction != null) {
            row.addView(addPlaceAction);
        }
        if (collectAction != null) {
            row.addView(collectAction);
        }
        if (addPlaceAction == null && collectAction == null) {
            ImageView pin = new ImageView(context);
            pin.setScaleType(ImageView.ScaleType.CENTER);
            pin.setImageDrawable(MediaVectorIconDrawable.place(CleanupUiStyle.PURPLE, dp(context, 17)));
            pin.setBackground(MemorySelectionStyle.surface(context, 0xffeae2fb, 10));
            pin.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context, 30), dp(context, 30));
            params.setMarginStart(dp(context, 8));
            row.addView(pin, params);
        }
        return row;
    }

    private static GradientDrawable surface(Context context, int color, int radius) {
        GradientDrawable background = MemorySelectionStyle.surface(context, color, radius);
        background.setStroke(dp(context, 1), BORDER);
        return background;
    }

    private static int dp(Context context, int value) {
        return CleanupUiStyle.dp(context, value);
    }
}

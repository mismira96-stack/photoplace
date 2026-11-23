package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.IntConsumer;

/** Two entry points sharing the existing all-records and physical-album screens. */
final class DiscoveryFilterRenderer {
    static void addTo(LinearLayout parent, boolean albums, IntConsumer navigate) {
        Context context = parent.getContext();
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(context, 10));
        add(row, "전체", !albums, DiscoveryTabDestinations.ALL, navigate);
        add(row, "위치 앨범", albums, DiscoveryTabDestinations.ALBUMS, navigate);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private static void add(LinearLayout row, String label, boolean selected,
                            int destination, IntConsumer navigate) {
        Context context = row.getContext();
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(14);
        chip.setTypeface(Typeface.create(Typeface.DEFAULT, selected ? 500 : 400, false));
        chip.setTextColor(selected ? Color.rgb(104, 82, 190) : Color.rgb(100, 111, 132));
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(context, 48));
        chip.setPadding(dp(context, 20), 0, dp(context, 20), 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(selected ? Color.rgb(239, 234, 252) : Color.WHITE);
        background.setCornerRadius(dp(context, 24));
        background.setStroke(dp(context, 1), selected ? Color.rgb(220, 211, 246) : Color.rgb(231, 234, 242));
        chip.setBackground(background);
        chip.setSelected(selected);
        chip.setContentDescription(label + (selected ? ", 선택됨" : ""));
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setOnClickListener(view -> { if (!selected) navigate.accept(destination); });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMarginEnd(dp(context, 8));
        row.addView(chip, params);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
    private DiscoveryFilterRenderer() { }
}

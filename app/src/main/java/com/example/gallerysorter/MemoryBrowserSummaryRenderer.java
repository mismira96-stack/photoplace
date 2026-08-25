package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

final class MemoryBrowserSummaryRenderer {
    private final Context context;

    MemoryBrowserSummaryRenderer(Context context) {
        this.context = context;
    }

    View render(List<MemoryRecord> records) {
        MemoryBrowserSummary summary = MemoryBrowserSummary.from(records);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(26), dp(14), dp(26), dp(14));
        card.setBackground(background());

        TextView title = text("발견한 장소 " + summary.placeCount + "곳", 16.0f, Color.rgb(31, 35, 48), true);
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView detail = text("사진 " + String.format(Locale.KOREA, "%,d", summary.photoCount) + "장 · " + summary.yearRange,
                13.0f, Color.rgb(104, 113, 132), false);
        detail.setPadding(0, dp(2), 0, 0);
        card.addView(detail, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(color);
        text.setIncludeFontPadding(false);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return text;
    }

    private GradientDrawable background() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(24));
        background.setStroke(dp(1), Color.rgb(231, 233, 239));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

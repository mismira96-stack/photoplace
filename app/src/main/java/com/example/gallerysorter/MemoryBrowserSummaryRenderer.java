package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import java.util.List;

final class MemoryBrowserSummaryRenderer {
    private final Context context;

    MemoryBrowserSummaryRenderer(Context context) {
        this.context = context;
    }

    View render(List<MemoryRecord> records) {
        MemoryBrowserSummary summary = MemoryBrowserSummary.from(records);
        TextView dateRange = new TextView(context);
        dateRange.setText(summary.yearRange);
        dateRange.setTextSize(12.0f);
        dateRange.setTextColor(Color.rgb(120, 129, 147));
        dateRange.setIncludeFontPadding(false);
        dateRange.setPadding(dp(2), 0, 0, 0);
        return dateRange;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

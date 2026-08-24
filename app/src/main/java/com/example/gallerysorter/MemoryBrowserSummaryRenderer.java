package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class MemoryBrowserSummaryRenderer {
    private final Context context;

    MemoryBrowserSummaryRenderer(Context context) {
        this.context = context;
    }

    View render(List<MemoryRecord> records) {
        int placeCount = 0;
        int photoCount = 0;
        long firstDate = 0L;
        long lastDate = 0L;
        if (records != null) {
            for (MemoryRecord record : records) {
                if (record == null) {
                    continue;
                }
                placeCount++;
                photoCount += Math.max(0, record.itemCount);
                long start = record.startDateMillis > 0L ? record.startDateMillis : record.endDateMillis;
                long end = record.endDateMillis > 0L ? record.endDateMillis : record.startDateMillis;
                if (start > 0L && (firstDate <= 0L || start < firstDate)) {
                    firstDate = start;
                }
                if (end > 0L && end > lastDate) {
                    lastDate = end;
                }
            }
        }

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(26), dp(14), dp(26), dp(14));
        card.setBackground(background());

        TextView title = text("발견한 장소 " + placeCount + "곳", 16.0f, Color.rgb(31, 35, 48), true);
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView detail = text("사진 " + String.format(Locale.KOREA, "%,d", photoCount) + "장 · " + yearRange(firstDate, lastDate),
                13.0f, Color.rgb(104, 113, 132), false);
        detail.setPadding(0, dp(2), 0, 0);
        card.addView(detail, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    private String yearRange(long firstDate, long lastDate) {
        if (firstDate <= 0L && lastDate <= 0L) {
            return "날짜 정보 없음";
        }
        long start = firstDate > 0L ? firstDate : lastDate;
        long end = lastDate > 0L ? lastDate : firstDate;
        String startYear = new SimpleDateFormat("yyyy", Locale.KOREA).format(new Date(start));
        String endYear = new SimpleDateFormat("yyyy", Locale.KOREA).format(new Date(end));
        return startYear.equals(endYear) ? startYear : startYear + " ~ " + endYear;
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

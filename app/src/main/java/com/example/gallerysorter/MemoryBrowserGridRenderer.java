package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

final class MemoryBrowserGridRenderer {
    interface Listener {
        void onMemorySelected(MemoryBrowserItem item);
    }

    interface ThumbnailLoader {
        void load(ImageView target, String uriValue, int sizePx);
    }

    private final Context context;
    private final Listener listener;
    private final ThumbnailLoader thumbnailLoader;

    MemoryBrowserGridRenderer(Context context, Listener listener, ThumbnailLoader thumbnailLoader) {
        this.context = context;
        this.listener = listener;
        this.thumbnailLoader = thumbnailLoader;
    }

    ScrollView.LayoutParams contentLayoutParams(int windowWidthPx) {
        ScrollView.LayoutParams params = new ScrollView.LayoutParams(-1, -2);
        if (windowWidthPx >= dp(600)) {
            params.width = Math.min(windowWidthPx, dp(600));
            params.gravity = Gravity.CENTER_HORIZONTAL;
        }
        return params;
    }

    int availableGridWidth(int windowWidthPx) {
        int contentWidth = windowWidthPx >= dp(600) ? Math.min(windowWidthPx, dp(600)) : windowWidthPx;
        return Math.max(dp(280), contentWidth - dp(36));
    }

    int columnCount(int windowWidthPx) {
        return windowWidthPx >= dp(600) ? 3 : 2;
    }

    View render(List<MemoryBrowserItem> items, int availableWidthPx, int columns) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        if (items == null || items.isEmpty()) {
            return grid;
        }

        columns = Math.max(2, Math.min(3, columns));
        int gap = dp(12);
        int cardWidth = Math.max(dp(132), (availableWidthPx - (gap * (columns - 1))) / columns);
        int imageHeight = columns == 3 ? dp(150) : dp(158);

        LinearLayout row = null;
        for (int index = 0; index < items.size(); index++) {
            if (index % columns == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            MemoryBrowserItem item = items.get(index);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, -2);
            params.setMargins(index % columns == 0 ? 0 : gap, 0, 0, gap);
            row.addView(createCard(item, cardWidth, imageHeight), params);
        }
        return grid;
    }

    View renderResults(List<MemoryBrowserItem> items,
                       int availableWidthPx,
                       int columns,
                       boolean searching) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        if (items == null || items.isEmpty()) {
            if (searching) {
                LinearLayout empty = new LinearLayout(context);
                empty.setOrientation(LinearLayout.VERTICAL);
                empty.setPadding(dp(16), dp(18), dp(16), dp(18));
                empty.setBackground(cardBackground());
                empty.addView(text("검색 결과가 없어요", 16.0f, Color.rgb(31, 35, 48), true));
                TextView hint = text("다른 장소명, 국가명, 도시명, 날짜로 다시 찾아보세요.", 13.0f, Color.rgb(104, 113, 132), false);
                hint.setPadding(0, dp(5), 0, 0);
                empty.addView(hint);
                container.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            }
            return container;
        }
        if (searching) {
            TextView count = text("검색 결과 " + items.size() + "개", 14.0f, Color.rgb(104, 82, 226), true);
            count.setPadding(dp(2), 0, 0, dp(10));
            container.addView(count, new LinearLayout.LayoutParams(-1, -2));
        }
        container.addView(render(items, availableWidthPx, columns), new LinearLayout.LayoutParams(-1, -2));
        return container;
    }

    private View createCard(final MemoryBrowserItem item, int cardWidth, int imageHeight) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setClipToOutline(true);
        card.setBackground(cardBackground());
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onMemorySelected(item);
                }
            }
        });

        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image, new LinearLayout.LayoutParams(-1, imageHeight));
        if (thumbnailLoader != null) {
            thumbnailLoader.load(image, item.coverUri, Math.max(cardWidth, imageHeight));
        }

        LinearLayout caption = new LinearLayout(context);
        caption.setOrientation(LinearLayout.VERTICAL);
        caption.setPadding(dp(12), dp(8), dp(10), dp(10));
        card.addView(caption, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        caption.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        TextView title = text(item.title, 15.0f, Color.rgb(31, 35, 48), true);
        singleLine(title);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        if (item.recentAddedCount > 0) {
            TextView badge = text("NEW", 10.0f, Color.rgb(104, 82, 226), true);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(6), dp(2), dp(6), dp(2));
            badge.setBackground(newBadgeBackground());
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
            badgeParams.setMargins(dp(4), 0, 0, 0);
            titleRow.addView(badge, badgeParams);
        }

        TextView count = text(item.countText, 15.0f, Color.rgb(104, 82, 226), true);
        count.setPadding(0, dp(4), 0, 0);
        caption.addView(count, new LinearLayout.LayoutParams(-1, -2));

        TextView date = text(item.cardDateText, 11.5f, Color.rgb(104, 113, 132), false);
        singleLine(date);
        date.setPadding(0, dp(2), 0, 0);
        caption.addView(date, new LinearLayout.LayoutParams(-1, -2));
        if (item.recentAddedCount > 0) {
            TextView added = text("이번에 +" + item.recentAddedCount + "장", 11.5f, Color.rgb(104, 82, 226), true);
            added.setPadding(0, dp(4), 0, 0);
            caption.addView(added, new LinearLayout.LayoutParams(-1, -2));
        }
        return card;
    }

    private void singleLine(TextView text) {
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value == null ? "" : value);
        text.setTextSize(sizeSp);
        text.setTextColor(color);
        text.setIncludeFontPadding(false);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return text;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(24));
        background.setStroke(dp(1), Color.rgb(231, 233, 239));
        return background;
    }

    private GradientDrawable newBadgeBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(245, 242, 255));
        background.setCornerRadius(dp(10));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

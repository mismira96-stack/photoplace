package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
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
            params.width = Math.min(windowWidthPx, dp(900));
            params.gravity = Gravity.CENTER_HORIZONTAL;
        }
        return params;
    }

    int availableGridWidth(int windowWidthPx) {
        int contentWidth = windowWidthPx >= dp(600) ? Math.min(windowWidthPx, dp(900)) : windowWidthPx;
        return Math.max(dp(280), contentWidth - dp(36));
    }

    View render(List<MemoryBrowserItem> items, int availableWidthPx) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        if (items == null || items.isEmpty()) {
            return grid;
        }

        int columns = availableWidthPx >= dp(720) ? 3 : 2;
        int gap = dp(8);
        int cardWidth = Math.max(dp(132), (availableWidthPx - (gap * (columns - 1))) / columns);
        int imageHeight = Math.max(dp(154), Math.min(dp(220), Math.round(cardWidth * 1.08f)));

        LinearLayout row = null;
        for (int index = 0; index < items.size(); index++) {
            if (index % columns == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            MemoryBrowserItem item = items.get(index);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, imageHeight);
            params.setMargins(index % columns == 0 ? 0 : gap, 0, 0, gap);
            row.addView(createCard(item, cardWidth, imageHeight), params);
        }
        return grid;
    }

    private View createCard(final MemoryBrowserItem item, int cardWidth, int imageHeight) {
        FrameLayout card = new FrameLayout(context);
        card.setClickable(true);
        card.setFocusable(true);
        card.setClipToOutline(true);
        card.setBackground(roundedBackground(Color.rgb(235, 237, 242), dp(8)));
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
        card.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if (thumbnailLoader != null) {
            thumbnailLoader.load(image, item.coverUri, Math.max(cardWidth, imageHeight));
        }

        LinearLayout caption = new LinearLayout(context);
        caption.setOrientation(LinearLayout.VERTICAL);
        caption.setPadding(dp(10), dp(8), dp(10), dp(9));
        caption.setBackground(roundedBackground(Color.argb(178, 18, 22, 29), dp(8)));
        FrameLayout.LayoutParams captionParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        card.addView(caption, captionParams);

        TextView title = text(item.title, 16.0f, Color.WHITE, true);
        title.setMaxLines(1);
        caption.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView count = text(item.countText, 14.0f, Color.WHITE, true);
        count.setPadding(0, dp(2), 0, 0);
        caption.addView(count, new LinearLayout.LayoutParams(-1, -2));

        TextView date = text(item.dateText, 11.5f, Color.rgb(225, 229, 238), false);
        date.setMaxLines(1);
        date.setPadding(0, dp(2), 0, 0);
        caption.addView(date, new LinearLayout.LayoutParams(-1, -2));
        return card;
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

    private GradientDrawable roundedBackground(int color, int radiusPx) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(radiusPx);
        return background;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/**
 * Renders Location Albums in a 2-column equal-height grid or a 3/4-column Samsung Gallery style
 * compact pinch view.
 */
final class LocationAlbumGridRenderer {

    interface Listener {
        void onAlbumClick(StoredAlbumSummary summary);
        void onAlbumLongClick(StoredAlbumSummary summary);
        void onAlbumMenuClick(StoredAlbumSummary summary);
    }

    interface ThumbnailLoader {
        void load(ImageView target, StoredAlbumSummary summary, int sizePx);
    }

    interface MemoProvider {
        String getMemo(StoredAlbumSummary summary);
    }

    private final Context context;
    private final Listener listener;
    private final ThumbnailLoader thumbnailLoader;
    private final MemoProvider memoProvider;

    LocationAlbumGridRenderer(
            Context context,
            Listener listener,
            ThumbnailLoader thumbnailLoader,
            MemoProvider memoProvider) {
        this.context = context;
        this.listener = listener;
        this.thumbnailLoader = thumbnailLoader;
        this.memoProvider = memoProvider;
    }

    int availableGridWidth(int contentWidth) {
        return Math.max(1, contentWidth - 2 * dp(MemoryListChrome.SIDE_DP));
    }

    View render(List<StoredAlbumSummary> items, int availableWidthPx, int columns) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        if (items == null || items.isEmpty()) {
            return grid;
        }

        boolean compact = columns >= 3;
        int gap = dp(columns == 2 ? 14 : (columns == 3 ? 4 : 3));
        int cell = (availableWidthPx - gap * (columns - 1)) / columns;
        int cardWidth = Math.max(1, cell);
        int cardHeight = compact ? Math.round(cardWidth * (columns == 3 ? 1.25f : 1.20f)) : -2;
        int coverRequestSize = Math.max(dp(150), (availableWidthPx - dp(9)) / 2);

        LinearLayout row = null;
        for (int index = 0; index < items.size(); index++) {
            if (index % columns == 0) {
                row = compact ? new LinearLayout(context) : new EqualHeightRow(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.bottomMargin = gap;
                grid.addView(row, rowParams);
            }
            StoredAlbumSummary item = items.get(index);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, compact ? cardHeight : -2);
            params.setMargins(index % columns == 0 ? 0 : gap, 0, 0, 0);
            View card = compact
                    ? createCompactCard(item, coverRequestSize, cardWidth, cardHeight, columns)
                    : createCard(item, coverRequestSize, dp(144), columns);
            row.addView(card, params);
        }
        return grid;
    }

    private View createCompactCard(final StoredAlbumSummary item, int coverRequestSize, int cardWidth, int cardHeight, int columns) {
        FrameLayout card = new FrameLayout(context);
        if (item.relativePath != null) {
            MemoryBrowserScrollController.markPlace(card, item.relativePath);
        }
        card.setClickable(true);
        card.setFocusable(true);
        card.setClipToOutline(true);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(238, 240, 245));
        shape.setCornerRadius(dp(14));
        card.setBackground(shape);

        card.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlbumClick(item);
            }
        });
        card.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onAlbumLongClick(item);
            }
            return true;
        });

        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if (thumbnailLoader != null) {
            thumbnailLoader.load(image, item, coverRequestSize);
        }

        GradientDrawable overlayGrad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, 0x33000000, 0xA6000000}
        );
        LinearLayout overlay = new LinearLayout(context);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        overlay.setBackground(overlayGrad);
        int padH = dp(columns == 3 ? 6 : 4);
        int padTop = dp(columns == 3 ? 24 : 18);
        int padBottom = dp(columns == 3 ? 8 : 6);
        overlay.setPadding(padH, padTop, padH, padBottom);

        TextView title = new TextView(context);
        title.setText(item.albumName == null ? "" : item.albumName);
        title.setTextSize(columns == 3 ? 12.5f : 11.0f);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setShadowLayer(dp(2), 0, dp(1), 0x99000000);
        overlay.addView(title, new LinearLayout.LayoutParams(-1, -2));

        if (columns == 3) {
            TextView countView = new TextView(context);
            countView.setText(item.itemCount + "장");
            countView.setTextSize(11.0f);
            countView.setTextColor(0xDDFFFFFF);
            countView.setGravity(Gravity.CENTER);
            countView.setSingleLine(true);
            countView.setShadowLayer(dp(2), 0, dp(1), 0x99000000);
            LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-1, -2);
            countLp.topMargin = dp(1);
            overlay.addView(countView, countLp);
            card.setContentDescription(item.albumName + ", " + item.itemCount + "장");
        } else {
            card.setContentDescription(item.albumName);
        }

        FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        card.addView(overlay, overlayLp);
        return card;
    }

    private View createCard(final StoredAlbumSummary item, int coverRequestSize, int imageHeight, int columns) {
        LinearLayout card = new LinearLayout(context);
        if (item.relativePath != null) {
            MemoryBrowserScrollController.markPlace(card, item.relativePath);
        }
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackground(cardBackground());
        card.setClipToOutline(true);

        card.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlbumClick(item);
            }
        });
        card.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onAlbumLongClick(item);
            }
            return true;
        });

        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image, new LinearLayout.LayoutParams(-1, imageHeight));
        if (thumbnailLoader != null) {
            thumbnailLoader.load(image, item, coverRequestSize);
        }

        LinearLayout caption = new LinearLayout(context);
        caption.setOrientation(LinearLayout.VERTICAL);
        caption.setPadding(dp(10), dp(5), dp(8), dp(6));
        card.addView(caption, new LinearLayout.LayoutParams(-1, -2, 1.0f));

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        caption.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        TextView title = text(item.albumName, 15.0f, -15656921, true);
        singleLine(title);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView count = text("사진 " + item.itemCount + "장", 12.0f, Color.rgb(104, 82, 226), false);
        count.setPadding(0, dp(2), 0, 0);
        caption.addView(count, new LinearLayout.LayoutParams(-1, -2));

        String dateText = formatMonthRange(item);
        TextView date = text(dateText, 11.5f, Color.rgb(104, 113, 132), false);
        singleLine(date);
        date.setPadding(0, dp(1), 0, 0);
        caption.addView(date, new LinearLayout.LayoutParams(-1, -2));

        return card;
    }

    static String formatMonthRange(StoredAlbumSummary summary) {
        if (summary == null) return "날짜 정보 없음";
        String start = monthFromDate(summary.startDate);
        String end = monthFromDate(summary.endDate);
        if (start == null && end == null) {
            return "날짜 정보 없음";
        }
        return start == null ? end : (end == null || start.equals(end)) ? start : start + "~" + end;
    }

    private static String monthFromDate(String str) {
        if (str == null || str.length() < 7) {
            return null;
        }
        try {
            return str.substring(0, 4) + "." + Integer.parseInt(str.substring(5, 7));
        } catch (Exception e) {
            return null;
        }
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
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), Color.rgb(231, 233, 239));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

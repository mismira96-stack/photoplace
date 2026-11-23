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
import java.util.function.Function;

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

    int availableGridWidth(int contentWidth) {
        return Math.max(1, contentWidth - 2 * dp(MemoryListChrome.SIDE_DP));
    }

    View render(List<MemoryBrowserItem> items, int availableWidthPx, int columns) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        if (items == null || items.isEmpty()) {
            return grid;
        }

        boolean compact = columns >= 3;
        int gap = dp(columns == 2 ? 14 : (columns == 3 ? 8 : 4));
        int cell = (availableWidthPx - gap * (columns - 1)) / columns;
        int cardWidth = Math.max(1, cell);
        int cardHeight = compact ? Math.round(cardWidth * (columns == 3 ? 1.25f : 1.20f)) : -2;
        int coverRequestSize = Math.max(dp(150), (availableWidthPx - gap) / 2);
        Function<String, List<PersonalPlace>> cardShortcuts = compact ? null
                : PersonalPlaceDetailPresenter.loadCardShortcuts(context);

        LinearLayout row = null;
        for (int index = 0; index < items.size(); index++) {
            if (index % columns == 0) {
                row = compact ? new LinearLayout(context) : new EqualHeightRow(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.bottomMargin = gap;
                grid.addView(row, rowParams);
            }
            MemoryBrowserItem item = items.get(index);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, compact ? cardHeight : -2);
            params.setMargins(index % columns == 0 ? 0 : gap, 0, 0, 0);
            View card = compact
                    ? createCompactCard(item, coverRequestSize, cardWidth, cardHeight, columns)
                    : createCard(item, coverRequestSize, dp(150), cardShortcuts.apply(item.memoryKey));
            row.addView(card, params);
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
            TextView count = text("검색 결과 " + items.size() + "개", 14.0f, Color.rgb(104, 82, 226), false);
            count.setPadding(dp(2), 0, 0, dp(10));
            container.addView(count, new LinearLayout.LayoutParams(-1, -2));
        }
        container.addView(render(items, availableWidthPx, columns), new LinearLayout.LayoutParams(-1, -2));
        return container;
    }

    private View createCompactCard(final MemoryBrowserItem item, int coverRequestSize, int cardWidth, int cardHeight, int columns) {
        FrameLayout card = new FrameLayout(context);
        MemoryBrowserScrollController.markPlace(card, item.memoryKey);
        card.setClickable(true);
        card.setFocusable(true);
        card.setClipToOutline(true);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(238, 240, 245));
        shape.setCornerRadius(dp(14));
        card.setBackground(shape);

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
            thumbnailLoader.load(image, item.coverUri, coverRequestSize);
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
        title.setText(item.title);
        title.setTextSize(columns == 3 ? 12.5f : 11.0f);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setShadowLayer(dp(2), 0, dp(1), 0x99000000);
        overlay.addView(title, new LinearLayout.LayoutParams(-1, -2));

        MemoryCardCountPresentation count = MemoryCardCountPresentation.from(item.countText);
        if (columns == 3) {
            String number = !count.photos.isEmpty() ? count.photos : (!count.videos.isEmpty() ? count.videos : "");
            if (!number.isEmpty()) {
                TextView countView = new TextView(context);
                countView.setText(number);
                countView.setTextSize(11.0f);
                countView.setTextColor(0xDDFFFFFF);
                countView.setGravity(Gravity.CENTER);
                countView.setSingleLine(true);
                countView.setShadowLayer(dp(2), 0, dp(1), 0x99000000);
                LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-1, -2);
                countLp.topMargin = dp(1);
                overlay.addView(countView, countLp);
            }
            card.setContentDescription(count.description.isEmpty() ? item.title : item.title + ", " + count.description);
        } else {
            card.setContentDescription(item.title);
        }

        FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        card.addView(overlay, overlayLp);
        return card;
    }

    private View createCard(final MemoryBrowserItem item, int coverRequestSize, int imageHeight,
                            List<PersonalPlace> cardPlaces) {
        LinearLayout card = new LinearLayout(context);
        MemoryBrowserScrollController.markPlace(card, item.memoryKey);
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
            thumbnailLoader.load(image, item.coverUri, coverRequestSize);
        }

        LinearLayout caption = new LinearLayout(context);
        caption.setOrientation(LinearLayout.VERTICAL);
        caption.setPadding(dp(10), dp(7), dp(8), dp(8));
        card.addView(caption, new LinearLayout.LayoutParams(-1, -2, 1.0f));

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setMinimumHeight(dp(22));
        caption.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        TextView title = text(item.title, 15.0f, Color.rgb(31, 35, 48), true);
        singleLine(title);
        titleRow.addView(title, new LinearLayout.LayoutParams(-2, -2));

        if (item.recentAddedCount > 0) {
            TextView badge = text("NEW", 10.0f, Color.rgb(104, 82, 226), false);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(6), dp(2), dp(6), dp(2));
            badge.setBackground(newBadgeBackground());
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
            badgeParams.setMargins(dp(4), 0, 0, 0);
            titleRow.addView(badge, badgeParams);
        }

        PersonalPlaceDetailPresenter.bindCardShortcuts(context, titleRow, item, cardPlaces, listener);

        TextView count = MemoryCardCountRenderer.render(context, item.countText);
        caption.addView(count, new LinearLayout.LayoutParams(-1, -2));

        TextView date = text(item.cardDateText, 11.5f, Color.rgb(104, 113, 132), false);
        singleLine(date);
        date.setPadding(0, dp(1), 0, 0);
        caption.addView(date, new LinearLayout.LayoutParams(-1, -2));
        if (item.recentAddedCount > 0) {
            TextView added = text("이번에 +" + item.recentAddedCount + "장", 11.5f, Color.rgb(104, 82, 226), false);
            singleLine(added);
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

package com.example.gallerysorter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Map;

/** Renders the collection cards shown above the discovery browser. */
final class MemoryCollectionCardRenderer {
    interface Listener {
        void onCollectionSelected(String collectionId);
    }

    private final MainActivity activity;
    private final Listener listener;

    MemoryCollectionCardRenderer(MainActivity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void renderPrepared(LinearLayout root, List<MemoryCollection> collections,
                        Map<String, MemoryCollectionCardSummary> summaries) {
        if (root == null) {
            return;
        }
        if (collections == null || collections.isEmpty()) {
            return;
        }
        for (final MemoryCollection collection : collections) {
            if (collection == null) {
                continue;
            }
            MemoryCollectionCardSummary summary = summaries.get(collection.collectionId);
            if (summary == null) continue;
            renderCard(root, collection.collectionId, collection.title, summary, "기억 모음",
                    () -> { if (listener != null) listener.onCollectionSelected(collection.collectionId); });
        }
    }

    void renderCountries(LinearLayout root, List<AutomaticCountryGroups.Group> groups,
                         java.util.function.Consumer<AutomaticCountryGroups.Group> opened) {
        for (AutomaticCountryGroups.Group group : groups)
            renderCard(root, group.key, group.title, group.summary, "국가 모음", () -> opened.accept(group));
    }

    private void renderCard(LinearLayout root, String key, String titleText, MemoryCollectionCardSummary summary,
                            String badgeText, Runnable opened) {
            LinearLayout card = new LinearLayout(activity);
            MemoryBrowserScrollController.markCollection(card, key);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
            activity.applyCardBackground(card);
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    opened.run();
                }
            });

            ImageView cover = new ImageView(activity);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable coverBg = new GradientDrawable();
            coverBg.setColor(Color.rgb(240, 242, 247));
            coverBg.setCornerRadius(activity.dp(14));
            cover.setBackground(coverBg);
            cover.setClipToOutline(true);
            cover.setContentDescription(titleText + " 대표 사진");
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(activity.dp(68), activity.dp(68));
            imageParams.setMargins(0, 0, activity.dp(12), 0);
            card.addView(cover, imageParams);
            if (!summary.coverUri.isEmpty()) {
                activity.loadMemoryBrowserThumbnailInto(cover, summary.coverUri, activity.dp(120));
            }

            LinearLayout labels = new LinearLayout(activity);
            labels.setOrientation(LinearLayout.VERTICAL);
            card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1.0f));

            LinearLayout metaRow = new LinearLayout(activity);
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setGravity(Gravity.CENTER_VERTICAL);
            labels.addView(metaRow, activity.matchWidth());

            TextView badge = new TextView(activity);
            badge.setText(badgeText);
            badge.setTextSize(10.5f);
            badge.setTextColor(Color.rgb(104, 82, 226));
            badge.setPadding(activity.dp(7), activity.dp(2), activity.dp(7), activity.dp(2));
            GradientDrawable badgeBackground = new GradientDrawable();
            badgeBackground.setColor(Color.rgb(245, 242, 255));
            badgeBackground.setCornerRadius(activity.dp(8));
            badge.setBackground(badgeBackground);
            metaRow.addView(badge, new LinearLayout.LayoutParams(-2, -2));

            if (!summary.dateRange.isEmpty()) {
                TextView date = new TextView(activity);
                date.setText(summary.dateRange);
                date.setTextSize(11.5f);
                date.setTextColor(Color.rgb(120, 129, 147));
                date.setPadding(activity.dp(8), 0, 0, 0);
                date.setSingleLine(true);
                date.setEllipsize(TextUtils.TruncateAt.END);
                metaRow.addView(date, new LinearLayout.LayoutParams(0, -2, 1.0f));
            }

            TextView title = new TextView(activity);
            title.setText(titleText);
            title.setTextSize(16.0f);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(Color.rgb(17, 24, 39));
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setPadding(0, activity.dp(3), 0, 0);
            labels.addView(title, activity.matchWidth());

            TextView places = new TextView(activity);
            places.setText(summary.placeNames.isEmpty()
                    ? "장소 정보를 불러올 수 없어요" : summary.placeNames);
            places.setTextSize(12.5f);
            places.setTextColor(Color.rgb(100, 111, 132));
            places.setSingleLine(true);
            places.setEllipsize(TextUtils.TruncateAt.END);
            places.setPadding(0, activity.dp(2), 0, 0);
            labels.addView(places, activity.matchWidth());

            String countText = summary.complete
                    ? summary.countText
                    : "불러오기 필요";
            TextView count = MemoryCardCountRenderer.render(activity, countText);
            labels.addView(count, activity.matchWidth());

            TextView arrow = new TextView(activity);
            arrow.setText("›");
            arrow.setTextSize(22.0f);
            arrow.setTypeface(Typeface.DEFAULT_BOLD);
            arrow.setTextColor(Color.rgb(148, 156, 172));
            arrow.setPadding(activity.dp(6), 0, 0, 0);
            card.addView(arrow, new LinearLayout.LayoutParams(-2, -2));
            root.addView(card, activity.matchWidthWithBottom(activity.dp(8)));
    }
}

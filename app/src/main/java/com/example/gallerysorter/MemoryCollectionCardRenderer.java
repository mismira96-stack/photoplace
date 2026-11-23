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

    void renderDiscoveryTopRow(LinearLayout root, MemoryBrowserEntry entry,
                               Runnable openCollections,
                               java.util.function.Consumer<AutomaticCountryGroups.Group> openSingleCountry,
                               Runnable openAllCountries) {
        if (root == null || entry == null) return;
        boolean hasCountries = entry.countries != null && entry.countries.groups != null && !entry.countries.groups.isEmpty();
        boolean hasCollections = entry.collections != null && !entry.collections.isEmpty();

        if (!hasCountries && !hasCollections) {
            return;
        }

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, activity.dp(10));
        row.setLayoutParams(rowParams);

        View collCard = null;
        if (hasCollections) {
            String collCoverUri = "";
            String collSubtitle = entry.collections.size() + "개 모음";
            MemoryCollection first = entry.collections.get(0);
            if (first != null && entry.collectionSummaries != null) {
                MemoryCollectionCardSummary s = entry.collectionSummaries.get(first.collectionId);
                if (s != null && s.coverUri != null) {
                    collCoverUri = s.coverUri;
                }
            }
            collCard = createTopTile(
                    "기억 모음",
                    collSubtitle,
                    collCoverUri,
                    "모음",
                    () -> {
                        if (openCollections != null) openCollections.run();
                    });
        }

        // 2. Overseas Countries tile
        View countryCard = null;
        if (hasCountries) {
            String countryCoverUri = "";
            String countrySubtitle = entry.countries.groups.size() + "개국 모아보기";
            if (entry.countries.groups.size() == 1) {
                AutomaticCountryGroups.Group g = entry.countries.groups.get(0);
                countrySubtitle = g.title + (g.summary != null && !g.summary.countText.isEmpty() ? " · " + g.summary.countText : "");
                if (g.summary != null && g.summary.coverUri != null) countryCoverUri = g.summary.coverUri;
            } else {
                AutomaticCountryGroups.Group g = entry.countries.groups.get(0);
                if (g.summary != null && g.summary.coverUri != null) countryCoverUri = g.summary.coverUri;
            }
            final boolean single = entry.countries.groups.size() == 1;
            countryCard = createTopTile(
                    "해외 기록",
                    countrySubtitle,
                    countryCoverUri,
                    "해외",
                    () -> {
                        if (single && openSingleCountry != null) {
                            openSingleCountry.accept(entry.countries.groups.get(0));
                        } else if (openAllCountries != null) {
                            openAllCountries.run();
                        }
                    });
        }

        if (countryCard != null) {
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            p1.setMargins(0, 0, activity.dp(4), 0);
            row.addView(collCard, p1);

            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            p2.setMargins(activity.dp(4), 0, 0, 0);
            row.addView(countryCard, p2);
        } else {
            row.addView(collCard, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        root.addView(row);
    }

    private View createTopTile(String titleText, String subtitleText, String coverUri,
                               String badgeText, Runnable opened) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(activity.dp(10), activity.dp(8), activity.dp(10), activity.dp(8));
        activity.applyCardBackground(card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> opened.run());

        ImageView cover = new ImageView(activity);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable coverBg = new GradientDrawable();
        coverBg.setColor(Color.rgb(240, 242, 247));
        coverBg.setCornerRadius(activity.dp(10));
        cover.setBackground(coverBg);
        cover.setClipToOutline(true);
        cover.setContentDescription(titleText);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(activity.dp(44), activity.dp(44));
        imgParams.setMargins(0, 0, activity.dp(8), 0);
        card.addView(cover, imgParams);
        if (coverUri != null && !coverUri.isEmpty()) {
            activity.loadMemoryBrowserThumbnailInto(cover, coverUri, activity.dp(88));
        }

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(activity);
        title.setText(titleText);
        title.setTextSize(13.5f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(26, 32, 44));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title, activity.matchWidth());

        TextView sub = new TextView(activity);
        sub.setText(subtitleText);
        sub.setTextSize(11.0f);
        sub.setTextColor(Color.rgb(104, 82, 226));
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        sub.setPadding(0, activity.dp(2), 0, 0);
        texts.addView(sub, activity.matchWidth());

        return card;
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

package com.example.gallerysorter;

import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Renders the collection cards shown above the discovery browser. */
final class MemoryCollectionCardRenderer {
    interface Listener {
        void onCollectionSelected(String collectionId);

        void onCreateCollectionRequested();
    }

    private final MainActivity activity;
    private final Listener listener;

    MemoryCollectionCardRenderer(MainActivity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void render(LinearLayout root, List<MemoryCollection> collections,
                List<MemoryRecord> records, Map<String, String> aliases) {
        if (root == null) {
            return;
        }
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.addView(activity.sectionTitle("내 기억 모음"),
                new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView createAction = activity.bodyText("＋ 모으기");
        createAction.setTextSize(13.0f);
        createAction.setTextColor(Color.rgb(104, 82, 190));
        createAction.setGravity(android.view.Gravity.CENTER_VERTICAL);
        createAction.setPadding(activity.dp(8), activity.dp(8), activity.dp(2), activity.dp(8));
        createAction.setContentDescription("기억 모으기");
        createAction.setClickable(true);
        createAction.setFocusable(true);
        createAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onCreateCollectionRequested();
                }
            }
        });
        header.addView(createAction, new LinearLayout.LayoutParams(-2, -2));
        root.addView(header, activity.matchWidthWithBottom(activity.dp(4)));
        if (collections == null || collections.isEmpty()) {
            return;
        }
        for (final MemoryCollection collection : collections) {
            if (collection == null) {
                continue;
            }
            Summary summary = summarize(collection, records, aliases);
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            card.setPadding(activity.dp(8), activity.dp(8), activity.dp(10), activity.dp(8));
            activity.applyCardBackground(card);
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onCollectionSelected(collection.collectionId);
                    }
                }
            });

            ImageView cover = new ImageView(activity);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cover.setBackground(activity.thumbnailPlaceholder());
            cover.setContentDescription(collection.title + " 대표 사진");
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(activity.dp(68), activity.dp(68));
            imageParams.setMargins(0, 0, activity.dp(10), 0);
            card.addView(cover, imageParams);
            if (!summary.coverUri.isEmpty()) {
                activity.loadMemoryBrowserThumbnailInto(cover, summary.coverUri, activity.dp(96));
            }

            LinearLayout labels = new LinearLayout(activity);
            labels.setOrientation(LinearLayout.VERTICAL);
            card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView badge = activity.bodyText("기억 모음");
            badge.setTextSize(10.0f);
            badge.setTextColor(Color.rgb(104, 82, 190));
            badge.setPadding(activity.dp(6), activity.dp(2), activity.dp(6), activity.dp(2));
            GradientDrawable badgeBackground = new GradientDrawable();
            badgeBackground.setColor(Color.rgb(245, 242, 255));
            badgeBackground.setCornerRadius(activity.dp(9));
            badge.setBackground(badgeBackground);
            labels.addView(badge, new LinearLayout.LayoutParams(-2, -2));
            TextView title = activity.bodyText(collection.title);
            title.setTextSize(16.0f);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setPadding(0, activity.dp(2), 0, 0);
            labels.addView(title, activity.matchWidth());

            TextView places = activity.bodyText(summary.placeNames.isEmpty()
                    ? "장소 정보를 불러올 수 없어요" : summary.placeNames);
            places.setSingleLine(true);
            places.setEllipsize(TextUtils.TruncateAt.END);
            places.setPadding(0, activity.dp(2), 0, 0);
            labels.addView(places, activity.matchWidth());

            TextView count = activity.bodyText(SingleAlbumCompletionResolver.formatMediaCount(
                    summary.photoCount, summary.videoCount)
                    + (summary.dateRange.isEmpty() ? "" : " · " + summary.dateRange));
            count.setTextSize(12.0f);
            count.setPadding(0, activity.dp(2), 0, 0);
            labels.addView(count, activity.matchWidth());
            TextView arrow = activity.bodyText("›");
            arrow.setTextSize(26.0f);
            card.addView(arrow, new LinearLayout.LayoutParams(-2, -2));
            root.addView(card, activity.matchWidthWithBottom(activity.dp(6)));
        }
    }

    Button createSelectionButton() {
        Button button = new Button(activity);
        button.setText("기억 모으기");
        button.setTextSize(14.0f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(Color.rgb(104, 82, 190));
        button.setAllCaps(false);
        button.setMinHeight(activity.dp(42));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(activity.dp(16));
        background.setStroke(activity.dp(1), Color.rgb(231, 233, 239));
        button.setBackground(background);
        return button;
    }

    private Summary summarize(MemoryCollection collection, List<MemoryRecord> records,
                              Map<String, String> aliases) {
        Set<String> memberIds = new HashSet<>();
        for (MemoryCollection.Member member : collection.members) {
            if (member != null) {
                memberIds.add(member.stableMemoryId);
            }
        }
        Set<String> names = new LinkedHashSet<>();
        Set<String> uris = new HashSet<>();
        String cover = "";
        int photos = 0;
        int videos = 0;
        long firstDate = Long.MAX_VALUE;
        long lastDate = 0L;
        if (records != null) {
            for (MemoryRecord record : records) {
                if (record == null || aliases == null
                        || !memberIds.contains(aliases.get(record.memoryKey))) {
                    continue;
                }
                MemoryBrowserItem item = MemoryBrowserItem.from(record);
                if (item != null) {
                    names.add(item.title);
                }
                firstDate = Math.min(firstDate,
                        record.startDateMillis > 0L ? record.startDateMillis : Long.MAX_VALUE);
                lastDate = Math.max(lastDate, record.endDateMillis);
                if (record.discoveryGroup == null) {
                    continue;
                }
                for (DiscoveryPhotoRef ref : record.discoveryGroup.photoRefs) {
                    if (ref == null || ref.stale || ref.sourceUri == null || !uris.add(ref.sourceUri)) {
                        continue;
                    }
                    if (ref.mediaKind == MediaKind.VIDEO) {
                        videos++;
                    } else {
                        photos++;
                    }
                    if (cover.isEmpty()) {
                        cover = ref.sourceUri;
                    }
                }
            }
        }
        String range = "";
        if (firstDate != Long.MAX_VALUE && lastDate > 0L) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
            String startText = formatter.format(new Date(firstDate));
            String endText = formatter.format(new Date(lastDate));
            range = startText.equals(endText) ? startText : startText + " ~ " + endText;
        }
        StringBuilder placeNames = new StringBuilder();
        int shownNameCount = 0;
        for (String name : names) {
            if (shownNameCount >= 3) {
                break;
            }
            if (placeNames.length() > 0) {
                placeNames.append(" · ");
            }
            placeNames.append(name);
            shownNameCount++;
        }
        int remainingNames = Math.max(0, collection.members.size() - shownNameCount);
        if (remainingNames > 0) {
            placeNames.append(" 외 ").append(remainingNames).append("곳");
        }
        return new Summary(photos, videos, cover, placeNames.toString(), range);
    }

    private static final class Summary {
        final int photoCount;
        final int videoCount;
        final String coverUri;
        final String placeNames;
        final String dateRange;

        Summary(int photoCount, int videoCount, String coverUri, String placeNames, String dateRange) {
            this.photoCount = Math.max(0, photoCount);
            this.videoCount = Math.max(0, videoCount);
            this.coverUri = coverUri == null ? "" : coverUri;
            this.placeNames = placeNames == null ? "" : placeNames;
            this.dateRange = dateRange == null ? "" : dateRange;
        }
    }
}

package com.example.gallerysorter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Renders the date -> place -> note -> photos collection detail hierarchy. */
final class MemoryCollectionDetailRenderer {
    interface Listener {
        void onBack();

        void onRenameRequested();

        void onDissolveConfirmed();
    }

    private final MainActivity activity;
    private final Listener listener;

    MemoryCollectionDetailRenderer(MainActivity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void render(final MemoryCollection collection, GroupMemoryDetail detail) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(-197377);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(activity.dp(18), activity.dp(56), activity.dp(18), activity.dp(24));
        scroll.addView(root, activity.scrollContentLayoutParams());
        activity.addMemoryHeader(root, collection.title, new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onBack();
                }
            }
        });
        addSummaryAndActionsCard(root, collection, detail);

        if (detail == null || detail.dates.isEmpty()) {
            LinearLayout emptyCard = new LinearLayout(activity);
            emptyCard.setOrientation(LinearLayout.VERTICAL);
            emptyCard.setPadding(activity.dp(16), activity.dp(20), activity.dp(16), activity.dp(20));
            activity.applyCardBackground(emptyCard);
            TextView empty = activity.bodyText("현재 볼 수 있는 사진이 없어요. 기억 모음과 장소별 메모는 보존되어 있어요.");
            emptyCard.addView(empty, activity.matchWidth());
            root.addView(emptyCard, activity.matchWidth());
        } else {
            for (GroupMemoryDateSection date : detail.dates) {
                root.addView(activity.sectionTitle(date.dateText),
                        activity.matchWidthWithBottom(activity.dp(8)));
                for (GroupMemoryPlaceSection place : date.places) {
                    addPlace(root, date, place);
                }
            }
        }
        activity.setContentViewWithBottomTabs(scroll, 1);
    }

    private void addSummaryAndActionsCard(LinearLayout root, MemoryCollection collection, GroupMemoryDetail detail) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(activity.dp(16), activity.dp(14), activity.dp(16), activity.dp(14));
        activity.applyCardBackground(card);

        int totalPhotos = 0;
        int totalVideos = 0;
        if (detail != null && detail.dates != null) {
            for (GroupMemoryDateSection date : detail.dates) {
                if (date == null || date.places == null) continue;
                for (GroupMemoryPlaceSection place : date.places) {
                    if (place == null || place.photos == null) continue;
                    for (MemoryPhotoItem item : place.photos) {
                        if (item == null) continue;
                        if (item.mediaKind == MediaKind.VIDEO) totalVideos++;
                        else totalPhotos++;
                    }
                }
            }
        }
        int memberCount = collection.members == null ? 0 : collection.members.size();
        LinearLayout metaRow = new LinearLayout(activity);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = new TextView(activity);
        badge.setText("기억 모음 · " + memberCount + "곳");
        badge.setTextSize(11.5f);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.rgb(104, 82, 226));
        badge.setPadding(activity.dp(8), activity.dp(3), activity.dp(8), activity.dp(3));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.rgb(245, 242, 255));
        badgeBg.setCornerRadius(activity.dp(9));
        badge.setBackground(badgeBg);
        metaRow.addView(badge, new LinearLayout.LayoutParams(-2, -2));

        TextView totalCount = MemoryCardCountRenderer.render(activity,
                SingleAlbumCompletionResolver.formatMediaCount(totalPhotos, totalVideos));
        totalCount.setPadding(activity.dp(10), 0, 0, 0);
        metaRow.addView(totalCount, new LinearLayout.LayoutParams(0, -2, 1.0f));

        if (totalPhotos + totalVideos > 0) {
            TextView storyShareBtn = new TextView(activity);
            storyShareBtn.setText("스토리 사진띠");
            storyShareBtn.setTextSize(12.0f);
            storyShareBtn.setTypeface(Typeface.DEFAULT_BOLD);
            storyShareBtn.setTextColor(Color.rgb(104, 82, 226));
            storyShareBtn.setGravity(Gravity.CENTER_VERTICAL);
            storyShareBtn.setPadding(activity.dp(10), activity.dp(6), activity.dp(10), activity.dp(6));
            int iconPx = activity.dp(14);
            MediaVectorIconDrawable shareIcon = MediaVectorIconDrawable.share(Color.rgb(104, 82, 226), iconPx);
            shareIcon.setBounds(0, 0, iconPx, iconPx);
            storyShareBtn.setCompoundDrawablePadding(activity.dp(5));
            storyShareBtn.setCompoundDrawables(shareIcon, null, null, null);
            GradientDrawable shareBg = new GradientDrawable();
            shareBg.setColor(Color.rgb(245, 242, 255));
            shareBg.setCornerRadius(activity.dp(12));
            shareBg.setStroke(activity.dp(1), Color.rgb(220, 212, 252));
            storyShareBtn.setBackground(shareBg);
            storyShareBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MemoryStoryShareSheet.showForCollection(activity, collection, detail);
                }
            });
            metaRow.addView(storyShareBtn, new LinearLayout.LayoutParams(-2, -2));
        }
        card.addView(metaRow, activity.matchWidthWithBottom(activity.dp(12)));

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button rename = new Button(activity);
        activity.styleSubtleActionButton(rename, "이름 변경");
        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onRenameRequested();
                }
            }
        });
        LinearLayout.LayoutParams renameParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        renameParams.rightMargin = activity.dp(8);
        actions.addView(rename, renameParams);

        Button dissolve = new Button(activity);
        activity.styleSubtleActionButton(dissolve, "기억 모음 해제");
        dissolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MemoryActionDialog.show(
                        activity,
                        "기억 모음을 해제할까요?",
                        "모음만 해제되며 원래 장소 기록과 사진, 날짜 메모는 삭제되지 않아요.",
                        "해제",
                        new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.onDissolveConfirmed();
                                }
                            }
                        },
                        "취소",
                        new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
            }
        });
        actions.addView(dissolve, new LinearLayout.LayoutParams(0, -2, 1.0f));
        card.addView(actions, activity.matchWidth());
        root.addView(card, activity.matchWidthWithBottom(activity.dp(14)));
    }

    private void addPlace(LinearLayout root, GroupMemoryDateSection date, GroupMemoryPlaceSection place) {
        LinearLayout placeBlock = new LinearLayout(activity);
        placeBlock.setOrientation(LinearLayout.VERTICAL);
        placeBlock.setPadding(activity.dp(14), activity.dp(12), activity.dp(14), activity.dp(12));
        activity.applyCardBackground(placeBlock);

        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView placeTitle = new TextView(activity);
        placeTitle.setText(place.placeTitle);
        placeTitle.setTypeface(Typeface.DEFAULT_BOLD);
        placeTitle.setTextSize(16.0f);
        placeTitle.setTextColor(Color.rgb(17, 24, 39));
        headerRow.addView(placeTitle, new LinearLayout.LayoutParams(0, -2, 1.0f));

        int photos = 0;
        int videos = 0;
        for (MemoryPhotoItem photo : place.photos) {
            if (photo.mediaKind == MediaKind.VIDEO) {
                videos++;
            } else {
                photos++;
            }
        }
        TextView photoCount = MemoryCardCountRenderer.render(activity,
                SingleAlbumCompletionResolver.formatMediaCount(photos, videos));
        headerRow.addView(photoCount, new LinearLayout.LayoutParams(-2, -2));
        placeBlock.addView(headerRow, activity.matchWidthWithBottom(activity.dp(8)));

        if (!place.noteText.isEmpty()) {
            TextView note = new TextView(activity);
            note.setText(place.noteText);
            note.setTextSize(13.5f);
            note.setTextColor(Color.rgb(64, 53, 106));
            note.setPadding(activity.dp(12), activity.dp(8), activity.dp(12), activity.dp(8));
            GradientDrawable noteBg = new GradientDrawable();
            noteBg.setColor(Color.rgb(246, 244, 255));
            noteBg.setCornerRadius(activity.dp(12));
            note.setBackground(noteBg);
            placeBlock.addView(note, activity.matchWidthWithBottom(activity.dp(10)));
        }

        addPhotos(placeBlock, date, place);
        root.addView(placeBlock, activity.matchWidthWithBottom(activity.dp(10)));
    }

    private void addPhotos(final LinearLayout parent,
                           GroupMemoryDateSection date,
                           final GroupMemoryPlaceSection place) {
        final List<MemoryPhotoItem> photos = place == null ? null : place.photos;
        final int total = photos == null ? 0 : photos.size();
        final int[] shown = {Math.min(total, MainActivity.MAX_RESULT_DETAIL_THUMBNAILS)};
        final MemoryPhotoSection viewerSection = MemoryPhotoSection.fromCollectionPlace(
                date == null ? "" : date.dateKey,
                date == null ? "" : date.dateText,
                place == null ? "" : place.placeTitle,
                photos);
        activity.addMemoryPhotoGrid(parent, viewerSection, 0, shown[0]);
        if (shown[0] >= total) {
            return;
        }
        final Button more = new Button(activity);
        activity.updateMemoryPhotoMoreButton(more, total - shown[0]);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int added = Math.min(MainActivity.MAX_RESULT_DETAIL_THUMBNAILS, total - shown[0]);
                activity.addMemoryPhotoGrid(parent, viewerSection, shown[0], added);
                shown[0] += added;
                if (shown[0] >= total) {
                    ((android.view.ViewGroup) more.getParent()).removeView(more);
                } else {
                    activity.updateMemoryPhotoMoreButton(more, total - shown[0]);
                }
            }
        });
        parent.addView(more, activity.matchWidthWithBottom(activity.dp(8)));
    }
}

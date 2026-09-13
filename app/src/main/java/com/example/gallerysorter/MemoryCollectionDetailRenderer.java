package com.example.gallerysorter;

import android.content.DialogInterface;
import android.app.AlertDialog;
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
        addActions(root);

        if (detail == null || detail.dates.isEmpty()) {
            TextView empty = activity.bodyText("현재 볼 수 있는 사진이 없어요. 기억 모음과 장소별 메모는 보존되어 있어요.");
            empty.setPadding(activity.dp(4), activity.dp(16), activity.dp(4), activity.dp(16));
            root.addView(empty, activity.matchWidth());
        } else {
            for (GroupMemoryDateSection date : detail.dates) {
                root.addView(activity.sectionTitle(date.dateText),
                        activity.matchWidthWithBottom(activity.dp(8)));
                for (GroupMemoryPlaceSection place : date.places) {
                    addPlace(root, place);
                }
            }
        }
        activity.setContentViewWithBottomTabs(scroll, 1);
    }

    private void addActions(LinearLayout root) {
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
        actions.addView(rename, new LinearLayout.LayoutParams(0, -2, 1.0f));

        Button dissolve = new Button(activity);
        activity.styleSubtleActionButton(dissolve, "기억 모음 해제");
        dissolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(activity)
                        .setTitle("기억 모음을 해제할까요?")
                        .setMessage("원래 장소와 사진, 날짜 메모는 삭제되지 않아요.")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("해제", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (listener != null) {
                                    listener.onDissolveConfirmed();
                                }
                            }
                        }).show();
            }
        });
        actions.addView(dissolve, new LinearLayout.LayoutParams(0, -2, 1.0f));
        root.addView(actions, activity.matchWidthWithBottom(activity.dp(14)));
    }

    private void addPlace(LinearLayout root, GroupMemoryPlaceSection place) {
        LinearLayout placeBlock = new LinearLayout(activity);
        placeBlock.setOrientation(LinearLayout.VERTICAL);
        placeBlock.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(12));
        activity.applyCardBackground(placeBlock);
        TextView placeTitle = activity.bodyText(place.placeTitle);
        placeTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        placeTitle.setTextSize(15.0f);
        placeBlock.addView(placeTitle, activity.matchWidth());
        if (!place.noteText.isEmpty()) {
            TextView note = activity.bodyText(place.noteText);
            note.setPadding(0, activity.dp(4), 0, activity.dp(8));
            placeBlock.addView(note, activity.matchWidth());
        }

        int photos = 0;
        int videos = 0;
        for (MemoryPhotoItem photo : place.photos) {
            if (photo.mediaKind == MediaKind.VIDEO) {
                videos++;
            } else {
                photos++;
            }
        }
        TextView photoCount = activity.bodyText(
                SingleAlbumCompletionResolver.formatMediaCount(photos, videos));
        photoCount.setTextSize(12.0f);
        photoCount.setPadding(0, activity.dp(4), 0, activity.dp(8));
        placeBlock.addView(photoCount, activity.matchWidth());
        addPhotos(placeBlock, place.photos);
        root.addView(placeBlock, activity.matchWidthWithBottom(activity.dp(10)));
    }

    private void addPhotos(final LinearLayout parent, final List<MemoryPhotoItem> photos) {
        final int total = photos == null ? 0 : photos.size();
        final int[] shown = {Math.min(total, MainActivity.MAX_RESULT_DETAIL_THUMBNAILS)};
        activity.addMemoryPhotoGrid(parent, photos, 0, shown[0]);
        if (shown[0] >= total) {
            return;
        }
        final Button more = new Button(activity);
        activity.updateMemoryPhotoMoreButton(more, total - shown[0]);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int added = Math.min(MainActivity.MAX_RESULT_DETAIL_THUMBNAILS, total - shown[0]);
                activity.addMemoryPhotoGrid(parent, photos, shown[0], added);
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

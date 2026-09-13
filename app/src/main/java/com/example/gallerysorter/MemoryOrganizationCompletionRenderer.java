package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Compact completion content for organizing one Memory into a Gallery album. */
final class MemoryOrganizationCompletionRenderer {
    interface Listener {
        void onReturnToMemory();

        void onOpenAlbums();
    }

    interface ButtonStyler {
        void stylePrimary(Button button, String label);

        void styleSecondary(Button button, String label);
    }

    interface ThumbnailLoader {
        void loadInto(ImageView imageView, String uri);
    }

    private final Context context;
    private final int densityDpi;
    private final ButtonStyler buttonStyler;
    private final ThumbnailLoader thumbnailLoader;

    MemoryOrganizationCompletionRenderer(Context context, ButtonStyler buttonStyler,
                                         ThumbnailLoader thumbnailLoader) {
        this.context = context;
        this.densityDpi = context.getResources().getDisplayMetrics().densityDpi;
        this.buttonStyler = buttonStyler;
        this.thumbnailLoader = thumbnailLoader;
    }

    LinearLayout render(String albumName, int copiedCount, int failedCount,
                        boolean canceled, boolean linkSaved, String coverUri,
                        String dateRange, int noLocationCount, String returnLabel,
                        Listener listener) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(18), dp(20), dp(18), dp(16));
        hero.setBackground(tintedCardBackground(0xFFF9F9FC, 0xFFE7E9EF));
        content.addView(hero, matchWidth());

        boolean complete = copiedCount > 0 && failedCount == 0 && !canceled && linkSaved;
        TextView mark = new TextView(context);
        mark.setText(complete ? "✓" : "!");
        mark.setTextSize(28);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setTextColor(complete ? 0xFF159653 : 0xFFB06A00);
        GradientDrawable markBackground = new GradientDrawable();
        markBackground.setShape(GradientDrawable.OVAL);
        markBackground.setColor(complete ? 0xFFE7F6EE : 0xFFFFF2D8);
        mark.setBackground(markBackground);
        hero.addView(mark, new LinearLayout.LayoutParams(dp(58), dp(58)));

        TextView title = new TextView(context);
        title.setText(titleText(albumName, copiedCount, failedCount, canceled, linkSaved));
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF1B2438);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(14), 0, 0);
        hero.addView(title, matchWidth());

        TextView count = new TextView(context);
        String summary = "사진·동영상 " + Math.max(0, copiedCount) + "개";
        if (dateRange != null && !dateRange.trim().isEmpty()) {
            summary += " · " + dateRange;
        }
        count.setText(summary);
        count.setTextSize(15);
        count.setTextColor(0xFF67738B);
        count.setGravity(Gravity.CENTER);
        count.setPadding(0, dp(5), 0, dp(12));
        hero.addView(count, matchWidth());

        Button albums = new Button(context);
        buttonStyler.stylePrimary(albums, "위치 앨범 보기  ›");
        albums.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onOpenAlbums();
            }
        });
        hero.addView(albums, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout stats = new LinearLayout(context);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        stats.setPadding(dp(12), dp(14), dp(12), dp(14));
        stats.setBackground(tintedCardBackground(0xFFFFFFFF, 0xFFE7E9EF));
        LinearLayout.LayoutParams statsParams = matchWidth();
        statsParams.topMargin = dp(12);
        content.addView(stats, statsParams);
        addStat(stats, "정리된 사진·동영상", Math.max(0, copiedCount) + "개", 0xFF159653);
        View divider = new View(context);
        divider.setBackgroundColor(0xFFE5E8EE);
        stats.addView(divider, new LinearLayout.LayoutParams(dp(1), dp(52)));
        addStat(stats, "위치 정보 없음", Math.max(0, noLocationCount) + "개", 0xFFE18A00);

        TextView keepOriginals = new TextView(context);
        keepOriginals.setText(statusText(copiedCount, failedCount, canceled, linkSaved));
        keepOriginals.setTextSize(13);
        keepOriginals.setTextColor(0xFF737F95);
        keepOriginals.setPadding(dp(4), dp(10), dp(4), 0);
        content.addView(keepOriginals, matchWidth());

        if (copiedCount > 0) {
            TextView albumLabel = new TextView(context);
            albumLabel.setText("이번에 만든 위치 앨범");
            albumLabel.setTextSize(15);
            albumLabel.setTypeface(Typeface.DEFAULT_BOLD);
            albumLabel.setTextColor(0xFF1B2438);
            albumLabel.setPadding(dp(4), dp(18), 0, dp(8));
            content.addView(albumLabel, matchWidth());

            LinearLayout albumRow = new LinearLayout(context);
            albumRow.setOrientation(LinearLayout.HORIZONTAL);
            albumRow.setGravity(Gravity.CENTER_VERTICAL);
            albumRow.setPadding(dp(12), dp(12), dp(12), dp(12));
            albumRow.setBackground(tintedCardBackground(0xFFFCFCFE, 0xFFE3E6EE));
            albumRow.setClickable(true);
            albumRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onOpenAlbums();
                }
            });
            LinearLayout.LayoutParams albumRowParams = matchWidth();
            content.addView(albumRow, albumRowParams);

            ImageView cover = new ImageView(context);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cover.setContentDescription(displayAlbumName(albumName));
            thumbnailLoader.loadInto(cover, coverUri);
            albumRow.addView(cover, new LinearLayout.LayoutParams(dp(76), dp(76)));

            LinearLayout albumText = new LinearLayout(context);
            albumText.setOrientation(LinearLayout.VERTICAL);
            albumText.setPadding(dp(12), 0, 0, 0);
            albumRow.addView(albumText, new LinearLayout.LayoutParams(0, -2, 1));

            TextView albumNameView = new TextView(context);
            albumNameView.setText(displayAlbumName(albumName));
            albumNameView.setTextSize(16);
            albumNameView.setTypeface(Typeface.DEFAULT_BOLD);
            albumNameView.setTextColor(0xFF1B2438);
            albumText.addView(albumNameView, matchWidth());

            TextView albumCount = new TextView(context);
            albumCount.setText("사진·동영상 " + Math.max(0, copiedCount) + "개 · 방금 생성됨");
            albumCount.setTextSize(13);
            albumCount.setTextColor(0xFF737F95);
            albumCount.setPadding(0, dp(4), 0, 0);
            albumText.addView(albumCount, matchWidth());

            TextView arrow = new TextView(context);
            arrow.setText("›");
            arrow.setTextSize(27);
            arrow.setTextColor(0xFF8A94A8);
            albumRow.addView(arrow, new LinearLayout.LayoutParams(dp(28), -2));
        }

        Button back = new Button(context);
        buttonStyler.styleSecondary(back, returnLabel);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onReturnToMemory();
            }
        });
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(46));
        backParams.topMargin = dp(12);
        content.addView(back, backParams);
        return content;
    }

    private void addStat(LinearLayout parent, String label, String value, int valueColor) {
        LinearLayout stat = new LinearLayout(context);
        stat.setOrientation(LinearLayout.VERTICAL);
        stat.setGravity(Gravity.CENTER);
        parent.addView(stat, new LinearLayout.LayoutParams(0, -2, 1));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(13);
        labelView.setTextColor(0xFF737F95);
        labelView.setGravity(Gravity.CENTER);
        stat.addView(labelView, matchWidth());

        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextSize(23);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setTextColor(valueColor);
        valueView.setGravity(Gravity.CENTER);
        valueView.setPadding(0, dp(3), 0, 0);
        stat.addView(valueView, matchWidth());
    }

    private String titleText(String albumName, int copiedCount, int failedCount,
                              boolean canceled, boolean linkSaved) {
        String name = displayAlbumName(albumName);
        if (copiedCount <= 0) {
            return "새로 정리된 항목이 없어요";
        }
        if (canceled) {
            return name + " 앨범 정리가 중단됐어요";
        }
        if (failedCount > 0) {
            return name + " 앨범 일부를 정리했어요";
        }
        if (!linkSaved) {
            return name + " 앨범은 만들었지만 기억 연결이 저장되지 않았어요";
        }
        String placeName = name.endsWith("에서")
                ? name.substring(0, name.length() - 2) : name;
        return placeName + " 위치 앨범을 만들었어요";
    }

    private String statusText(int copiedCount, int failedCount, boolean canceled, boolean linkSaved) {
        if (canceled) {
            return linkSaved
                    ? "정리된 항목은 유지됩니다. 다시 실행하면 남은 항목을 정리할 수 있어요."
                    : "일부 앨범은 만들어졌지만 Memory 연결을 저장하지 못했어요. 원본 사진은 그대로 보관돼요.";
        }
        if (failedCount > 0) {
            return linkSaved
                    ? "일부 항목을 정리하지 못했어요. 원본 사진은 그대로 보관돼요."
                    : "일부 항목을 정리하지 못했고 Memory 연결도 저장되지 않았어요. 원본 사진은 그대로 보관돼요.";
        }
        if (!linkSaved && copiedCount > 0) {
            return "앨범은 만들어졌지만 Memory 연결을 저장하지 못했어요. 원본 사진은 그대로 보관돼요.";
        }
        return copiedCount > 0
                ? "원본은 갤러리에 그대로 남아 있어요."
                : "원본은 갤러리에 그대로 남아 있어요.";
    }

    private GradientDrawable tintedCardBackground(int fillColor, int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), strokeColor);
        return background;
    }

    private String displayAlbumName(String albumName) {
        return albumName == null || albumName.trim().isEmpty() ? "위치 앨범" : albumName.trim();
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(int value) {
        return (int) (value * (densityDpi / 160.0f) + 0.5f);
    }
}

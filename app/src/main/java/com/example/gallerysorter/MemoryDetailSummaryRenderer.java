package com.example.gallerysorter;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Locale;

/** Compact record header & metadata: 1-row title & actions, subtle counts & date subline. */
final class MemoryDetailSummaryRenderer {
    static void bind(Activity activity, LinearLayout root, MemoryBrowserDetail detail,
                     MemoryDetailSelection selection, Runnable onBack, Runnable onAddAlbum) {
        // Row 1: Back + Title + SelectionCount + Action Buttons
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.bottomMargin = CleanupUiStyle.dp(activity, 2);
        root.addView(headerRow, headerParams);

        if (onBack != null) {
            TextView back = new TextView(activity);
            back.setText("‹");
            back.setTextSize(34.0f);
            back.setGravity(Gravity.CENTER);
            back.setTextColor(CleanupUiStyle.INK);
            back.setOnClickListener(v -> onBack.run());
            headerRow.addView(back, new LinearLayout.LayoutParams(
                    CleanupUiStyle.dp(activity, 38), CleanupUiStyle.dp(activity, 38)));
        }

        TextView title = new TextView(activity);
        title.setText(detail.item.title);
        title.setTextSize(20.0f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(CleanupUiStyle.INK);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        titleParams.leftMargin = CleanupUiStyle.dp(activity, 2);
        headerRow.addView(title, titleParams);

        if (selection != null) {
            headerRow.addView(selection.countView());
        }

        // Action buttons: 1. Place(+)  2. Story(Share)  3. Trash  4. More(...)
        int iconSize = CleanupUiStyle.dp(activity, 36);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.leftMargin = CleanupUiStyle.dp(activity, 2);

        // 1. Personal place / location add
        headerRow.addView(PersonalPlaceDetailPresenter.createHeaderAction(activity, detail, selection), iconParams);

        // 2. Story
        headerRow.addView(MemoryManagementEntry.detailStoryAction(activity, detail, selection), iconParams);

        // 3. Trash / Organize
        headerRow.addView(MemoryManagementEntry.detailOptions(activity, detail, selection), iconParams);

        // 4. More Options (...)
        ImageView moreBtn = new ImageView(activity);
        moreBtn.setScaleType(ImageView.ScaleType.CENTER);
        moreBtn.setImageDrawable(MediaVectorIconDrawable.more(CleanupUiStyle.PURPLE, CleanupUiStyle.dp(activity, 20)));
        int inset = CleanupUiStyle.dp(activity, 8);
        moreBtn.setPadding(inset, inset, inset, inset);
        moreBtn.setContentDescription("더보기");
        TypedValue selectable = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, selectable, true);
        moreBtn.setBackgroundResource(selectable.resourceId);
        moreBtn.setOnClickListener(v -> {
            if (onAddAlbum != null) {
                String label = MemoryAlbumAction.label(detail.record);
                MemoryPlaceActionsPopup.showAlbumMenu(v, label, onAddAlbum);
            }
        });
        if (onAddAlbum != null) headerRow.addView(moreBtn, iconParams);

        // Row 1.5: Compact subline (Counts · Date)
        StringBuilder sub = new StringBuilder();
        if (detail.mediaResolved && !"불러오기 필요".equals(detail.item.countText)) {
            MemoryMediaCounts counts = MemoryMediaCounts.fromSections(detail.photoSections);
            sub.append("사진 ").append(number(counts.photos)).append(" · 동영상 ").append(number(counts.videos));
        } else if (!"불러오기 필요".equals(detail.item.countText)) {
            sub.append("사진·동영상 ").append(number(detail.record.itemCount));
        }
        if (!detail.item.dateText.isEmpty()) {
            if (sub.length() > 0) {
                sub.append("  ·  ");
            }
            sub.append(detail.item.dateText);
        }

        if (sub.length() > 0) {
            TextView subline = CleanupUiStyle.text(activity, sub.toString(), 11, false, CleanupUiStyle.MUTED);
            subline.setSingleLine(true);
            subline.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
            subParams.leftMargin = CleanupUiStyle.dp(activity, 40);
            subParams.bottomMargin = CleanupUiStyle.dp(activity, 6);
            root.addView(subline, subParams);
        }
    }

    private static String number(int n) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(n);
    }
}

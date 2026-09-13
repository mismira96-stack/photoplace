package com.example.gallerysorter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Renders collection membership selection without owning persistence or screen state. */
final class MemoryCollectionSelectionRenderer {
    interface Listener {
        void onBack();

        void onCreateRequested();
    }

    private final MainActivity activity;
    private final Listener listener;

    MemoryCollectionSelectionRenderer(MainActivity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void render(final List<MemoryRecord> records, Set<String> groupedIds,
                Map<String, String> aliases, final Set<String> selectedKeys) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(-197377);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(activity.dp(18), activity.dp(56), activity.dp(18), activity.dp(24));
        scroll.addView(root, activity.scrollContentLayoutParams());
        activity.addMemoryHeader(root, "기억 모으기", new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onBack();
                }
            }
        });
        TextView hint = activity.bodyText("함께 기억하고 싶은 장소를 2곳 이상 선택하세요.");
        root.addView(hint, activity.matchWidthWithBottom(activity.dp(14)));

        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        root.addView(rows, activity.matchWidthWithBottom(activity.dp(12)));
        TextView selectedCount = activity.bodyText(selectedKeys.size() + "곳 선택");
        selectedCount.setTextSize(15.0f);
        selectedCount.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        selectedCount.setPadding(activity.dp(4), activity.dp(8), activity.dp(4), activity.dp(8));
        root.addView(selectedCount, activity.matchWidth());
        final Button create = new Button(activity);
        updateCreateButton(create, selectedKeys.size());
        root.addView(create, activity.matchWidthWithBottom(activity.dp(12)));

        for (final MemoryRecord record : records) {
            final MemoryBrowserItem item = MemoryBrowserItem.from(record);
            if (item == null) {
                continue;
            }
            String stableId = aliases == null ? null : aliases.get(record.memoryKey);
            boolean alreadyGrouped = stableId != null && groupedIds != null && groupedIds.contains(stableId);
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(activity.dp(10), activity.dp(8), activity.dp(12), activity.dp(8));
            boolean initiallySelected = selectedKeys.contains(record.memoryKey);
            applyRowBackground(row, initiallySelected, alreadyGrouped);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = activity.dp(8);
            rows.addView(row, rowParams);

            ImageView cover = new ImageView(activity);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(activity.dp(58), activity.dp(58));
            coverParams.setMargins(0, 0, activity.dp(10), 0);
            row.addView(cover, coverParams);
            activity.loadMemoryBrowserThumbnailInto(cover, item.coverUri, activity.dp(80));

            LinearLayout text = new LinearLayout(activity);
            text.setOrientation(LinearLayout.VERTICAL);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView title = activity.bodyText(item.title);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            text.addView(title, activity.matchWidth());
            TextView count = activity.bodyText(item.countText
                    + (alreadyGrouped ? " · 다른 모음에 포함됨" : ""));
            count.setTextSize(12.0f);
            count.setPadding(0, activity.dp(3), 0, 0);
            text.addView(count, activity.matchWidth());
            if (!item.cardDateText.isEmpty()) {
                TextView date = activity.bodyText(item.cardDateText);
                date.setTextSize(12.0f);
                date.setTextColor(Color.rgb(120, 129, 147));
                date.setPadding(0, activity.dp(3), 0, 0);
                text.addView(date, activity.matchWidth());
            }

            final TextView check = new TextView(activity);
            check.setGravity(Gravity.CENTER);
            check.setTextSize(16.0f);
            check.setTextColor(Color.WHITE);
            check.setContentDescription(alreadyGrouped
                    ? "이미 다른 기억 모음에 포함된 장소"
                    : initiallySelected ? "선택됨" : "선택되지 않음");
            int checkSize = activity.dp(28);
            row.addView(check, new LinearLayout.LayoutParams(checkSize, checkSize));
            bindSelectionIndicator(check, initiallySelected, alreadyGrouped);
            if (!alreadyGrouped) {
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean isSelected = !selectedKeys.contains(record.memoryKey);
                        if (isSelected) {
                            selectedKeys.add(record.memoryKey);
                        } else {
                            selectedKeys.remove(record.memoryKey);
                        }
                        applyRowBackground(row, isSelected, false);
                        bindSelectionIndicator(check, isSelected, false);
                        updateCreateButton(create, selectedKeys.size());
                        selectedCount.setText(selectedKeys.size() + "곳 선택");
                    }
                });
            }
        }
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedKeys.size() >= MemoryCollection.MIN_MEMBER_COUNT && listener != null) {
                    listener.onCreateRequested();
                }
            }
        });
        activity.setContentViewWithBottomTabs(scroll, 1);
    }

    private void updateCreateButton(Button button, int selectedCount) {
        button.setEnabled(selectedCount >= MemoryCollection.MIN_MEMBER_COUNT);
        activity.stylePurpleCtaButton(button, selectedCount < MemoryCollection.MIN_MEMBER_COUNT
                ? "2곳 이상 선택해 주세요" : "선택한 " + selectedCount + "곳 모으기");
    }

    private void applyRowBackground(LinearLayout row, boolean selected, boolean disabled) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(disabled ? Color.rgb(248, 249, 251)
                : selected ? Color.rgb(247, 245, 255) : Color.WHITE);
        background.setCornerRadius(activity.dp(18));
        background.setStroke(activity.dp(selected ? 1 : 1), selected
                ? Color.rgb(167, 139, 250) : Color.rgb(231, 233, 239));
        row.setBackground(background);
        row.setClickable(!disabled);
        row.setFocusable(!disabled);
    }

    private void bindSelectionIndicator(TextView indicator, boolean selected, boolean disabled) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(selected ? Color.rgb(104, 82, 226) : Color.TRANSPARENT);
        background.setStroke(activity.dp(2), selected
                ? Color.rgb(104, 82, 226)
                : disabled ? Color.rgb(205, 210, 219) : Color.rgb(157, 163, 173));
        indicator.setBackground(background);
        indicator.setText(selected ? "✓" : "");
        indicator.setAlpha(disabled ? 0.7f : 1.0f);
        indicator.setContentDescription(disabled
                ? "이미 다른 기억 모음에 포함된 장소"
                : selected ? "선택됨" : "선택되지 않음");
    }
}

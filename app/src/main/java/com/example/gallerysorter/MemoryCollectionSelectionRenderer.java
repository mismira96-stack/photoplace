package com.example.gallerysorter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private String searchQuery = "";

    MemoryCollectionSelectionRenderer(MainActivity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void render(final List<MemoryRecord> records, final Set<String> groupedIds,
                final Map<String, String> aliases, final Set<String> selectedKeys) {
        int maxWidth = activity.scrollContentLayoutParams().width;

        LinearLayout topSection = new LinearLayout(activity);
        topSection.setOrientation(LinearLayout.VERTICAL);
        topSection.setPadding(activity.dp(18), activity.dp(52), activity.dp(18), activity.dp(10));
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(maxWidth, -2);
        topParams.gravity = Gravity.CENTER_HORIZONTAL;

        final TextView selectionBadge = createSelectionBadge(selectedKeys.size());
        addHeaderWithBadge(topSection, selectionBadge);

        final LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);

        final Button create = new Button(activity);
        updateCreateButton(create, selectedKeys.size());

        EditText searchInput = createSearchInput();
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchQuery = editable == null ? "" : editable.toString();
                populateRows(rows, records, groupedIds, aliases, selectedKeys, selectionBadge, create);
            }
        });
        topSection.addView(searchInput, activity.matchWidth());

        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(-197377);
        scroll.setFillViewport(true);
        LinearLayout scrollContent = new LinearLayout(activity);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(activity.dp(18), activity.dp(4), activity.dp(18), activity.dp(16));
        scroll.addView(scrollContent, activity.scrollContentLayoutParams());
        scrollContent.addView(rows, activity.matchWidth());

        LinearLayout footerWrap = new LinearLayout(activity);
        footerWrap.setOrientation(LinearLayout.VERTICAL);
        footerWrap.setPadding(activity.dp(18), activity.dp(8), activity.dp(18), activity.dp(10));
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(maxWidth, -2);
        footerParams.gravity = Gravity.CENTER_HORIZONTAL;

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedKeys.size() >= MemoryCollection.MIN_MEMBER_COUNT && listener != null) {
                    listener.onCreateRequested();
                }
            }
        });
        footerWrap.addView(create, activity.matchWidth());

        populateRows(rows, records, groupedIds, aliases, selectedKeys, selectionBadge, create);
        activity.setContentViewWithBottomTabs(scroll, 1);
        if (scroll.getParent() instanceof LinearLayout) {
            LinearLayout container = (LinearLayout) scroll.getParent();
            container.addView(topSection, 0, topParams);
            container.addView(footerWrap, Math.min(2, container.getChildCount()), footerParams);
        } else {
            scrollContent.addView(topSection, 0, activity.matchWidthWithBottom(activity.dp(8)));
            scrollContent.addView(footerWrap, activity.matchWidth());
        }
    }

    private void addHeaderWithBadge(LinearLayout root, TextView badge) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, activity.matchWidthWithBottom(activity.dp(12)));

        TextView back = new TextView(activity);
        back.setText("‹");
        back.setTextSize(34.0f);
        back.setGravity(Gravity.CENTER);
        back.setTextColor(Color.rgb(17, 24, 39));
        back.setContentDescription("뒤로 가기");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onBack();
                }
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(activity.dp(40), activity.dp(42)));

        TextView title = new TextView(activity);
        title.setText("기억 모으기");
        title.setTextSize(22.0f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(17, 24, 39));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        header.addView(badge, new LinearLayout.LayoutParams(-2, -2));
    }

    private TextView createSelectionBadge(int count) {
        TextView badge = new TextView(activity);
        badge.setTextSize(12.5f);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.rgb(104, 82, 226));
        badge.setPadding(activity.dp(10), activity.dp(5), activity.dp(10), activity.dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(244, 241, 255));
        bg.setCornerRadius(activity.dp(12));
        badge.setBackground(bg);
        updateSelectionBadge(badge, count);
        return badge;
    }

    private void updateSelectionBadge(TextView badge, int count) {
        badge.setText("선택 " + count + "곳");
    }

    private EditText createSearchInput() {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setTextSize(14.5f);
        input.setTextColor(Color.rgb(17, 24, 39));
        input.setHintTextColor(Color.rgb(140, 148, 164));
        input.setHint("장소, 국가, 날짜로 검색");
        input.setIncludeFontPadding(false);
        input.setPadding(activity.dp(14), 0, activity.dp(14), 0);
        input.setMinHeight(activity.dp(44));
        input.setCompoundDrawablePadding(activity.dp(8));
        int searchIconSize = activity.dp(18);
        MediaVectorIconDrawable searchIcon = MediaVectorIconDrawable.search(
                Color.rgb(120, 129, 147), searchIconSize);
        searchIcon.setBounds(0, 0, searchIconSize, searchIconSize);
        input.setCompoundDrawables(searchIcon, null, null, null);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(activity.dp(14));
        bg.setStroke(activity.dp(1), Color.rgb(231, 234, 242));
        input.setBackground(bg);
        return input;
    }

    private void populateRows(LinearLayout rows,
                              List<MemoryRecord> records,
                              Set<String> groupedIds,
                              Map<String, String> aliases,
                              final Set<String> selectedKeys,
                              final TextView selectionBadge,
                              final Button create) {
        rows.removeAllViews();
        List<MemoryRecord> filtered = MemoryBrowserSearch.filter(records, searchQuery);
        if (filtered.isEmpty()) {
            LinearLayout empty = new LinearLayout(activity);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(activity.dp(16), activity.dp(28), activity.dp(16), activity.dp(28));
            activity.applyCardBackground(empty);
            TextView emptyTitle = new TextView(activity);
            emptyTitle.setText("일치하는 장소가 없어요");
            emptyTitle.setTextSize(15.0f);
            emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
            emptyTitle.setTextColor(Color.rgb(31, 35, 48));
            emptyTitle.setGravity(Gravity.CENTER);
            empty.addView(emptyTitle);
            rows.addView(empty, activity.matchWidth());
            return;
        }

        for (final MemoryRecord record : filtered) {
            final MemoryBrowserItem item = MemoryBrowserItem.from(record);
            if (item == null) {
                continue;
            }
            String stableId = aliases == null ? null : aliases.get(record.memoryKey);
            boolean alreadyGrouped = stableId != null && groupedIds != null && groupedIds.contains(stableId);
            final LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(activity.dp(12), activity.dp(10), activity.dp(14), activity.dp(10));
            boolean initiallySelected = selectedKeys.contains(record.memoryKey);
            applyRowBackground(row, initiallySelected, alreadyGrouped);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = activity.dp(8);
            rows.addView(row, rowParams);

            ImageView cover = new ImageView(activity);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable thumbBg = new GradientDrawable();
            thumbBg.setColor(Color.rgb(240, 242, 247));
            thumbBg.setCornerRadius(activity.dp(14));
            cover.setBackground(thumbBg);
            cover.setClipToOutline(true);
            LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(activity.dp(60), activity.dp(60));
            coverParams.setMargins(0, 0, activity.dp(12), 0);
            row.addView(cover, coverParams);
            activity.loadMemoryBrowserThumbnailInto(cover, item.coverUri, activity.dp(96));

            LinearLayout text = new LinearLayout(activity);
            text.setOrientation(LinearLayout.VERTICAL);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1.0f));

            LinearLayout titleRow = new LinearLayout(activity);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            text.addView(titleRow, activity.matchWidth());

            TextView title = new TextView(activity);
            title.setText(item.title);
            title.setTextSize(16.0f);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(alreadyGrouped ? Color.rgb(140, 148, 164) : Color.rgb(17, 24, 39));
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

            if (alreadyGrouped) {
                TextView groupedBadge = new TextView(activity);
                groupedBadge.setText("모음 포함됨");
                groupedBadge.setTextSize(10.5f);
                groupedBadge.setTypeface(Typeface.DEFAULT_BOLD);
                groupedBadge.setTextColor(Color.rgb(120, 129, 147));
                groupedBadge.setPadding(activity.dp(7), activity.dp(2), activity.dp(7), activity.dp(2));
                GradientDrawable gBg = new GradientDrawable();
                gBg.setColor(Color.rgb(236, 239, 245));
                gBg.setCornerRadius(activity.dp(8));
                groupedBadge.setBackground(gBg);
                LinearLayout.LayoutParams gbParams = new LinearLayout.LayoutParams(-2, -2);
                gbParams.leftMargin = activity.dp(6);
                titleRow.addView(groupedBadge, gbParams);
            }

            String countLabel = SingleAlbumCompletionResolver.formatMediaCount(record.photoCount, record.videoCount);
            TextView count = MemoryCardCountRenderer.render(activity, countLabel);
            if (alreadyGrouped) {
                count.setAlpha(0.55f);
            }
            text.addView(count, activity.matchWidth());

            if (!item.cardDateText.isEmpty()) {
                TextView date = new TextView(activity);
                date.setText(item.cardDateText);
                date.setTextSize(12.0f);
                date.setTextColor(Color.rgb(100, 111, 132));
                date.setPadding(0, activity.dp(2), 0, 0);
                text.addView(date, activity.matchWidth());
            }

            final TextView check = new TextView(activity);
            check.setGravity(Gravity.CENTER);
            check.setTextSize(14.0f);
            check.setTypeface(Typeface.DEFAULT_BOLD);
            check.setTextColor(Color.WHITE);
            int checkSize = activity.dp(26);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(checkSize, checkSize);
            checkParams.leftMargin = activity.dp(10);
            row.addView(check, checkParams);
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
                        updateSelectionBadge(selectionBadge, selectedKeys.size());
                        updateCreateButton(create, selectedKeys.size());
                    }
                });
            }
        }
    }

    private void updateCreateButton(Button button, int selectedCount) {
        boolean ready = selectedCount >= MemoryCollection.MIN_MEMBER_COUNT;
        button.setEnabled(ready);
        activity.stylePurpleCtaButton(button, !ready
                ? "장소를 2곳 이상 선택해 주세요" : "선택한 " + selectedCount + "곳 모으기");
        button.setAlpha(ready ? 1.0f : 0.55f);
    }

    private void applyRowBackground(LinearLayout row, boolean selected, boolean disabled) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(disabled ? Color.rgb(246, 247, 250)
                : selected ? Color.rgb(245, 242, 255) : Color.WHITE);
        background.setCornerRadius(activity.dp(18));
        background.setStroke(activity.dp(selected ? 2 : 1), selected
                ? Color.rgb(104, 82, 226) : Color.rgb(231, 234, 242));
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
                : disabled ? Color.rgb(212, 216, 226) : Color.rgb(180, 187, 200));
        indicator.setBackground(background);
        indicator.setText(selected ? "✓" : "");
        indicator.setAlpha(disabled ? 0.5f : 1.0f);
        indicator.setContentDescription(disabled
                ? "이미 다른 기억 모음에 포함된 장소"
                : selected ? "선택됨" : "선택되지 않음");
    }
}

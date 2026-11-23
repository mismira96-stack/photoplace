package com.example.gallerysorter;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

/** Selection belongs to one bound discovery detail, including search results and later pages. */
final class MemoryDetailSelection {
    private final Activity activity;
    private final View screen;
    private final Executor worker;
    private final MemoryBrowserDetail detail;
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();
    private final Map<FrameLayout, String> thumbnails = new WeakHashMap<>();
    private final TextView count;
    private boolean openingStory;
    private Runnable selectionListener;
    private String storyTitleOverride = "";

    MemoryDetailSelection(Activity activity, View screen, Executor worker, MemoryBrowserDetail detail) {
        this.activity = activity;
        this.screen = screen;
        this.worker = worker;
        this.detail = detail;
        count = CleanupUiStyle.text(activity, "", 13, true, CleanupUiStyle.PURPLE);
        count.setMinHeight(CleanupUiStyle.dp(activity, 48));
        count.setGravity(Gravity.CENTER_VERTICAL);
        count.setTooltipText("선택 해제");
        count.setOnClickListener(view -> clear());
        count.setVisibility(View.GONE);
    }

    View countView() { return count; }

    List<String> selectedUris() {
        return new ArrayList<>(selected);
    }

    void setSelectionListener(Runnable listener) {
        this.selectionListener = listener;
    }

    void setStoryTitleOverride(String titleOverride) {
        this.storyTitleOverride = titleOverride == null ? "" : titleOverride.trim();
    }

    void bind(FrameLayout tile, MemoryPhotoItem item, Runnable open) {
        TextView check = CleanupUiStyle.text(activity, "✓", 15, true, Color.WHITE);
        check.setGravity(Gravity.CENTER);
        check.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(CleanupUiStyle.PURPLE);
        int inset = CleanupUiStyle.dp(activity, 6);
        background.setStroke(CleanupUiStyle.dp(activity, 1), Color.WHITE);
        check.setBackground(background);
        int size = CleanupUiStyle.dp(activity, 24);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.END);
        params.setMargins(inset, inset, inset, inset);
        tile.addView(check, params);
        thumbnails.put(tile, item.sourceUri);
        tile.setOnClickListener(view -> {
            if (selected.isEmpty()) open.run();
            else toggle(item.sourceUri);
        });
        tile.setOnLongClickListener(view -> { toggle(item.sourceUri); return true; });
        updateTile(tile, item.sourceUri);
    }

    private void toggle(String uri) {
        if (!selected.add(uri)) selected.remove(uri);
        update();
    }

    boolean clear() {
        if (!screen.isAttachedToWindow() || selected.isEmpty()) return false;
        selected.clear();
        update();
        return true;
    }

    void story(Runnable openPicker) {
        if (selected.isEmpty()) {
            openPicker.run();
            return;
        }
        if (openingStory) return;
        openingStory = true;
        List<String> requested = new ArrayList<>(selected);
        worker.execute(() -> {
            try {
                List<UnresolvedMediaRecord> rows = new MemoryMediaAccess(activity).observeExactUris(requested);
                activity.runOnUiThread(() -> {
                    openingStory = false;
                    if (screen.isAttachedToWindow() && requested.equals(new ArrayList<>(selected))) {
                        String storyTitle = storyTitleOverride.isEmpty() ? detail.item.title : storyTitleOverride;
                        MemoryStoryShareSheet.showForSelection(activity, storyTitle, rows);
                    }
                });
            } catch (Exception error) {
                activity.runOnUiThread(() -> {
                    openingStory = false;
                    if (screen.isAttachedToWindow()) Toast.makeText(activity,
                            "선택한 사진을 다시 확인해 주세요.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    void trash(Runnable openPicker) {
        if (selected.isEmpty()) openPicker.run();
        else MemoryManagementEntry.openSelectedTrash(activity, detail, new ArrayList<>(selected));
    }

    private void update() {
        count.setVisibility(selected.isEmpty() ? View.GONE : View.VISIBLE);
        count.setText("선택 " + selected.size() + "개  ×");
        count.setContentDescription("선택 " + selected.size() + "개, 선택 해제");
        thumbnails.forEach(this::updateTile);
        if (selectionListener != null) {
            selectionListener.run();
        }
    }

    private void updateTile(FrameLayout tile, String uri) {
        boolean checked = selected.contains(uri);
        tile.getChildAt(tile.getChildCount() - 1).setVisibility(checked ? View.VISIBLE : View.GONE);
        tile.setSelected(checked);
        tile.setContentDescription(checked ? "선택된 사진" : null);
    }

}

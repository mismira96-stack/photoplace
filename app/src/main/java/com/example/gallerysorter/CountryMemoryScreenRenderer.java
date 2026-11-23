package com.example.gallerysorter;

import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Country containers use the same place cards, pinch density and thumbnails as discovery. */
final class CountryMemoryScreenRenderer {
    static void bind(MainActivity activity, CountryMemoryLoadController.Entry entry, MemoryListDensity density,
                     BooleanSupplier wide, int windowWidth, int destination, int scrollY,
                     IntConsumer capture, Runnable back, Consumer<MemoryBrowserItem> open) {
        MemoryDensityScrollView scroll = new MemoryDensityScrollView(activity, density, wide);
        scroll.setBackgroundColor(MemoryListChrome.BACKGROUND);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(activity.dp(MemoryListChrome.SIDE_DP), activity.dp(MemoryListChrome.TOP_DP),
                activity.dp(MemoryListChrome.SIDE_DP), activity.dp(24));
        ScrollView.LayoutParams content = activity.scrollContentLayoutParams();
        scroll.addView(root, content);
        activity.addMemoryHeader(root, entry.projection.countryName, back);
        root.addView(new MemoryBrowserSummaryRenderer(activity).render(entry.browser, null, null),
                activity.matchWidthWithBottom(activity.dp(12)));
        LinearLayout results = new LinearLayout(activity);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results, activity.matchWidth());
        MemoryBrowserGridRenderer renderer = new MemoryBrowserGridRenderer(activity, item -> {
            capture.accept(scroll.getScrollY()); open.accept(item);
        }, activity::loadMemoryBrowserThumbnailInto);
        int width = renderer.availableGridWidth(content.width > 0 ? content.width : windowWidth);
        Runnable render = () -> {
            results.removeAllViews();
            results.addView(renderer.render(entry.browser.state.items, width, density.columns(wide.getAsBoolean())),
                    activity.matchWidth());
        };
        scroll.setOnDensityChanged(render);
        render.run();
        activity.setContentViewWithBottomTabs(scroll, destination);
        scroll.post(() -> scroll.scrollTo(0, scrollY));
    }
    private CountryMemoryScreenRenderer() { }
}

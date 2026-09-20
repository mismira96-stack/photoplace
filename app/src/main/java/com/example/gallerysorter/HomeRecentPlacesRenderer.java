package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Renders only Discovery-backed Home cards; selection and ordering belong to the resolver. */
final class HomeRecentPlacesRenderer {
    interface Host {
        int dp(int value);
        TextView sectionTitle(String text);
        TextView cardTitle(String text);
        TextView meta(String text);
        void styleCard(View view);
        void loadCover(ImageView image, String uri, int size);
        void openMemory(HomeRecentPlacesResolver.Card card);
        void openBrowser();
    }

    static void render(Context context, LinearLayout container,
                       List<HomeRecentPlacesResolver.Card> cards, Host host) {
        if (cards.isEmpty()) return;
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(host.sectionTitle("최근 발견한 장소"), new LinearLayout.LayoutParams(0, -2, 1));
        TextView all = host.meta("전체 보기");
        all.setTypeface(Typeface.DEFAULT_BOLD);
        all.setPadding(host.dp(8), host.dp(6), host.dp(2), host.dp(6));
        all.setOnClickListener(view -> host.openBrowser());
        header.addView(all);
        section.addView(header, widthWithBottom(host.dp(6)));
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(context);
        for (int i = 0; i < cards.size(); i++) {
            HomeRecentPlacesResolver.Card item = cards.get(i);
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setClickable(true);
            card.setFocusable(true);
            card.setPadding(0, 0, 0, host.dp(8));
            card.setOnClickListener(view -> host.openMemory(item));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(host.dp(124), -2);
            params.setMargins(0, 0, i == cards.size() - 1 ? 0 : host.dp(8), 0);
            row.addView(card, params);
            host.styleCard(card);
            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            card.addView(image, new LinearLayout.LayoutParams(-1, host.dp(92)));
            host.loadCover(image, item.coverUri, host.dp(92));
            TextView title = host.cardTitle(item.title);
            title.setPadding(host.dp(9), host.dp(7), host.dp(7), 0);
            card.addView(title, widthWithBottom(0));
            TextView count = host.meta(item.countText);
            count.setPadding(host.dp(9), host.dp(2), host.dp(7), host.dp(8));
            card.addView(count, widthWithBottom(0));
        }
        scroll.addView(row);
        section.addView(scroll, widthWithBottom(host.dp(8)));
        container.addView(section, widthWithBottom(host.dp(8)));
    }

    private static LinearLayout.LayoutParams widthWithBottom(int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = bottom;
        return params;
    }
}

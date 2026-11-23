package com.example.gallerysorter;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

final class HomeCountryCardsRenderer {
    interface Host {
        int dp(int value);
        int cardWidth(int count);
        int photoHeight(int count);
        TextView sectionTitle(String text);
        TextView title(String text);
        TextView meta(String text);
        void styleCard(View card);
        void loadCover(ImageView image, String uri, int size);
        void openCountry(String code);
    }
    static void render(Context context, LinearLayout container, List<HomeDashboardSnapshot.CountryCard> countries, Host host) {
        if (countries.isEmpty()) return;
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, host.dp(2), 0, host.dp(6));
        container.addView(section, widthWithBottom(host.dp(4)));
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(host.dp(2), 0, host.dp(2), 0);
        header.addView(host.sectionTitle("해외 기록"), new LinearLayout.LayoutParams(0, -2, 1));
        TextView count = host.meta(countries.size() + "개 국가");
        count.setGravity(Gravity.CENTER);
        header.addView(count);
        section.addView(header, widthWithBottom(host.dp(4)));
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < countries.size(); i++) {
            HomeDashboardSnapshot.CountryCard item = countries.get(i);
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(0, 0, 0, host.dp(7));
            card.setClickable(true); card.setFocusable(true);
            card.setOnClickListener(view -> host.openCountry(item.code));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(host.cardWidth(countries.size()), -2);
            params.rightMargin = i == countries.size() - 1 ? 0 : host.dp(8);
            row.addView(card, params);
            host.styleCard(card);
            ImageView cover = new ImageView(context);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(cover, new LinearLayout.LayoutParams(-1, host.photoHeight(countries.size())));
            if (!item.coverUri.isEmpty()) host.loadCover(cover, item.coverUri, host.dp(160));
            LinearLayout body = new LinearLayout(context);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(host.dp(10), host.dp(7), host.dp(9), host.dp(8));
            body.addView(host.title(item.name));
            TextView meta = host.meta(item.summary());
            meta.setSingleLine(false); meta.setMaxLines(2); meta.setEllipsize(null);
            body.addView(meta);
            card.addView(body, new LinearLayout.LayoutParams(-1, host.dp(76)));
        }
        scroll.addView(row);
        section.addView(scroll, widthWithBottom(host.dp(6)));
    }
    private static LinearLayout.LayoutParams widthWithBottom(int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = bottom;
        return params;
    }
}

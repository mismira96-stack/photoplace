package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class BottomNavigationRenderer {
    interface Listener {
        void onTabSelected(int index);
    }

    interface IconFactory {
        Drawable create(String iconName, int color, int sizePx);
    }

    private static final String[] ICONS = {"home", "photoLibrary", "settings"};
    private static final String[] LABELS = {"홈", "발견 기록", "설정"};

    private final Context context;
    private final IconFactory iconFactory;

    BottomNavigationRenderer(Context context, IconFactory iconFactory) {
        this.context = context;
        this.iconFactory = iconFactory;
    }

    View render(int selectedIndex, final Listener listener) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setBackground(barBackground());

        for (int index = 0; index < LABELS.length; index++) {
            final int tabIndex = DiscoveryTabDestinations.destinationAt(index);
            TextView tab = new TextView(context);
            boolean selected = DiscoveryTabDestinations.selected(index, selectedIndex);
            tab.setText(LABELS[index]);
            tab.setTextSize(12.0f);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            tab.setGravity(Gravity.CENTER);
            tab.setMaxLines(1);
            tab.setAutoSizeTextTypeUniformWithPresetSizes(new int[]{9, 10, 11, 12}, TypedValue.COMPLEX_UNIT_SP);
            tab.setTextColor(selected ? Color.rgb(109, 87, 218) : Color.rgb(100, 111, 132));
            tab.setCompoundDrawablePadding(dp(3));
            if (iconFactory != null) {
                int color = selected ? Color.rgb(109, 87, 218) : Color.rgb(100, 111, 132);
                Drawable icon = iconFactory.create(ICONS[index], color, dp(18));
                tab.setCompoundDrawables(icon, null, null, null);
            }
            tab.setPadding(dp(2), dp(9), dp(2), dp(9));
            if (selected) {
                tab.setBackground(selectedBackground());
            }
            tab.setClickable(true);
            tab.setFocusable(true);
            tab.setContentDescription(LABELS[index] + (selected ? ", 선택됨" : ""));
            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onTabSelected(tabIndex);
                    }
                }
            });
            bar.addView(tab, new LinearLayout.LayoutParams(0, -2, 1.0f));
        }
        return bar;
    }

    private GradientDrawable barBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(24));
        background.setStroke(dp(1), Color.rgb(232, 234, 240));
        return background;
    }

    private GradientDrawable selectedBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(243, 239, 255));
        background.setCornerRadius(dp(20));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

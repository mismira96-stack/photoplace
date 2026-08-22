package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class MemorySearchHeaderRenderer {
    interface Listener {
        void onSearchVisibilityChanged(boolean visible);

        void onQueryChanged(String query);
    }

    interface IconFactory {
        Drawable create(String iconName, int color, int backgroundColor, int sizePx);
    }

    private final Context context;
    private final IconFactory iconFactory;
    private final Listener listener;

    MemorySearchHeaderRenderer(Context context, IconFactory iconFactory, Listener listener) {
        this.context = context;
        this.iconFactory = iconFactory;
        this.listener = listener;
    }

    View header(boolean searchVisible, boolean searchEnabled) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text(searchVisible ? "발견 검색" : "발견", 24.0f, true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));
        if (searchEnabled) {
            TextView action = new TextView(context);
            action.setGravity(Gravity.CENTER);
            action.setClickable(true);
            action.setFocusable(true);
            action.setContentDescription(searchVisible ? "검색 닫기" : "검색 열기");
            if (iconFactory != null) {
                action.setCompoundDrawables(
                        iconFactory.create(searchVisible ? "close" : "search", Color.rgb(109, 88, 222), Color.rgb(241, 238, 255), dp(38)),
                        null,
                        null,
                        null);
            }
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onSearchVisibilityChanged(!searchVisible);
                    }
                }
            });
            header.addView(action, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        return header;
    }

    EditText input(String initialQuery) {
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setTextSize(15.0f);
        input.setTextColor(Color.rgb(25, 31, 45));
        input.setHintTextColor(Color.rgb(128, 136, 153));
        input.setHint("장소, 국가, 도시, 날짜 검색");
        input.setIncludeFontPadding(false);
        input.setPadding(dp(16), 0, dp(16), 0);
        input.setMinHeight(dp(48));
        input.setCompoundDrawablePadding(dp(10));
        if (iconFactory != null) {
            input.setCompoundDrawables(iconFactory.create("search", Color.rgb(128, 136, 153), 0, dp(22)), null, null, null);
        }
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), Color.rgb(230, 232, 238));
        input.setBackground(background);
        input.setText(initialQuery == null ? "" : initialQuery);
        input.setSelection(input.getText().length());
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (listener != null) {
                    listener.onQueryChanged(editable == null ? "" : editable.toString());
                }
            }
        });
        return input;
    }

    private TextView text(String value, float sizeSp, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(Color.rgb(17, 24, 39));
        if (bold) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return text;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

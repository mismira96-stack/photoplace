package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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

    private final Context context;
    private final Listener listener;

    MemorySearchHeaderRenderer(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    View header(boolean searchVisible, boolean searchEnabled, Runnable makeAlbum) {
        return header(searchVisible, searchEnabled, makeAlbum, null, null, false);
    }

    View header(boolean searchVisible, boolean searchEnabled, Runnable makeAlbum,
                View addPlaceAction, Runnable collectMemories, boolean wide) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("발견 기록", 24.0f);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        if (wide && searchEnabled) {
            if (addPlaceAction != null) {
                header.addView(addPlaceAction);
            }
            if (collectMemories != null) {
                header.addView(MemoryAddActionIcon.collectMemories(context, collectMemories));
            }
        }

        if (searchEnabled) {
            View action = MemoryIconButton.search(context, searchVisible, false);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onSearchVisibilityChanged(!searchVisible);
                    }
                }
            });
            header.addView(action);
            header.addView(MemoryAddActionIcon.albumMenu(context, makeAlbum));
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
        input.setCompoundDrawablesWithIntrinsicBounds(
                MediaVectorIconDrawable.search(CleanupUiStyle.MUTED, dp(22)), null, null, null);
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

    private TextView text(String value, float sizeSp) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(Color.rgb(17, 24, 39));
        text.setTypeface(Typeface.create(Typeface.DEFAULT, 500, false));
        return text;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

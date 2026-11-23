package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/** One fixed-height count row, with exact numbers and a complete TalkBack label. */
final class MemoryCardCountRenderer {
    private MemoryCardCountRenderer() { }

    static TextView render(Context context, String source) {
        MemoryCardCountPresentation model = MemoryCardCountPresentation.from(source);
        TextView view = new TextView(context);
        view.setTextColor(Color.rgb(104, 82, 226));
        view.setTextSize(15);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(0, dp(context, 3), 0, 0);
        view.setHeight(view.getPaint().getFontMetricsInt(null) + dp(context, 3));
        view.setMaxLines(1);
        view.setHorizontallyScrolling(false);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setAutoSizeTextTypeUniformWithConfiguration(11, 15, 1, TypedValue.COMPLEX_UNIT_SP);
        view.setContentDescription(model.displayText());
        SpannableStringBuilder text = new SpannableStringBuilder();
        int purple = Color.rgb(104, 82, 226);
        int iconSize = dp(context, 14);
        if (!model.photoLabel().isEmpty()) {
            add(text, MediaVectorIconDrawable.photo(purple, iconSize), iconSize, model.photoLabel());
        }
        if (!model.videoLabel().isEmpty()) {
            if (text.length() > 0) text.append("   ");
            add(text, MediaVectorIconDrawable.video(purple, iconSize), iconSize, model.videoLabel());
        }
        view.setText(text.length() == 0 ? model.fallback : text);
        return view;
    }

    private static void add(SpannableStringBuilder text, Drawable drawable, int iconSize, String label) {
        int start = text.length();
        text.append("\uFFFC ").append(label);
        drawable.setBounds(0, 0, iconSize, iconSize);
        text.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_CENTER), start, start + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

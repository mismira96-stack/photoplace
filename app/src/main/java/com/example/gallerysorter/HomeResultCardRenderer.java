package com.example.gallerysorter;

import android.app.Activity;
import android.view.*;
import android.widget.*;

/** Compact result summary; the discovery CTA already explains the flow. */
final class HomeResultCardRenderer {
    static void compact(Activity a,LinearLayout card,TextView summary,LinearLayout footer,TextView repeatedHint) {
        card.setPadding(CleanupUiStyle.dp(a,16),CleanupUiStyle.dp(a,10),CleanupUiStyle.dp(a,14),CleanupUiStyle.dp(a,10));
        summary.setTextSize(13);summary.setMaxLines(2);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        repeatedHint.setVisibility(View.GONE);footer.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        footer.setPadding(0,CleanupUiStyle.dp(a,4),0,0);
    }
}

package com.example.gallerysorter;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Quiet, inline completion feedback below the discovery action. */
final class PreviewRefreshNoticeRenderer {
    private static final String TAG = "preview-refresh-notice";

    static void checking(View anchor) {
        show(anchor, "새 사진·동영상을 확인하고 있어요.");
        if (anchor == null) return;
        anchor.animate().cancel();
        anchor.animate().scaleX(0.985f).scaleY(0.985f).setDuration(75)
                .withEndAction(() -> anchor.animate().scaleX(1f).scaleY(1f).setDuration(110).start())
                .start();
    }

    static void clear(View anchor) {
        if (anchor == null || !(anchor.getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) anchor.getParent();
        View previous = parent.findViewWithTag(TAG);
        if (previous != null) parent.removeView(previous);
    }

    static void completed(View anchor,View resultCard,String message) {
        if(resultCard!=null&&resultCard.getVisibility()==View.VISIBLE)clear(anchor);
        else show(anchor,message);
    }

    static void show(View anchor, String message) {
        if (anchor == null || !(anchor.getParent() instanceof LinearLayout)) return;
        LinearLayout parent = (LinearLayout) anchor.getParent();
        View previous = parent.findViewWithTag(TAG);
        if (previous instanceof TextView) {
            ((TextView) previous).setText(message);
            return;
        }
        TextView notice = new TextView(anchor.getContext());
        notice.setTag(TAG);
        notice.setText(message);
        notice.setTextSize(13);
        notice.setMinLines(1);
        notice.setTextColor(Color.rgb(87, 101, 113));
        int spacing = Math.round(12 * anchor.getResources().getDisplayMetrics().density);
        notice.setPadding(spacing, spacing / 2, spacing, spacing);
        parent.addView(notice, parent.indexOfChild(anchor) + 1,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}

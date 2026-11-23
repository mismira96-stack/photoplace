package com.example.gallerysorter;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Always binds a hidden slot so a job started on an existing screen can reveal progress. */
final class WorkingProgressBanner extends LinearLayout {
    private static final Object TAG = new Object();
    private final TextView title, detail;
    private final ProgressBar progress;

    private WorkingProgressBanner(LinearLayout parent) {
        super(parent.getContext());
        setTag(TAG);
        setOrientation(VERTICAL);
        setPadding(dp(16), dp(13), dp(16), dp(13));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), Color.rgb(231, 234, 242));
        setBackground(background);
        title = new TextView(getContext());
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(17, 24, 39));
        addView(title, new LayoutParams(-1, -2));
        detail = new TextView(getContext());
        detail.setTextSize(13);
        detail.setTextColor(Color.rgb(100, 111, 132));
        LayoutParams detailLayout = new LayoutParams(-1, -2);
        detailLayout.topMargin = dp(4);
        addView(detail, detailLayout);
        progress = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        LayoutParams progressLayout = new LayoutParams(-1, dp(8));
        progressLayout.topMargin = dp(8);
        addView(progress, progressLayout);
        LayoutParams layout = new LayoutParams(-1, -2);
        layout.bottomMargin = dp(16);
        parent.addView(this, layout);
    }

    static void attach(LinearLayout parent, boolean working, String message, String label, int current, int total) {
        new WorkingProgressBanner(parent).render(
                WorkingProgressPresentation.from(working, message, label, current, total));
    }

    static void update(Activity activity, boolean working, String message, String label, int current, int total) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        View banner = content.findViewWithTag(TAG);
        if (banner instanceof WorkingProgressBanner) {
            ((WorkingProgressBanner) banner).render(
                    WorkingProgressPresentation.from(working, message, label, current, total));
        }
    }

    private void render(WorkingProgressPresentation state) {
        setVisibility(state.visible ? VISIBLE : GONE);
        title.setText(state.title);
        detail.setText(state.detail);
        progress.setIndeterminate(state.indeterminate);
        progress.setMax(Math.max(1, state.total));
        progress.setProgress(state.current);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}

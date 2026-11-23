package com.example.gallerysorter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/** Dedicated full-height progress view. No media access or operation state lives in this view. */
final class CleanupProgressScreen extends Dialog {
    private final TextView heading, detail;
    private final TextView[] stages = new TextView[3];
    private final ProgressBar progress;
    private final Context context;

    CleanupProgressScreen(Context context, Runnable onCancel) {
        super(context);
        this.context = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        setOnCancelListener(dialog -> onCancel.run());
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(20));
        root.setBackground(CleanupUiStyle.card(context,CleanupUiStyle.BACKGROUND));
        root.addView(CleanupUiStyle.header(context,"원본 검사",this::cancel));
        ScrollView scroll = CleanupUiStyle.scroll(context);
        LinearLayout content = new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0,dp(16),0,dp(16));scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        TextView subtitle = text("위치 앨범에 복사된 사진을 확인해요", 13, false);
        subtitle.setTextColor(CleanupUiStyle.MUTED);
        subtitle.setPadding(0, 0, 0, dp(12));
        content.addView(subtitle);
        String[] labels = {"1. 사진 찾기", "2. 내용 비교", "3. 기록 확인"};
        LinearLayout stageRow = new LinearLayout(context);
        content.addView(stageRow);
        for (int i = 0; i < stages.length; i++) {
            stages[i] = text(labels[i], 11, true);
            stages[i].setGravity(Gravity.CENTER);
            stages[i].setPadding(dp(4), dp(10), dp(4), dp(10));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2,1);
            if(i<2)params.setMarginEnd(dp(6));
            stageRow.addView(stages[i], params);
        }
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(CleanupUiStyle.card(context,Color.WHITE));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(14);
        content.addView(card, cardParams);
        heading = text("사진을 찾고 있어요", 16, true);
        card.addView(heading);
        detail = text("사진은 그대로 보관돼요", 13, false);
        detail.setTextColor(CleanupUiStyle.MUTED);
        detail.setPadding(0, dp(6), 0, dp(12));
        card.addView(detail);
        progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(CleanupUiStyle.PROGRESS));
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(CleanupUiStyle.PROGRESS));
        card.addView(progress, new LinearLayout.LayoutParams(-1, dp(6)));
        TextView notice = text("검사가 끝나면 정리할 원본을 직접 선택할 수 있어요.\n검사 중에는 사진을 이동하지 않아요.", 12, false);
        notice.setTextColor(CleanupUiStyle.MUTED);
        notice.setPadding(0, dp(16), 0, dp(16));
        root.addView(notice);
        Button stop = CleanupUiStyle.button(context,"검사 중지",false);
        stop.setOnClickListener(view -> cancel());
        root.addView(stop, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        stage("FINDING");
    }

    @Override public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().setLayout(context.getResources().getDisplayMetrics().widthPixels-dp(32), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    void stage(String stage) {
        int active = "CONTENT".equals(stage) ? 1 : "CONNECTION".equals(stage) ? 2 : 0;
        for (int i = 0; i < stages.length; i++) {
            stages[i].setTextColor(i == active ? CleanupUiStyle.PURPLE : Color.rgb(115, 121, 137));
            stages[i].setBackground(background(i == active ? 0xffede8fc : Color.TRANSPARENT));
        }
        progress.setIndeterminate(true);
        heading.setText(active == 0 ? "사진을 찾고 있어요" : active == 1 ? "사진 내용을 비교하고 있어요" : "기록 연결을 확인하고 있어요");
        detail.setText(active == 2 ? "원본 없이도 사진·메모·모음을 볼 수 있는지 확인해요" : "사진은 이동하거나 삭제하지 않아요");
    }
    void update(int checked, int total) {
        progress.setIndeterminate(false);
        progress.setProgress(total > 0 ? (int) Math.min(100, checked * 100L / total) : 0);
        detail.setText(CleanupReviewDiagnostics.number(checked) + " / " + CleanupReviewDiagnostics.number(total) + "건 비교 완료");
    }
    void connectionUpdate(int checked, int total) {
        progress.setIndeterminate(false);
        progress.setProgress(total > 0 ? (int)Math.min(100, checked * 100L / total) : 0);
        detail.setText("기록 " + CleanupReviewDiagnostics.number(checked) + " / " + CleanupReviewDiagnostics.number(total) + " 연결 확인 중");
    }
    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value); view.setTextSize(size);
        view.setTextColor(Color.rgb(36, 43, 62));
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }
    private GradientDrawable background(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color); drawable.setCornerRadius(dp(16)); return drawable;
    }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}

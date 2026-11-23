package com.example.gallerysorter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/** Execution, continuation and recovery share one full-height, horizontal-progress surface. */
final class CleanupExecutionScreen extends Dialog {
    private final TextView title, detail, count;
    private final ProgressBar bar;
    private final Button primary, secondary;
    private String message = "";
    CleanupExecutionScreen(Context context, Runnable hide) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(false);
        setOnCancelListener(d -> hide.run());
        LinearLayout root = new LinearLayout(context); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(16),dp(20),dp(20)); root.setBackground(CleanupUiStyle.card(context,CleanupUiStyle.BACKGROUND));
        root.addView(CleanupUiStyle.header(context,"원본 정리",this::cancel));
        ScrollView scroll = CleanupUiStyle.scroll(context);
        LinearLayout content = new LinearLayout(context); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0,dp(20),0,dp(16)); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(CleanupUiStyle.card(context,Color.WHITE));
        content.addView(card,new LinearLayout.LayoutParams(-1,-2));
        title = text("진행 상태를 확인하고 있어요",16,true); card.addView(title);
        count = text("",20,true); count.setTextColor(CleanupUiStyle.PURPLE); count.setPadding(0,dp(12),0,dp(10)); card.addView(count);
        bar = new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(CleanupUiStyle.PROGRESS));
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(CleanupUiStyle.PROGRESS));
        card.addView(bar,new LinearLayout.LayoutParams(-1,dp(6)));
        detail = text("",13,false);detail.setTextColor(CleanupUiStyle.MUTED); detail.setPadding(0,dp(12),0,0); card.addView(detail);
        TextView note=text("위치 앨범은 그대로 유지돼요.\n이동·복구 전에는 시스템에서 한 번 더 확인해요.",13,false);
        note.setTextColor(CleanupUiStyle.MUTED);note.setPadding(dp(4),dp(16),dp(4),0);content.addView(note);
        TextView leave=text("화면을 닫아도 저장된 작업 상태는 유지돼요",12,false);
        leave.setTextColor(CleanupUiStyle.MUTED);leave.setPadding(0,dp(8),0,dp(12));root.addView(leave);
        primary = CleanupUiStyle.button(context,"",true);
        secondary = CleanupUiStyle.button(context,"",false);root.addView(primary,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams secondaryParams=new LinearLayout.LayoutParams(-1,-2);secondaryParams.topMargin=dp(8);
        root.addView(secondary,secondaryParams);
        setContentView(root);
    }
    void render(String heading, String message, int done, int total,
                String action, Runnable next, String alternate, Runnable other) {
        title.setText(heading); detail.setText(message);
        count.setText(total > 0 ? done + " / " + total + "장" : "상태 확인 중");
        bar.setIndeterminate(total <= 0); bar.setMax(Math.max(1,total)); bar.setProgress(done);
        bind(primary,action,next); bind(secondary,alternate,other);
    }
    void setMessage(String message) {
        this.message = message;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+) / (\\d+)").matcher(message);
        int done=0,total=0;
        if (m.find()) { done=Integer.parseInt(m.group(1));total=Integer.parseInt(m.group(2)); }
        String heading="진행 상태를 확인하고 있어요";
        String description=message;
        m.reset();
        if(m.find()) {
            String prefix=message.substring(0,m.start()).trim();
            if(!prefix.isEmpty())heading=prefix;
            else heading="복사본 보존 확인";
            description=message.substring(m.end()).trim();
            if(description.startsWith("장 이동 확인 완료"))description=description.substring("장 이동 확인 완료".length()).trim();
            else if(description.startsWith("건"))description=description.substring(1).trim();
            if(description.startsWith("·"))description=description.substring(1).trim();
            if(description.isEmpty())description="사진과 기록 연결을 확인하고 있어요";
        }
        render(heading,description,done,total,null,null,null,null);
        if(total>0 && !message.contains("장 이동 확인 완료"))count.setText(done+" / "+total+"건 확인");
    }
    String message() { return message; }
    /** Migration adapter for continuation/recovery actions, keeping their handlers unchanged. */
    static final class Builder {
        private final Context context;
        private String title="원본 정리",message="",positive,negative,neutral;
        private android.content.DialogInterface.OnClickListener onPositive,onNegative,onNeutral;
        Builder(Context context) { this.context=context; }
        Builder setTitle(String value) { title=value;return this; }
        Builder setMessage(String value) { message=value;return this; }
        Builder setPositiveButton(String text,android.content.DialogInterface.OnClickListener action) { positive=text;onPositive=action;return this; }
        Builder setNegativeButton(String text,android.content.DialogInterface.OnClickListener action) { negative=text;onNegative=action;return this; }
        Builder setNeutralButton(String text,android.content.DialogInterface.OnClickListener action) { neutral=text;onNeutral=action;return this; }
        CleanupExecutionScreen show() {
            CleanupExecutionScreen screen=new CleanupExecutionScreen(context,()->{});
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+) / (\\d+)").matcher(message);
            int done=0,total=0;if(m.find()){done=Integer.parseInt(m.group(1));total=Integer.parseInt(m.group(2));}
            screen.render(title,message,done,total,positive,
                    onPositive==null?null:()->onPositive.onClick(screen,-1),
                    onNeutral!=null?neutral:negative,
                    onNeutral!=null?()->onNeutral.onClick(screen,-3):()->{if(onNegative!=null)onNegative.onClick(screen,-2);});
            screen.show();return screen;
        }
    }
    private void bind(Button button, String label, Runnable action) {
        button.setVisibility(action == null ? View.GONE : View.VISIBLE);
        button.setText(label); button.setOnClickListener(v -> { dismiss(); action.run(); });
    }
    @Override public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            getWindow().setLayout(getContext().getResources().getDisplayMetrics().widthPixels-dp(32),ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    private TextView text(String value,int size,boolean bold) {
        TextView t = new TextView(getContext()); t.setText(value); t.setTextSize(size);t.setTextColor(0xff242b3e);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD); return t;
    }
    private GradientDrawable background(int color) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color);d.setCornerRadius(dp(16));return d;
    }
    private int dp(int value) { return Math.round(value*getContext().getResources().getDisplayMetrics().density); }
}

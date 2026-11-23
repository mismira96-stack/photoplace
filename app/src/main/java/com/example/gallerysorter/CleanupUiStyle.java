package com.example.gallerysorter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

/** Matches the existing album cards and WorkingProgressBanner. Presentation only. */
final class CleanupUiStyle {
    static final int BACKGROUND=0xfff7f8fc, INK=0xff111827, MUTED=0xff646f84,
            PURPLE=0xff6852be, PROGRESS=PURPLE, BORDER=0xffe7eaf2;
    static int dp(Context c,int value) { return Math.round(value*c.getResources().getDisplayMetrics().density); }
    static GradientDrawable card(Context c,int color) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,18));
        d.setStroke(dp(c,1),BORDER);return d;
    }
    static TextView text(Context c,String value,int size,boolean bold,int color) {
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);return t;
    }
    static Button button(Context c,String label,boolean primary) {
        Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT_BOLD);b.setTextColor(primary?Color.WHITE:PURPLE);
        b.setStateListAnimator(null);b.setElevation(0);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));
        b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x226852be),card(c,primary?PURPLE:Color.WHITE),null));
        return b;
    }
    static LinearLayout header(Context c,String title,Runnable close) {
        LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(c,title,20,true,INK),new LinearLayout.LayoutParams(0,-2,1));
        Button button=button(c,"닫기",false);button.setOnClickListener(v->close.run());
        row.addView(button,new LinearLayout.LayoutParams(-2,-2));return row;
    }
    static ScrollView scroll(Context c) {
        return new ScrollView(c) {
            @Override protected void onMeasure(int width,int height) {
                int cap=(int)(getResources().getDisplayMetrics().heightPixels*0.45f);
                int mode=MeasureSpec.getMode(height),size=MeasureSpec.getSize(height);
                super.onMeasure(width,MeasureSpec.makeMeasureSpec(mode==MeasureSpec.UNSPECIFIED?cap:Math.min(cap,size),MeasureSpec.AT_MOST));
            }
        };
    }
}

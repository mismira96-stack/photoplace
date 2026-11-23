package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;

/** Square, recycled thumbnail cell; file metadata is available through the long-press action. */
final class MemorySelectionTile extends FrameLayout {
    final ImageView image;
    final TextView check;
    final ImageView video;
    MemorySelectionTile(Context context) {
        super(context);setClipToOutline(true);
        image=new ImageView(context);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(0xffe8e9f0);addView(image,new LayoutParams(-1,-1));
        check=new TextView(context);check.setGravity(Gravity.CENTER);check.setTextSize(15);check.setTypeface(Typeface.DEFAULT_BOLD);
        LayoutParams cp=new LayoutParams(dp(24),dp(24),Gravity.TOP|Gravity.RIGHT);cp.setMargins(dp(7),dp(7),dp(7),dp(7));addView(check,cp);
        video=new ImageView(context);video.setScaleType(ImageView.ScaleType.CENTER);
        video.setImageDrawable(MediaVectorIconDrawable.play(Color.WHITE,dp(11)));
        GradientDrawable vbg=new GradientDrawable();vbg.setShape(GradientDrawable.OVAL);
        vbg.setColor(Color.argb(175,18,22,32));vbg.setStroke(dp(1),Color.argb(65,255,255,255));
        video.setBackground(vbg);
        LayoutParams vp=new LayoutParams(dp(24),dp(24),Gravity.BOTTOM|Gravity.LEFT);vp.setMargins(dp(6),dp(6),dp(6),dp(6));addView(video,vp);
        setFocusable(false);image.setFocusable(false);check.setFocusable(false);check.setClickable(false);
    }
    void bindState(boolean selected,boolean isVideo,String description) {
        GradientDrawable shape=new GradientDrawable();shape.setColor(selected?CleanupUiStyle.PURPLE:Color.WHITE);
        shape.setCornerRadius(dp(14));shape.setStroke(selected?dp(2):0,CleanupUiStyle.PURPLE);setBackground(shape);
        int inset=selected?dp(2):0;setPadding(inset,inset,inset,inset);
        GradientDrawable badge=new GradientDrawable();badge.setShape(GradientDrawable.OVAL);
        badge.setColor(selected?CleanupUiStyle.PURPLE:0x22000000);badge.setStroke(dp(1),Color.WHITE);
        check.setBackground(badge);check.setText(selected?"✓":"");check.setTextColor(Color.WHITE);
        video.setVisibility(isVideo?VISIBLE:GONE);setContentDescription(description+(selected?", 선택됨":", 선택 안 됨"));setSelected(selected);
    }
    private int dp(int n){return CleanupUiStyle.dp(getContext(),n);}
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int width=MeasureSpec.getSize(widthSpec);super.onMeasure(widthSpec,MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY));
    }
}

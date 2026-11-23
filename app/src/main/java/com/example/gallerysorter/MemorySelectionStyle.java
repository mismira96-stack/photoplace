package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;

/** PhotoPlace's light surfaces and purple accents for photo selection and place picking. */
final class MemorySelectionStyle {
    static GradientDrawable surface(Context c,int color,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(CleanupUiStyle.dp(c,radius));return d;
    }
    static GradientDrawable background() {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xfffffafc,0xfff5f2fc,0xfffaf8ff});
    }
    static Button action(Context c,String text,boolean primary) {
        Button button=CleanupUiStyle.button(c,text,primary);
        if(primary){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff9670df,CleanupUiStyle.PURPLE});
            d.setCornerRadius(CleanupUiStyle.dp(c,22));button.setBackground(d);}
        else button.setBackground(surface(c,0xfff3eff8,22));
        return button;
    }
    static TextView icon(Context c,String text,String description) {
        TextView icon=CleanupUiStyle.text(c,text,24,false,CleanupUiStyle.MUTED);icon.setGravity(Gravity.CENTER);icon.setContentDescription(description);
        return icon;
    }
}

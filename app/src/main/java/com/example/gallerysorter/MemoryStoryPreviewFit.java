package com.example.gallerysorter;

import android.view.*;
import android.widget.*;

/** Reclaims editor space from the preview, without changing the exported card or hiding controls. */
final class MemoryStoryPreviewFit {
    static void bind(ScrollView scroll,LinearLayout body,FrameLayout stage,View preview){
        Runnable fit=()->{
            if(scroll.getHeight()<=0)return;
            int other=body.getPaddingTop()+body.getPaddingBottom();
            for(int i=0;i<body.getChildCount();i++){View child=body.getChildAt(i);if(child==stage||child.getVisibility()==View.GONE)continue;other+=child.getMeasuredHeight();if(child.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams){android.view.ViewGroup.MarginLayoutParams lp=(android.view.ViewGroup.MarginLayoutParams)child.getLayoutParams();other+=lp.topMargin+lp.bottomMargin;}}
            if(stage.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams){android.view.ViewGroup.MarginLayoutParams margins=(android.view.ViewGroup.MarginLayoutParams)stage.getLayoutParams();other+=margins.topMargin+margins.bottomMargin;}
            int available=scroll.getHeight()-other-stage.getPaddingTop()-stage.getPaddingBottom();
            float density=body.getResources().getDisplayMetrics().density;
            MemoryStoryConfig config=preview instanceof MemoryStoryPhotoPreview?((MemoryStoryPhotoPreview)preview).snapshot():null;
            boolean strip=config!=null&&config.layoutMode==MemoryStoryConfig.LayoutMode.PHOTO_STRIP;
            int height=Math.max(Math.round((strip?240:180)*density),Math.min(Math.round(309*density),available));
            FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)preview.getLayoutParams();
            if(Math.abs(lp.height-height)>2){lp.height=height;lp.width=Math.round(height*9f/16f);preview.setLayoutParams(lp);}
        };
        View.OnLayoutChangeListener change=(v,l,t,r,b,ol,ot,or,ob)->scroll.post(fit);
        scroll.addOnLayoutChangeListener(change);body.addOnLayoutChangeListener(change);scroll.post(fit);
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(()->scroll.post(fit));
    }
}

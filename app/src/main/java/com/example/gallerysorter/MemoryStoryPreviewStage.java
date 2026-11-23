package com.example.gallerysorter;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import java.util.function.Supplier;

/** Positions the caption sibling after preview layout, using the same final card transform. */
final class MemoryStoryPreviewStage extends FrameLayout {
    private MemoryStoryPhotoPreview preview;
    private View caption;
    private EditText input;
    private Supplier<MemoryStoryConfig> config;
    MemoryStoryPreviewStage(Context context){super(context);}
    void bindCaption(MemoryStoryPhotoPreview preview,View caption,EditText input,Supplier<MemoryStoryConfig> config) {
        this.preview=preview;this.caption=caption;this.input=input;this.config=config;
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b) {
        super.onLayout(changed,l,t,r,b);
        if(preview==null||caption.getVisibility()!=View.VISIBLE||preview.snapshot()==null)return;
        MemoryStoryComposer.Box box=preview.captionBox();
        int left=Math.round(preview.getLeft()+preview.viewX(box.left));
        int top=Math.round(preview.getTop()+preview.viewY(box.top));
        int width=Math.max(1,Math.round(preview.viewX(box.right)-preview.viewX(box.left)));
        int height=Math.max(1,Math.round(preview.viewY(box.bottom)-preview.viewY(box.top)));
        MemoryStoryConfig value=config.get();
        input.setPadding(0,0,CleanupUiStyle.dp(getContext(),3),0);
        input.setTypeface(MemoryStoryComposer.resolveTypeface(getContext(),value.fontKind));
        input.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,16,getResources().getDisplayMetrics()),preview.captionTextSize()*preview.displayScale()));
        // Do not wait for a sibling's old LayoutParams to catch up after an IME resize.
        caption.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));
        caption.layout(left,top,left+width,top+height);
    }
}

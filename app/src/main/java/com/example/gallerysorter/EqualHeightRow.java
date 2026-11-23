package com.example.gallerysorter;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/**
 * A horizontal LinearLayout that ensures all child views in a row have identical height,
 * equal to the maximum measured child height.
 */
class EqualHeightRow extends LinearLayout {

    EqualHeightRow(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int maxHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
            }
        }
        if (maxHeight > 0) {
            boolean remeasured = false;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != GONE && child.getMeasuredHeight() != maxHeight) {
                    child.measure(
                            MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY)
                    );
                    remeasured = true;
                }
            }
            if (remeasured) {
                setMeasuredDimension(getMeasuredWidth(), resolveSize(maxHeight, heightMeasureSpec));
            }
        }
    }
}

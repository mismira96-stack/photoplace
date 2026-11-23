package com.example.gallerysorter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.ImageView;

final class MemoryIconButton {
    static ImageView create(Context context, String label, Drawable icon) {
        ImageView button = new ImageView(context);
        int target = CleanupUiStyle.dp(context, 48);
        button.setMinimumWidth(target);
        button.setMinimumHeight(target);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setImageDrawable(icon);
        button.setBackground(ripple(context, null));
        button.setContentDescription(label);
        button.setTooltipText(label);
        button.setFocusable(true);
        return button;
    }

    static ImageView cardMenu(Context context, Runnable open) {
        ImageView button = create(context, "앨범 메모",
                MediaVectorIconDrawable.more(CleanupUiStyle.MUTED, CleanupUiStyle.dp(context, 22)));
        button.setOnClickListener(view -> open.run());
        return button;
    }

    static ImageView search(Context context, boolean open, boolean chip) {
        ImageView button = create(context, "", null);
        bindSearch(button, open, "검색 열기", "검색 닫기");
        if (chip) {
            Drawable fill = MemorySelectionStyle.surface(context, 0xFFF1EEFF, 12);
            button.setBackground(ripple(context, new InsetDrawable(fill, CleanupUiStyle.dp(context, 5))));
        }
        return button;
    }

    static void bindSearch(ImageView button, boolean open, String openLabel, String closeLabel) {
        int size = CleanupUiStyle.dp(button.getContext(), 22);
        int color = CleanupUiStyle.PURPLE;
        button.setImageDrawable(open ? MediaVectorIconDrawable.close(color, size)
                : MediaVectorIconDrawable.search(color, size));
        String label = open ? closeLabel : openLabel;
        button.setContentDescription(label);
        button.setTooltipText(label);
    }

    private static RippleDrawable ripple(Context context, Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x226852BE), content,
                MemorySelectionStyle.surface(context, Color.WHITE, 12));
    }
}

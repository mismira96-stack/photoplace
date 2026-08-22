package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

final class MemoryPhotoThumbnailRenderer {
    interface Listener {
        void onOpen(MemoryPhotoItem item);
    }

    interface ThumbnailLoader {
        void load(ImageView target, String uriValue, int sizePx);
    }

    private final Context context;
    private final Listener listener;
    private final ThumbnailLoader thumbnailLoader;

    MemoryPhotoThumbnailRenderer(Context context,
                                 Listener listener,
                                 ThumbnailLoader thumbnailLoader) {
        this.context = context;
        this.listener = listener;
        this.thumbnailLoader = thumbnailLoader;
    }

    View render(final MemoryPhotoItem item, int thumbnailSizePx) {
        FrameLayout frame = new FrameLayout(context);
        frame.setClickable(true);
        frame.setFocusable(true);

        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if (thumbnailLoader != null) {
            thumbnailLoader.load(image, item == null ? "" : item.sourceUri, thumbnailSizePx);
        }

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(248, 249, 252));
        background.setCornerRadius(dp(16));
        frame.setBackground(background);
        frame.setClipToOutline(true);

        if (item != null && item.mediaKind == MediaKind.VIDEO) {
            TextView play = new TextView(context);
            play.setText("▶");
            play.setTextSize(12.0f);
            play.setTypeface(Typeface.DEFAULT_BOLD);
            play.setTextColor(Color.WHITE);
            play.setGravity(Gravity.CENTER);
            GradientDrawable badge = new GradientDrawable();
            badge.setColor(Color.argb(185, 24, 31, 43));
            badge.setShape(GradientDrawable.OVAL);
            play.setBackground(badge);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.END | Gravity.BOTTOM);
            badgeParams.setMargins(0, 0, dp(7), dp(7));
            frame.addView(play, badgeParams);
        }

        frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null && item != null) {
                    listener.onOpen(item);
                }
            }
        });
        return frame;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

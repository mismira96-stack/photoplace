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
    private final String originKey;
    private final MemoryDetailSelection selection;

    MemoryPhotoThumbnailRenderer(Context context,
                                 Listener listener,
                                 ThumbnailLoader thumbnailLoader) {
        this(context,listener,thumbnailLoader,"",null);
    }
    MemoryPhotoThumbnailRenderer(Context context,Listener listener,ThumbnailLoader thumbnailLoader,String originKey, MemoryDetailSelection selection) {
        this.context = context;
        this.listener = listener;
        this.thumbnailLoader = thumbnailLoader;
        this.originKey=originKey;
        this.selection=selection;
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
            ImageView play = new ImageView(context);
            play.setScaleType(ImageView.ScaleType.CENTER);
            play.setImageDrawable(MediaVectorIconDrawable.play(Color.WHITE, dp(13)));
            GradientDrawable badge = new GradientDrawable();
            badge.setColor(Color.argb(175, 18, 22, 32));
            badge.setStroke(dp(1), Color.argb(65, 255, 255, 255));
            badge.setShape(GradientDrawable.OVAL);
            play.setBackground(badge);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.END | Gravity.BOTTOM);
            badgeParams.setMargins(0, 0, dp(7), dp(7));
            frame.addView(play, badgeParams);
        }

        Runnable open = () -> { if (listener != null && item != null) listener.onOpen(item); };
        if (selection != null && item != null) {
            selection.bind(frame, item, open);
        } else {
            frame.setOnClickListener(view -> open.run());
            frame.setOnLongClickListener(view -> {
                MemoryManagementEntry.openSingle(context, item, originKey);
                return true;
            });
        }
        return frame;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

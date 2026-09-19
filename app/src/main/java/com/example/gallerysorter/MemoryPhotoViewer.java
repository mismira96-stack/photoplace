package com.example.gallerysorter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Small in-app viewer for one place/date section. Gallery remains an explicit fallback action. */
final class MemoryPhotoViewer {
    interface Host {
        android.content.Context context();
        int dp(int value);
        void loadThumbnail(ImageView target, Uri uri, int sizePx);
        void openExternal(MemoryPhotoItem item);
    }

    interface Listener {
        void onClose();
    }

    private MemoryPhotoViewer() {
    }

    static View create(final String title,
                       final MemoryPhotoSection section,
                       int initialIndex,
                       final Host host,
                       final Listener listener) {
        final List<MemoryPhotoItem> photos = section == null ? null : section.photos;
        final int count = photos == null ? 0 : photos.size();
        final int firstIndex = clamp(initialIndex, count);

        final FrameLayout root = new FrameLayout(host.context());
        root.setBackgroundColor(Color.rgb(18, 20, 28));

        final LinearLayout column = new LinearLayout(root.getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(host.dp(16), host.dp(18), host.dp(16), host.dp(18));
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout toolbar = new LinearLayout(root.getContext());
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        column.addView(toolbar, new LinearLayout.LayoutParams(-1, host.dp(48)));

        TextView close = new TextView(root.getContext());
        close.setText("‹");
        close.setTextSize(38.0f);
        close.setTextColor(Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Memory 상세로 돌아가기");
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClose();
            }
        });
        toolbar.addView(close, new LinearLayout.LayoutParams(host.dp(44), host.dp(44)));

        TextView heading = new TextView(root.getContext());
        heading.setText((section == null ? "사진" : section.dateText)
                + (section == null || section.placeText.isEmpty() ? "" : " · " + section.placeText));
        heading.setTextSize(17.0f);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextColor(Color.WHITE);
        heading.setSingleLine(true);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0, -1, 1.0f));

        final TextView counter = new TextView(root.getContext());
        counter.setTextColor(Color.LTGRAY);
        counter.setTextSize(14.0f);
        counter.setGravity(Gravity.CENTER);
        toolbar.addView(counter, new LinearLayout.LayoutParams(host.dp(56), -1));

        final FrameLayout photoViewport = new FrameLayout(root.getContext());
        final ImageView image = new ImageView(root.getContext());
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);
        image.setContentDescription("Memory 사진");
        photoViewport.addView(image, new FrameLayout.LayoutParams(-1, -1));
        photoViewport.setClickable(true);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        imageParams.setMargins(0, host.dp(8), 0, host.dp(8));
        column.addView(photoViewport, imageParams);

        final LinearLayout navigation = new LinearLayout(root.getContext());
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(0, 0, 0, host.dp(4));
        column.addView(navigation, new LinearLayout.LayoutParams(-1, host.dp(48)));

        final TextView previous = new TextView(root.getContext());
        previous.setText("‹ 이전");
        previous.setTextSize(13.0f);
        previous.setTextColor(Color.WHITE);
        previous.setGravity(Gravity.CENTER);
        navigation.addView(previous, new LinearLayout.LayoutParams(0, host.dp(42), 1.0f));

        final TextView next = new TextView(root.getContext());
        next.setText("다음 ›");
        next.setTextSize(13.0f);
        next.setTextColor(Color.WHITE);
        next.setGravity(Gravity.CENTER);
        navigation.addView(next, new LinearLayout.LayoutParams(0, host.dp(42), 1.0f));

        final TextView videoMessage = new TextView(root.getContext());
        videoMessage.setText("동영상은 Gallery에서 재생할 수 있어요.");
        videoMessage.setTextColor(Color.LTGRAY);
        videoMessage.setTextSize(14.0f);
        videoMessage.setGravity(Gravity.CENTER);
        videoMessage.setVisibility(View.GONE);
        column.addView(videoMessage, new LinearLayout.LayoutParams(-1, host.dp(36)));

        final int[] indexHolder = {firstIndex};

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (indexHolder[0] > 0) {
                    indexHolder[0]--;
                    bind(photos, indexHolder[0], image, counter, videoMessage, previous, next, host);
                }
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (indexHolder[0] + 1 < count) {
                    indexHolder[0]++;
                    bind(photos, indexHolder[0], image, counter, videoMessage, previous, next, host);
                }
            }
        });

        final float[] downX = {0.0f};
        final float[] downY = {0.0f};
        photoViewport.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downX[0] = event.getX();
                    downY[0] = event.getY();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float dx = event.getX() - downX[0];
                    float dy = event.getY() - downY[0];
                    if (Math.abs(dx) > host.dp(40) && Math.abs(dx) > Math.abs(dy)) {
                        int nextIndex = indexHolder[0] + (dx < 0 ? 1 : -1);
                        if (nextIndex >= 0 && nextIndex < count) {
                            indexHolder[0] = nextIndex;
                            bind(photos, indexHolder[0], image, counter, videoMessage, previous, next, host);
                        }
                    }
                    return true;
                }
                return true;
            }
        });

        final Button gallery = new Button(root.getContext());
        gallery.setText("Gallery에서 열기");
        gallery.setTextColor(Color.WHITE);
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MemoryPhotoItem item = current(photos, indexHolder[0]);
                if (item != null) {
                    host.openExternal(item);
                }
            }
        });
        LinearLayout.LayoutParams galleryParams = new LinearLayout.LayoutParams(-1, host.dp(46));
        galleryParams.setMargins(0, host.dp(4), 0, 0);
        column.addView(gallery, galleryParams);

        bind(photos, firstIndex, image, counter, videoMessage, previous, next, host);
        return root;
    }

    private static void bind(List<MemoryPhotoItem> photos,
                             int index,
                             ImageView image,
                             TextView counter,
                             TextView videoMessage,
                             TextView previous,
                             TextView next,
                             Host host) {
        MemoryPhotoItem item = current(photos, index);
        int count = photos == null ? 0 : photos.size();
        counter.setText(count == 0 ? "0/0" : (index + 1) + "/" + count);
        previous.setEnabled(index > 0);
        next.setEnabled(index + 1 < count);
        previous.setAlpha(index > 0 ? 1.0f : 0.4f);
        next.setAlpha(index + 1 < count ? 1.0f : 0.4f);
        if (item == null || item.sourceUri.isEmpty()) {
            image.setImageDrawable(null);
            videoMessage.setVisibility(View.GONE);
            return;
        }
        boolean video = item.mediaKind == MediaKind.VIDEO;
        videoMessage.setVisibility(video ? View.VISIBLE : View.GONE);
        if (video) {
            image.setImageDrawable(null);
        } else {
            host.loadThumbnail(image, Uri.parse(item.sourceUri), host.dp(900));
        }
    }

    private static MemoryPhotoItem current(List<MemoryPhotoItem> photos, int index) {
        return photos == null || index < 0 || index >= photos.size() ? null : photos.get(index);
    }

    private static int clamp(int index, int count) {
        if (count <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(index, count - 1));
    }
}

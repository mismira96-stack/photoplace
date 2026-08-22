package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class LocationAlbumEmptyStateRenderer {
    interface Listener {
        void onOpenDiscovery();
    }

    interface ButtonStyler {
        void style(Button button);
    }

    private final Context context;
    private final ButtonStyler buttonStyler;
    private final Listener listener;

    LocationAlbumEmptyStateRenderer(Context context, ButtonStyler buttonStyler, Listener listener) {
        this.context = context;
        this.buttonStyler = buttonStyler;
        this.listener = listener;
    }

    View render(boolean canOpenDiscovery) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(28), dp(24), dp(24));
        card.setBackground(cardBackground());

        ImageView illustration = new ImageView(context);
        illustration.setImageResource(R.drawable.location_album_empty);
        illustration.setScaleType(ImageView.ScaleType.FIT_CENTER);
        illustration.setContentDescription(null);
        illustration.setAlpha(0.92f);
        card.addView(illustration, new LinearLayout.LayoutParams(-1, dp(150)));

        TextView title = text("아직 만든 위치 앨범이 없어요", 19.0f, Color.rgb(31, 35, 48), true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(20), 0, 0);
        card.addView(title, titleParams);

        TextView body = text("발견한 장소를 앨범으로 만들면\n여기에 차곡차곡 모여요.", 14.0f, Color.rgb(91, 101, 120), false);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(0.0f, 1.25f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.setMargins(0, dp(9), 0, canOpenDiscovery ? dp(22) : 0);
        card.addView(body, bodyParams);

        if (canOpenDiscovery) {
            Button action = new Button(context);
            action.setText("발견한 장소 보기");
            action.setContentDescription("발견한 장소 보기");
            action.setAllCaps(false);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onOpenDiscovery();
                    }
                }
            });
            if (buttonStyler != null) {
                buttonStyler.style(action);
            }
            card.addView(action, new LinearLayout.LayoutParams(-1, dp(52)));
        }
        return card;
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(color);
        text.setIncludeFontPadding(false);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return text;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(24));
        background.setStroke(dp(1), Color.rgb(229, 233, 240));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

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

final class DiscoveryEmptyStateRenderer {
    interface Listener {
        void onFindPlaces();
    }

    interface ButtonStyler {
        void style(Button button);
    }

    private final Context context;
    private final ButtonStyler buttonStyler;
    private final Listener listener;

    DiscoveryEmptyStateRenderer(Context context,
                                ButtonStyler buttonStyler,
                                Listener listener) {
        this.context = context;
        this.buttonStyler = buttonStyler;
        this.listener = listener;
    }

    View render() {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(28), dp(24), dp(24));
        card.setBackground(cardBackground());

        ImageView illustration = new ImageView(context);
        illustration.setImageResource(R.drawable.discovery_empty_place);
        illustration.setScaleType(ImageView.ScaleType.FIT_CENTER);
        illustration.setContentDescription(null);
        illustration.setAlpha(0.92f);
        card.addView(illustration, new LinearLayout.LayoutParams(-1, dp(150)));

        TextView title = text("아직 발견한 장소가 없어요", 19.0f, Color.rgb(31, 35, 48), true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(20), 0, 0);
        card.addView(title, titleParams);

        TextView body = text("사진 속 위치 정보를 확인하면\n장소별로 모아볼 수 있어요.", 14.0f, Color.rgb(91, 101, 120), false);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(0.0f, 1.25f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.setMargins(0, dp(9), 0, dp(22));
        card.addView(body, bodyParams);

        Button action = new Button(context);
        action.setText("사진 속 장소 찾기");
        action.setContentDescription("사진 속 장소 찾기");
        action.setAllCaps(false);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onFindPlaces();
                }
            }
        });
        if (buttonStyler != null) {
            buttonStyler.style(action);
        }
        card.addView(action, new LinearLayout.LayoutParams(-1, dp(72)));
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

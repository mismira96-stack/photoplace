package com.example.gallerysorter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Confirmation UI for the one global discovery-to-album action. */
final class DiscoveryOrganizeConfirmDialog {
    interface Listener {
        void onConfirmed();
    }

    private final Context context;

    DiscoveryOrganizeConfirmDialog(Context context) {
        this.context = context;
    }

    void show(List<MemoryRecord> records, boolean moveVideos, final Listener listener) {
        int placeCount = 0;
        int itemCount = 0;
        if (records != null) {
            for (MemoryRecord record : records) {
                OrganizePlaceService.Plan plan = OrganizePlaceService.planFor(record);
                if (plan.canOrganize()) {
                    placeCount++;
                    itemCount += plan.refs.size();
                }
            }
        }
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(18));
        root.setBackground(rounded(Color.WHITE, 24, 0, Color.TRANSPARENT));

        TextView title = text("위치 앨범을 만들까요?", 21, Color.rgb(17, 24, 39), true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, widthWithBottom(8));

        TextView summary = text(
                "발견한 장소 " + placeCount + "곳 · 사진과 동영상 " + itemCount + "개",
                15,
                Color.rgb(75, 85, 99),
                false);
        summary.setGravity(Gravity.CENTER);
        root.addView(summary, widthWithBottom(16));

        TextView message = text(
                moveVideos
                        ? "사진은 위치별 앨범으로 복사되어 원본이 유지됩니다.\n동영상은 위치 앨범으로 이동됩니다.\n이미 만들어진 항목은 자동으로 건너뜁니다."
                        : "사진은 위치별 앨범으로 복사되어 원본이 유지됩니다.\n이미 만들어진 항목은 자동으로 건너뜁니다.",
                14,
                Color.rgb(75, 85, 99),
                false);
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(dp(2), 1.0f);
        message.setPadding(dp(14), dp(12), dp(14), dp(12));
        message.setBackground(rounded(Color.rgb(247, 247, 252), 16, 1, Color.rgb(230, 228, 241)));
        root.addView(message, widthWithBottom(16));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(actions, width());

        Button cancel = button("취소", false);
        cancel.setOnClickListener(view -> dialog.dismiss());
        actions.addView(cancel, weightedButton(true));

        Button confirm = button("위치 앨범 만들기", true);
        confirm.setOnClickListener(view -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onConfirmed();
            }
        });
        actions.addView(confirm, weightedButton(false));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            int maxWidth = Math.min(dp(520), context.getResources().getDisplayMetrics().widthPixels - dp(36));
            shownWindow.setLayout(maxWidth, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(primary ? Color.WHITE : Color.rgb(75, 85, 99));
        button.setAllCaps(false);
        button.setBackground(rounded(
                primary ? Color.rgb(111, 84, 235) : Color.rgb(245, 245, 249),
                14,
                primary ? 0 : 1,
                Color.rgb(225, 223, 234)));
        return button;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams width() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams widthWithBottom(int bottomDp) {
        LinearLayout.LayoutParams params = width();
        params.bottomMargin = dp(bottomDp);
        return params;
    }

    private LinearLayout.LayoutParams weightedButton(boolean addEndMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
        if (addEndMargin) {
            params.rightMargin = dp(8);
        }
        return params;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

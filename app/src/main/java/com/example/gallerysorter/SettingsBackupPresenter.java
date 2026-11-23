package com.example.gallerysorter;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Presenter for Data Backup & Restore card in Settings.
 * Complies with UI_GUIDE.md and P0 isolation rules.
 */
final class SettingsBackupPresenter {

    // Disabled for 1.3.46 release; will be re-enabled after physical device verification in 1.3.47
    private static final boolean ENABLED = false;

    static void attach(LinearLayout parent, Activity activity) {
        if (!ENABLED) {
            return;
        }
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 16);
        card.setPadding(pad, pad, pad, pad);

        // Background: 16dp rounded card, white, subtle border
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xffffffff);
        bg.setCornerRadius(dp(activity, 16));
        bg.setStroke(dp(activity, 1), 0xffe7eaf2);
        card.setBackground(bg);

        // Top Row: Icon + Texts
        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Icon Box
        TextView icon = new TextView(activity);
        icon.setText("💾");
        icon.setTextSize(22f);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(0xfff4f1fc); // light lavender
        iconBg.setCornerRadius(dp(activity, 14));
        icon.setBackground(iconBg);
        int iconSize = dp(activity, 46);
        topRow.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        // Texts
        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(activity, 14), 0, dp(activity, 4), 0);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
        topRow.addView(textCol, textLp);

        TextView title = new TextView(activity);
        title.setText("기록 백업 및 복원");
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xff111827);
        textCol.addView(title);

        TextView desc = new TextView(activity);
        desc.setText("작성한 날짜별 메모와 개인 장소를 파일로 안전하게 보관하거나 복원합니다.");
        desc.setTextSize(13f);
        desc.setTextColor(0xff646f84);
        desc.setLineSpacing(dp(activity, 2), 1.0f);
        desc.setPadding(0, dp(activity, 3), 0, 0);
        textCol.addView(desc);

        card.addView(topRow, new LinearLayout.LayoutParams(-1, -2));

        // Bottom Row: 2 Action Buttons
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(-1, -2);
        btnRowLp.topMargin = dp(activity, 14);
        card.addView(btnRow, btnRowLp);

        // Button 1: 내 기록 백업
        Button backupBtn = createStyledButton(activity, "내 기록 백업", true);
        backupBtn.setOnClickListener(v -> SettingsBackupController.performBackup(activity));
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, dp(activity, 42), 1f);
        b1Lp.rightMargin = dp(activity, 8);
        btnRow.addView(backupBtn, b1Lp);

        // Button 2: 백업에서 복원
        Button restoreBtn = createStyledButton(activity, "백업에서 복원", false);
        restoreBtn.setOnClickListener(v -> handleRestoreClick(activity));
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, dp(activity, 42), 1f);
        btnRow.addView(restoreBtn, b2Lp);

        // Add card to parent
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = dp(activity, 18);
        parent.addView(card, cardLp);
    }

    private static void handleRestoreClick(Activity activity) {
        List<SettingsBackupController.BackupFileInfo> backups = SettingsBackupController.findAvailableBackups(activity);
        if (backups.isEmpty()) {
            // No backups found automatically -> launch file picker
            SettingsBackupActivity.launch(activity);
            return;
        }

        // Show custom dialog listing backups
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        int p = dp(activity, 20);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        root.setBackground(MemorySelectionStyle.surface(activity, 0xfffffbff, 24));

        TextView brand = CleanupUiStyle.text(activity, "PhotoPlace", 11, true, CleanupUiStyle.PURPLE);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand);

        TextView heading = CleanupUiStyle.text(activity, "백업 파일 복원", 20, true, CleanupUiStyle.INK);
        heading.setPadding(0, p / 2, 0, p / 4);
        root.addView(heading);

        TextView sub = CleanupUiStyle.text(activity, "기기에서 발견된 백업 파일입니다. 복원할 항목을 선택해 주세요.", 13, false, CleanupUiStyle.MUTED);
        sub.setPadding(0, 0, 0, p / 2);
        root.addView(sub);

        // List up to 4 recent backups
        int count = Math.min(backups.size(), 4);
        for (int i = 0; i < count; i++) {
            SettingsBackupController.BackupFileInfo item = backups.get(i);
            LinearLayout itemRow = new LinearLayout(activity);
            itemRow.setOrientation(LinearLayout.VERTICAL);
            itemRow.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setColor(0xfffaf7fd);
            itemBg.setCornerRadius(dp(activity, 12));
            itemBg.setStroke(dp(activity, 1), 0xffe7eaf2);
            itemRow.setBackground(itemBg);
            itemRow.setClickable(true);
            itemRow.setFocusable(true);

            TextView nameText = new TextView(activity);
            nameText.setText("💾 " + item.displayName);
            nameText.setTextSize(14f);
            nameText.setTypeface(Typeface.DEFAULT_BOLD);
            nameText.setTextColor(0xff111827);
            itemRow.addView(nameText);

            String dateStr = new SimpleDateFormat("yyyy.MM.dd a h:mm 저장본", Locale.KOREA).format(new Date(item.lastModified));
            TextView dateText = new TextView(activity);
            dateText.setText(dateStr);
            dateText.setTextSize(12f);
            dateText.setTextColor(0xff6852be);
            dateText.setPadding(0, dp(activity, 2), 0, 0);
            itemRow.addView(dateText);

            itemRow.setOnClickListener(v -> {
                dialog.dismiss();
                SettingsBackupController.RestoreResult res = SettingsBackupController.restoreFromUri(activity, item.uri);
                if (res.success) {
                    MemoryActionDialog.show(activity, "기록 복원 완료", res.message, "확인", () -> {}, null, null);
                } else {
                    MemoryActionDialog.show(activity, "복원 실패", res.message, "확인", () -> {}, null, null);
                }
            });

            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(-1, -2);
            itemLp.bottomMargin = dp(activity, 8);
            root.addView(itemRow, itemLp);
        }

        // Actions: [ 다른 파일 찾기 ] / [ 닫기 ]
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(activity, 8);
        root.addView(actions, ap);

        Button otherBtn = MemorySelectionStyle.action(activity, "다른 파일 찾기", false);
        otherBtn.setOnClickListener(v -> {
            dialog.dismiss();
            SettingsBackupActivity.launch(activity);
        });
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, -2, 1);
        op.rightMargin = dp(activity, 8);
        actions.addView(otherBtn, op);

        Button cancelBtn = MemorySelectionStyle.action(activity, "닫기", true);
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        actions.addView(cancelBtn, new LinearLayout.LayoutParams(0, -2, 1));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(Math.min(dp(activity, 420), activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 36)), -2);
        }
        dialog.show();
    }

    private static Button createStyledButton(Activity a, String text, boolean primary) {
        Button b = new Button(a);
        b.setText(text);
        b.setTextSize(13.5f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(a, 12));
        if (primary) {
            d.setColor(0xff6852be); // Primary Purple
            b.setTextColor(0xffffffff);
        } else {
            d.setColor(0xfff4f1fc); // Lavender
            d.setStroke(dp(a, 1), 0xffdcd6f7);
            b.setTextColor(0xff6852be);
        }
        b.setBackground(d);
        return b;
    }

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}

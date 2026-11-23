package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/** App-styled viewer place actions. Owns presentation only; callbacks retain all mutation policy. */
final class MemoryPlaceActionsPopup {
    static PopupWindow show(View anchor, Runnable change, Runnable unlink) {
        return show(anchor, 300, "다른 위치기록으로 변경", false, change, unlink);
    }

    static void showAlbumMenu(View anchor, Runnable action) {
        showAlbumMenu(anchor, "앨범 만들기", action);
    }

    static void showAlbumMenu(View anchor, String label, Runnable action) {
        show(anchor, 188, label, true, action, null);
    }

    private static PopupWindow show(View anchor, int preferredWidth, String label,
                                    boolean album, Runnable action, Runnable unlink) {
        Context c = anchor.getContext();
        int padding = CleanupUiStyle.dp(c, 8);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setBackground(MemorySelectionStyle.surface(c, 0xfffffbff, 18));
        int width = Math.min(CleanupUiStyle.dp(c, preferredWidth), c.getResources().getDisplayMetrics().widthPixels - padding * 4);
        PopupWindow popup = new PopupWindow(root, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(CleanupUiStyle.dp(c, 12));
        int iconSize = CleanupUiStyle.dp(c, 22);
        Drawable icon = album ? MediaVectorIconDrawable.folderPlus(CleanupUiStyle.PURPLE, iconSize)
                : MediaVectorIconDrawable.place(CleanupUiStyle.PURPLE, iconSize);
        addRow(root, label, icon, () -> { popup.dismiss(); action.run(); });
        if (unlink != null) addRow(root, "위치 연결 해제", null, () -> { popup.dismiss(); unlink.run(); });
        popup.showAsDropDown(anchor, 0, padding, Gravity.END);
        return popup;
    }
    private static void addRow(LinearLayout parent, String label, Drawable drawable, Runnable action) {
        boolean primary = drawable != null;
        Context c = parent.getContext();
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(CleanupUiStyle.dp(c, 12), 0, CleanupUiStyle.dp(c, 12), 0);
        row.setBackground(MemorySelectionStyle.surface(c, primary ? 0xfff3effb : 0xfffffbff, 12));
        if (primary) {
            ImageView icon = new ImageView(c);
            icon.setImageDrawable(drawable);
            row.addView(icon, new LinearLayout.LayoutParams(CleanupUiStyle.dp(c, 22), CleanupUiStyle.dp(c, 22)));
        }
        TextView text = CleanupUiStyle.text(c, label, 14, primary, primary ? CleanupUiStyle.PURPLE : CleanupUiStyle.INK);
        text.setPadding(CleanupUiStyle.dp(c, primary ? 12 : 0), 0, 0, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        row.setContentDescription(label);
        row.setOnClickListener(v -> action.run());
        parent.addView(row, new LinearLayout.LayoutParams(-1, CleanupUiStyle.dp(c, 52)));
    }
}

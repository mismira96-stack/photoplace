package com.example.gallerysorter;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.function.Consumer;

/** Small persistent trash action beside the location-album heading. */
final class CleanupReviewEntry {
    static void addAction(Activity activity, LinearLayout header, Consumer<String> notice) {
        ImageView action = MemoryIconButton.create(activity, "원본 정리",
                MediaVectorIconDrawable.trash(CleanupUiStyle.PURPLE, CleanupUiStyle.dp(activity, 20)));
        action.setContentDescription("카메라·다운로드에 남은 중복 원본 사진 정리");
        action.setOnClickListener(view -> {
            android.view.inputmethod.InputMethodManager keyboard = activity.getSystemService(android.view.inputmethod.InputMethodManager.class);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
            View focused = activity.getCurrentFocus();
            if (focused != null) focused.clearFocus();
            CleanupFeatureGate.onLegacyRequest(activity, notice);
        });
        int size = CleanupUiStyle.dp(activity, 44);
        header.addView(action, new LinearLayout.LayoutParams(size, size));
    }
}

package com.example.gallerysorter;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;

/** Lightweight discovery actions with a full-size touch target. */
final class MemoryAddActionIcon {
    static ImageView addPlace(Context context, Runnable action) {
        ImageView icon = MemoryIconButton.create(context, "장소 추가",
                MediaVectorIconDrawable.discoveryAdd(CleanupUiStyle.PURPLE, CleanupUiStyle.dp(context, 28), false));
        icon.setOnClickListener(view -> action.run());
        return icon;
    }

    static ImageView collectMemories(Context context, Runnable collectMemories) {
        ImageView collection = MemoryIconButton.create(context, "기억 모으기",
                MediaVectorIconDrawable.discoveryAdd(CleanupUiStyle.PURPLE, CleanupUiStyle.dp(context, 28), true));
        collection.setOnClickListener(view -> collectMemories.run());
        return collection;
    }

    static View discoveryActions(Context context, View addPlace, Runnable collectMemories) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(addPlace);
        row.addView(collectMemories(context, collectMemories));
        return row;
    }

    static ImageView albumMenu(Context context, Runnable action) {
        ImageView icon = MemoryIconButton.create(context, "발견 기록 옵션", MediaVectorIconDrawable.more(
                CleanupUiStyle.PURPLE, CleanupUiStyle.dp(context, 22)));
        icon.setOnClickListener(view -> MemoryPlaceActionsPopup.showAlbumMenu(view, action));
        return icon;
    }

}

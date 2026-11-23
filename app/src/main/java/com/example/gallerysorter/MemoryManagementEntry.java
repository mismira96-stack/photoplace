package com.example.gallerysorter;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Consumer;

/** View binding and routing only; no selection or mutation logic is placed in MainActivity. */
final class MemoryManagementEntry {
    static final int REQUEST=7413;

    interface ViewerReturnHandler {
        boolean onSingleActionReturn(String returnKey, Consumer<String> exitToDetail);
    }

    private static ViewerReturnHandler viewerReturnHandler;

    static void bindViewerReturnHandler(ViewerReturnHandler handler) {
        viewerReturnHandler = handler;
    }

    static void unbindViewerReturnHandler(ViewerReturnHandler handler) {
        if (viewerReturnHandler == handler) {
            viewerReturnHandler = null;
        }
    }

    static int unresolvedCount(Context c) {
        try {
            List<StoredAlbumSummary> albums=MemoryManagementAlbums.read(c);
            Set<String> paths=new HashSet<>();
            for(String p:MemoryManagementAlbums.paths(albums))if(p!=null&&!p.isEmpty())paths.add(ManualPlacePaths.normalize(p));
            return new ManualPlaceStore(c).countUnresolved(paths);
        } catch (Exception ignored) {
            return 0;
        }
    }

    static View addRecordAction(Activity activity, int unresolvedCount) {
        ImageView button = MemoryAddActionIcon.addPlace(activity,
                () -> open(activity, "", ""));
        if (unresolvedCount > 0) {
            button.setContentDescription("장소 추가, 위치 없는 사진 "
                    + NumberFormat.getIntegerInstance(Locale.KOREA).format(unresolvedCount) + "장");
        }
        return button;
    }

    static android.view.View detailOptions(Activity a,MemoryBrowserDetail detail, MemoryDetailSelection selection) {
        ImageView options=new ImageView(a);
        options.setScaleType(ImageView.ScaleType.CENTER);
        options.setImageDrawable(MediaVectorIconDrawable.trash(CleanupUiStyle.PURPLE,CleanupUiStyle.dp(a,20)));
        int inset=CleanupUiStyle.dp(a,8);options.setPadding(inset,inset,inset,inset);
        options.setContentDescription("사진 정리");
        android.util.TypedValue selectable=new android.util.TypedValue();
        a.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,selectable,true);
        options.setBackgroundResource(selectable.resourceId);
        options.setEnabled(!detail.sourceUris.isEmpty());options.setAlpha(options.isEnabled()?1f:0.4f);
        options.setOnClickListener(v -> selection.trash(() -> open(a, detail.item.memoryKey, "", detail.item.title)));
        return options;
    }
    static android.view.View detailStoryAction(Activity a,MemoryBrowserDetail detail, MemoryDetailSelection selection) {
        ImageView share=new ImageView(a);
        share.setScaleType(ImageView.ScaleType.CENTER);
        share.setImageDrawable(MediaVectorIconDrawable.share(CleanupUiStyle.PURPLE,CleanupUiStyle.dp(a,20)));
        int inset=CleanupUiStyle.dp(a,8);share.setPadding(inset,inset,inset,inset);
        share.setContentDescription("Instagram 스토리 공유");
        android.util.TypedValue selectable=new android.util.TypedValue();
        a.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,selectable,true);
        share.setBackgroundResource(selectable.resourceId);
        share.setEnabled(!detail.sourceUris.isEmpty());share.setAlpha(share.isEnabled()?1f:0.4f);
        share.setOnClickListener(v -> selection.story(() -> {
            Intent intent=MemoryManagementRoute.of(MemoryManagementRoute.RECORD,detail.item.memoryKey,"",detail.item.title)
                    .apply(new Intent(a,MemoryManagementActivity.class));
            intent.putExtra("story_selection",true);
            a.startActivityForResult(intent,REQUEST);
        }));
        return share;
    }
    static final String SELECTED_TRASH = "selected_trash";

    static void openSelectedTrash(Activity activity, MemoryBrowserDetail detail, ArrayList<String> uris) {
        Intent intent = MemoryManagementRoute.of(MemoryManagementRoute.RECORD, detail.item.memoryKey, "", detail.item.title)
                .apply(new Intent(activity, MemoryManagementActivity.class));
        intent.putStringArrayListExtra(SELECTED_TRASH, uris);
        activity.startActivityForResult(intent, REQUEST);
    }

    static void openSingle(Context c,MemoryPhotoItem item) {if(item!=null&&!item.sourceUri.isEmpty())open(c,"",item.sourceUri);}
    static void openSingle(Context c,MemoryPhotoItem item,String originKey) {if(item!=null&&!item.sourceUri.isEmpty())open(c,originKey,item.sourceUri);}
    static void openSinglePlaceChange(Context c,MemoryPhotoItem item,String originKey,boolean unassignOnly) {
        if(item==null||item.sourceUri.isEmpty())return;
        Intent intent=MemoryManagementRoute.of(MemoryManagementRoute.SINGLE,originKey,item.sourceUri,"")
                .apply(new Intent(c,MemoryManagementActivity.class));
        intent.putExtra("single_place_action",unassignOnly?"unassign":"change");
        if(c instanceof Activity)((Activity)c).startActivityForResult(intent,REQUEST);
        else {intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(intent);}
    }
    private static void open(Context c,String key,String uri) {
        open(c,key,uri,"");
    }
    private static void open(Context c,String key,String uri,String title) {
        String mode=!uri.isEmpty()?MemoryManagementRoute.SINGLE:!key.isEmpty()?MemoryManagementRoute.RECORD:MemoryManagementRoute.ADD;
        Intent intent=MemoryManagementRoute.of(mode,key,uri,title).apply(new Intent(c,MemoryManagementActivity.class));
        if(c instanceof Activity)((Activity)c).startActivityForResult(intent,REQUEST);
        else {intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(intent);}
    }
    // Returning even after cancellation reloads the original record from live MediaStore.
    // The result signals a return route, never proof that files were moved.
    static void prepareReturn(Activity a,Intent intent) {
        MemoryManagementRoute origin=MemoryManagementRoute.from(intent);
        a.setResult(Activity.RESULT_OK,new Intent().putExtra("managementReturnKey",origin.key));
    }
    static void result(int request,Intent data,Consumer<String> refresh) {
        if(request!=REQUEST||data==null)return;
        String key=data.getStringExtra("managementReturnKey");
        String normalized=key==null?"":key.trim();
        if(viewerReturnHandler!=null&&viewerReturnHandler.onSingleActionReturn(normalized,refresh)) {
            return;
        }
        refresh.accept(normalized);
    }
    static void openRecord(Activity a,String key) {
        a.startActivity(new Intent(a,MainActivity.class).putExtra("managedMemoryKey",key)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));a.finish();
    }
    static void route(Intent intent,Consumer<String> open) {
        if(intent==null)return;String key=intent.getStringExtra("managedMemoryKey");
        intent.removeExtra("managedMemoryKey");
        if(key!=null&&!key.isEmpty())new Handler(Looper.getMainLooper()).post(()->open.accept(key));
    }
}

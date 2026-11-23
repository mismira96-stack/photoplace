package com.example.gallerysorter;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/** Reattach the exact originating detail, without rebuilding its media or grid on Back. */
final class MemoryViewerReturnController {
    private final Activity activity;
    private final HomeRevisionReader revisions;
    private final ViewerReturnState<SavedScreen> state = new ViewerReturnState<>();
    private View originRoot;
    private String originRevision;
    private String pendingRevision;

    MemoryViewerReturnController(Activity activity) {
        this.activity = activity;
        revisions = new HomeRevisionReader(activity.getApplicationContext());
    }

    void beginDetail() {
        invalidate();
        pendingRevision = revisions.read();
    }

    void useDetailRevision(String revision) {
        invalidate();
        pendingRevision = revision;
    }

    void detailBound() {
        ViewGroup content = activity.findViewById(android.R.id.content);
        String stable = HomeDashboardPolicy.stableRevision(pendingRevision, revisions.read());
        pendingRevision = null;
        if (content != null && content.getChildCount() == 1 && !stable.isEmpty()) {
            originRoot = content.getChildAt(0);
            originRevision = stable;
        }
    }

    void capture(boolean alreadyViewing, boolean collectionDetail, String collectionId, boolean working,
                 Runnable restoreMemoryMode, Runnable restoreCollectionMode) {
        if (alreadyViewing) return;
        state.clear();
        if (working) return;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() != 1 || content.getChildAt(0) != originRoot) return;
        Runnable restoreMode = collectionDetail && collectionId != null && !collectionId.isEmpty()
                ? restoreCollectionMode : restoreMemoryMode;
        // Use the revision of the displayed detail, not the later viewer-open time.
        state.remember(new SavedScreen(originRoot, restoreMode), originRevision);
    }

    void restoreOrRebuild(Runnable rebuild) {
        long started = SystemClock.elapsedRealtime();
        SavedScreen saved = state.hasScreen() ? state.take(revisions.read()) : null;
        if (saved == null || saved.root.getParent() != null) {
            Log.i("PhotoPlaceViewer", "returnReused=false checkMs=" + (SystemClock.elapsedRealtime() - started));
            rebuild.run();
            return;
        }
        saved.restoreMode.run();
        activity.setContentView(saved.root);
        saved.root.requestApplyInsets();
        Log.i("PhotoPlaceViewer", "returnReused=true reattachMs=" + (SystemClock.elapsedRealtime() - started));
    }

    // External apps, background transitions and layout changes must use a fresh detail.
    void invalidate() {
        state.clear();
        originRoot = null;
        originRevision = null;
        pendingRevision = null;
    }

    private static final class SavedScreen {
        final View root;
        final Runnable restoreMode;
        SavedScreen(View root, Runnable restoreMode) {
            this.root = root;
            this.restoreMode = restoreMode;
        }
    }
}

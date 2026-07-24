package com.example.gallerysorter;

import android.content.Context;
import android.content.SharedPreferences;

final class SortProgressStore {
    static final String PREFS = "sort_progress";
    static final String KEY_ACTIVE = "active";
    static final String KEY_LABEL = "label";
    static final String KEY_CURRENT = "current";
    static final String KEY_TOTAL = "total";
    static final String KEY_CONTEXT = "context";
    static final String KEY_STARTED_AT = "started_at";
    static final String KEY_UPDATED_AT = "updated_at";

    private SortProgressStore() {
    }

    static Snapshot read(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
        return new Snapshot(
                prefs.getBoolean(KEY_ACTIVE, false),
                prefs.getString(KEY_LABEL, ""),
                prefs.getInt(KEY_CURRENT, 0),
                prefs.getInt(KEY_TOTAL, 0),
                prefs.getString(KEY_CONTEXT, ""),
                prefs.getLong(KEY_STARTED_AT, 0L),
                prefs.getLong(KEY_UPDATED_AT, 0L));
    }

    static void start(Context context, String label) {
        long now = System.currentTimeMillis();
        context.getSharedPreferences(PREFS, 0).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putString(KEY_LABEL, label == null ? "정리 준비 중" : label)
                .putInt(KEY_CURRENT, 0)
                .putInt(KEY_TOTAL, 0)
                .putString(KEY_CONTEXT, "")
                .putLong(KEY_STARTED_AT, now)
                .putLong(KEY_UPDATED_AT, now)
                .apply();
    }

    static void update(Context context, String label, int current, int total, String progressContext) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, 0).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putString(KEY_LABEL, label == null ? "정리 진행 중" : label)
                .putInt(KEY_CURRENT, Math.max(0, current))
                .putInt(KEY_TOTAL, Math.max(0, total))
                .putString(KEY_CONTEXT, progressContext == null ? "" : progressContext)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis());
        editor.apply();
    }

    static void finish(Context context) {
        context.getSharedPreferences(PREFS, 0).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    static final class Snapshot {
        final boolean active;
        final String label;
        final int current;
        final int total;
        final String progressContext;
        final long startedAtMillis;
        final long updatedAtMillis;

        Snapshot(boolean active, String label, int current, int total, String progressContext, long startedAtMillis, long updatedAtMillis) {
            this.active = active;
            this.label = label == null ? "" : label;
            this.current = Math.max(0, current);
            this.total = Math.max(0, total);
            this.progressContext = progressContext == null ? "" : progressContext;
            this.startedAtMillis = startedAtMillis;
            this.updatedAtMillis = updatedAtMillis;
        }

        boolean isFresh(long nowMillis) {
            return active && updatedAtMillis > 0L && nowMillis - updatedAtMillis < 10L * 60L * 1000L;
        }
    }
}

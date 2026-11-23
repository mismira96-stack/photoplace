package com.example.gallerysorter;

import android.content.Context;

/** Advisory capture state is separate from authoritative user assignments. */
final class ManualInventoryStatus {
    private static final String PREFS="manual_inventory_status";
    static void write(Context c,String state) {
        if (!c.getSharedPreferences(PREFS,0).edit().putString("state",state)
                .putLong("updatedAt",System.currentTimeMillis()).commit())
            android.util.Log.w("ManualPlaceInventory","Capture status could not be persisted");
    }
    static String state(Context c) { return c.getSharedPreferences(PREFS,0).getString("state","UNCAPTURED"); }
    static boolean captured(ManualPlaceSnapshot snapshot) {
        try { org.json.JSONObject value=snapshot.writableCopy().optJSONObject("inventory");
            return value!=null && value.optLong("capturedAt",0)>0; }
        catch(Exception unavailable) { return false; }
    }
    static String description(Context c,ManualPlaceSnapshot snapshot) {
        if (!captured(snapshot)) return "사진을 다시 찾으면 위치 없는 사진을 정리할 수 있어요. 홈에서 사진 찾기를 실행해 주세요.";
        if (!"SUCCEEDED".equals(state(c))) return "최근 검색이 끝나지 않았어요. 이전 검색 결과를 보여드려요. 홈에서 사진을 다시 찾아 주세요.";
        return "최근 검색한 폴더에서 접근 가능한 사진·동영상을 보여드려요.";
    }
}

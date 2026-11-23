package com.example.gallerysorter;

import android.content.*;

/** Explicit UI intent: adding unlocated media never leaks into record/single-file trash. */
final class MemoryManagementRoute {
    static final String ADD="add_unlocated",RECORD="record_trash",SINGLE="single_trash";
    final String mode,key,uri,title;
    private MemoryManagementRoute(String mode,String key,String uri,String title){this.mode=mode;this.key=key;this.uri=uri;this.title=title;}
    static MemoryManagementRoute of(String mode,String key,String uri,String title) {
        key=clean(key);uri=clean(uri);title=clean(title);
        if(!uri.isEmpty())return new MemoryManagementRoute(SINGLE,key,uri,"사진 정리");
        if(!key.isEmpty())return new MemoryManagementRoute(RECORD,key,"",title.isEmpty()?"사진 정리":title);
        if(ADD.equals(mode)||clean(mode).isEmpty())return new MemoryManagementRoute(ADD,"","","위치 없는 사진");
        // A trash route with lost source context stays a trash screen; never reclassify as ADD.
        return new MemoryManagementRoute(SINGLE,"","","사진 정리");
    }
    boolean addsPlace(){return ADD.equals(mode);}
    static String viewerKey(int mode,int memoryMode,String key){return mode==memoryMode?clean(key):"";}
    static String recordKey(boolean detail,String key){return detail?clean(key):"";}
    static MemoryManagementRoute from(Intent intent) {
        return of(intent.getStringExtra("managementMode"),intent.getStringExtra("memoryKey"),intent.getStringExtra("singleUri"),intent.getStringExtra("memoryTitle"));
    }
    Intent apply(Intent intent){return intent.putExtra("managementMode",mode).putExtra("memoryKey",key).putExtra("singleUri",uri).putExtra("memoryTitle",title);}
    static void save(Context c,MemoryManagementRoute route)throws Exception {
        if(!c.getSharedPreferences("memory_trash_route",0).edit().putString("mode",route.mode).putString("key",route.key)
                .putString("uri",route.uri).putString("title",route.title).commit())throw new IllegalStateException("사진 정리 화면을 저장하지 못했어요.");
    }
    static MemoryManagementRoute saved(Context c) {
        android.content.SharedPreferences p=c.getSharedPreferences("memory_trash_route",0);
        return of(p.getString("mode",SINGLE),p.getString("key",""),p.getString("uri",""),p.getString("title","사진 정리"));
    }
    private static String clean(String value){return value==null?"":value.trim();}
}

package com.example.gallerysorter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import java.util.*;

/** Read-only MediaStore identity/availability checks shared by manual places and intentional trash. */
final class MemoryMediaAccess {
    static final class Pending extends IllegalStateException {Pending(){super("아직 저장 중인 사진이에요.");}}
    private final Context context;
    private final Map<String,String> versions=new HashMap<>();
    MemoryMediaAccess(Context context) { this.context=context.getApplicationContext(); }

    static final class State {
        final UnresolvedMediaRecord media;
        final boolean trashed;
        State(UnresolvedMediaRecord media,boolean trashed) { this.media=media;this.trashed=trashed; }
    }
    private String version(String volume) {
        if (!versions.containsKey(volume)) versions.put(volume,Build.VERSION.SDK_INT>=30
                ?MediaStore.getVersion(context,volume):MediaStore.getVersion(context));
        return versions.get(volume);
    }
    private String[] projection() {
        ArrayList<String> columns=new ArrayList<>(Arrays.asList("_id","volume_name","_display_name","mime_type",
                "relative_path","_size","date_modified","datetaken","is_pending"));
        if(Build.VERSION.SDK_INT>=30)columns.add("is_trashed");
        return columns.toArray(new String[0]);
    }
    State read(String uri,boolean includeTrash) throws Exception {
        if(uri==null||!uri.matches("content://media/[^/]+/(images|video)/media/[0-9]+"))
            throw new IllegalArgumentException("지원하는 기기 사진이 아니에요.");
        String kind=uri.contains("/images/")?"PHOTO":"VIDEO";
        Bundle args=new Bundle();
        if(Build.VERSION.SDK_INT>=30&&includeTrash)args.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,MediaStore.MATCH_INCLUDE);
        try(Cursor cursor=context.getContentResolver().query(Uri.parse(uri),projection(),args,null)) {
            if(cursor==null)throw new IllegalStateException("사진 상태를 확인하지 못했어요.");
            if(!cursor.moveToFirst())return null;
            return state(cursor,uri,kind);
        }
    }
    private State state(Cursor c,String uri,String kind) {
        if(number(c,"is_pending")!=0)throw new Pending();
        String volume=text(c,"volume_name");
        return new State(new UnresolvedMediaRecord(uri,volume,version(volume),number(c,"_id"),kind,
                text(c,"_display_name"),text(c,"mime_type"),text(c,"relative_path"),number(c,"_size"),
                number(c,"date_modified"),number(c,"datetaken")),number(c,"is_trashed")!=0);
    }

    Map<String,UnresolvedMediaRecord> available(Collection<UnresolvedMediaRecord> expected,boolean includeTrash) throws Exception {
        LinkedHashMap<String,UnresolvedMediaRecord> wanted=new LinkedHashMap<>();
        for(UnresolvedMediaRecord r:expected)wanted.put(r.assetKey(),r);
        Map<String,List<UnresolvedMediaRecord>> groups=new LinkedHashMap<>();
        for(UnresolvedMediaRecord r:wanted.values()) {
            if(!r.mediaVersion.equals(version(r.volume)))continue;
            groups.computeIfAbsent(r.volume+"|"+r.kind,k->new ArrayList<>()).add(r);
        }
        Map<String,UnresolvedMediaRecord> result=new LinkedHashMap<>();
        for(List<UnresolvedMediaRecord> group:groups.values())for(int start=0;start<group.size();start+=400) {
            List<UnresolvedMediaRecord> batch=group.subList(start,Math.min(start+400,group.size()));
            UnresolvedMediaRecord sample=batch.get(0);
            Uri collection=sample.kind.equals("PHOTO")?MediaStore.Images.Media.getContentUri(sample.volume)
                    :MediaStore.Video.Media.getContentUri(sample.volume);
            List<String> ids=new ArrayList<>();
            Map<Long,UnresolvedMediaRecord> byId=new HashMap<>();
            for(UnresolvedMediaRecord r:batch){ids.add(Long.toString(r.mediaId));byId.put(r.mediaId,r);}
            Bundle args=new Bundle();
            args.putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION,"_id IN ("+
                    String.join(",",Collections.nCopies(ids.size(),"?"))+") AND is_pending=0");
            args.putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,ids.toArray(new String[0]));
            if(Build.VERSION.SDK_INT>=30&&includeTrash)args.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,MediaStore.MATCH_INCLUDE);
            try(Cursor c=context.getContentResolver().query(collection,projection(),args,null)) {
                if(c==null)throw new IllegalStateException("사진 접근 권한을 확인해 주세요.");
                while(c.moveToNext()) {
                    UnresolvedMediaRecord old=byId.get(number(c,"_id"));
                    if(old==null)continue;
                    State actual=state(c,old.uri,sample.kind);
                    if((includeTrash||!actual.trashed)&&actual.media.assetKey().equals(old.assetKey()))
                        result.put(old.assetKey(),actual.media);
                }
            }
        }
        return result;
    }

    List<UnresolvedMediaRecord> requireFresh(List<UnresolvedMediaRecord> selected) throws Exception {
        Map<String,UnresolvedMediaRecord> live=available(selected,false);
        if(live.size()!=new HashSet<>(keys(selected)).size())throw new IllegalStateException("일부 사진이 없거나 접근할 수 없어요. 다시 불러와 주세요.");
        return new ArrayList<>(live.values());
    }
    List<UnresolvedMediaRecord> observeUris(Collection<String> uris) throws Exception {
        Map<String,LinkedHashMap<Long,String>> groups=new LinkedHashMap<>();
        for(String uri:uris) {
            if(!uri.matches("content://media/[^/]+/(images|video)/media/[0-9]+"))continue;
            Uri parsed=Uri.parse(uri);List<String> parts=parsed.getPathSegments();
            String prefix="content://media/"+parts.get(0)+"/"+parts.get(1)+"/media";
            groups.computeIfAbsent(prefix,k->new LinkedHashMap<>()).put(Long.parseLong(parsed.getLastPathSegment()),uri);
        }
        Map<String,UnresolvedMediaRecord> result=new LinkedHashMap<>();
        for(Map.Entry<String,LinkedHashMap<Long,String>> group:groups.entrySet()) {
            List<Long> ids=new ArrayList<>(group.getValue().keySet());
            for(int start=0;start<ids.size();start+=400) {
                List<Long> batch=ids.subList(start,Math.min(start+400,ids.size()));
                String[] args=new String[batch.size()];for(int i=0;i<batch.size();i++)args[i]=batch.get(i).toString();
                String where="_id IN ("+String.join(",",Collections.nCopies(batch.size(),"?"))+") AND is_pending=0";
                if(Build.VERSION.SDK_INT>=30)where+=" AND is_trashed=0";
                Set<String> returnedUris=new HashSet<>();
                try(Cursor c=context.getContentResolver().query(Uri.parse(group.getKey()),projection(),where,args,null)) {
                    if(c==null)throw new IllegalStateException("사진 목록을 확인하지 못했어요.");
                    while(c.moveToNext()) {
                        String uri=group.getValue().get(number(c,"_id"));
                        if(uri==null)continue;
                        if(!returnedUris.add(uri))throw new IllegalStateException("저장소의 사진 식별자가 겹쳐 있어요.");
                        State state=state(c,uri,group.getKey().contains("/images/")?"PHOTO":"VIDEO");
                        result.putIfAbsent(state.media.assetKey(),state.media);
                    }
                }
            }
        }
        return new ArrayList<>(result.values());
    }
    List<UnresolvedMediaRecord> observeExactUris(List<String> uris) throws Exception {
        Map<String, UnresolvedMediaRecord> byUri = new HashMap<>();
        for (UnresolvedMediaRecord row : observeUris(uris)) byUri.put(row.uri, row);
        List<UnresolvedMediaRecord> ordered = new ArrayList<>(uris.size());
        Set<String> seen = new HashSet<>();
        for (String uri : uris) {
            UnresolvedMediaRecord row = byUri.get(uri);
            if (row == null || !seen.add(uri)) {
                throw new IllegalStateException("선택한 사진을 다시 확인해 주세요.");
            }
            ordered.add(row);
        }
        return ordered;
    }
    private List<String> keys(List<UnresolvedMediaRecord> rows){List<String> keys=new ArrayList<>();for(UnresolvedMediaRecord r:rows)keys.add(r.assetKey());return keys;}
    private static long number(Cursor c,String column){int i=c.getColumnIndex(column);return i<0||c.isNull(i)?0:c.getLong(i);}
    private static String text(Cursor c,String column){int i=c.getColumnIndex(column);return i<0||c.isNull(i)?"":c.getString(i);}
}

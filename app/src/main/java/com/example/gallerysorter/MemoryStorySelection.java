package com.example.gallerysorter;

import java.util.*;
import java.text.SimpleDateFormat;

/** Immutable export input from existing selection. No repository or media mutations. */
final class MemoryStorySelection {
    final List<String> uris;
    final MemoryStoryConfig config;
    private MemoryStorySelection(List<String> uris, MemoryStoryConfig config) {
        this.uris = Collections.unmodifiableList(uris); this.config = config;
    }
    static MemoryStorySelection from(String title, List<UnresolvedMediaRecord> selected) {
        if (selected == null || selected.isEmpty()) throw new IllegalArgumentException("공유할 사진을 선택해 주세요");
        if (selected.size() > 4) throw new IllegalArgumentException("Instagram Story에는 최대 4장까지 선택할 수 있어요");
        List<String> uris = new ArrayList<>(); long first=Long.MAX_VALUE,last=0;
        for (UnresolvedMediaRecord photo : selected) {
            if (photo == null || !"PHOTO".equals(photo.kind)) throw new IllegalArgumentException("Instagram Story에는 사진을 선택해 주세요");
            uris.add(photo.uri);
            if (photo.takenAt > 0) {first=Math.min(first,photo.takenAt);last=Math.max(last,photo.takenAt);}
        }
        SimpleDateFormat format=new SimpleDateFormat("M월 d일",Locale.KOREA);
        String date=last==0?"":format.format(new Date(first));
        if(last>0&&!date.equals(format.format(new Date(last))))date+=" ~ "+format.format(new Date(last));
        return new MemoryStorySelection(uris, new MemoryStoryConfig(uris.size()==1
                ?MemoryStoryConfig.LayoutMode.SINGLE_FRAME:MemoryStoryConfig.LayoutMode.PHOTO_STRIP,
                true,MemoryStoryCaption.place(title),date,MemoryStoryConfig.FontKind.SANS,
                MemoryStoryConfig.ThemeTone.CREAM,1f));
    }
}

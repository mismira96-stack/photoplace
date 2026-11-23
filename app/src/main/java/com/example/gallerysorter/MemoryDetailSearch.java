package com.example.gallerysorter;

import java.util.*;

/** Read-only search of already resolved media and notes; never scans or changes media dates. */
final class MemoryDetailSearch {
    static List<MemoryPhotoSection> filter(List<MemoryPhotoSection> sections,List<MemoryDateNote> notes,String query) {
        String text=normalize(query);
        if(text.isEmpty())return sections;
        String[] tokens=text.split("\\s+");
        Map<String,String> byDate=new HashMap<>();
        for(MemoryDateNote note:notes)byDate.put(note.dateKey,note.text);
        List<MemoryPhotoSection> result=new ArrayList<>();
        for(MemoryPhotoSection section:sections) {
            String common=normalize(section.dateKey+" "+section.dateText+" "+section.placeText+" "+byDate.getOrDefault(section.dateKey,""));
            List<MemoryPhotoItem> matches=new ArrayList<>();
            for(MemoryPhotoItem photo:section.photos) {
                String hay=common+" "+normalize(photo.displayName)+" "+(photo.mediaKind==MediaKind.VIDEO?"동영상 video":"사진 photo");
                if(matches(hay,tokens))matches.add(photo);
            }
            if(!matches.isEmpty()||(section.photos.isEmpty()&&matches(common,tokens)))
                result.add(MemoryPhotoSection.fromCollectionPlace(section.dateKey,section.dateText,section.placeText,matches));
        }
        return Collections.unmodifiableList(result);
    }
    private static boolean matches(String hay,String[] tokens) {
        for(String token:tokens)if(!hay.contains(token))return false;
        return true;
    }
    private static String normalize(String value) {
        if(value==null)return "";
        // Support yyyy.MM.dd / yyyy-MM-dd / yyyyMMdd without modifying the actual date key.
        return value.toLowerCase(Locale.ROOT).replaceAll("(?<=\\d)[./-](?=\\d)","").trim();
    }
}

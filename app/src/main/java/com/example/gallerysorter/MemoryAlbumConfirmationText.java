package com.example.gallerysorter;

final class MemoryAlbumConfirmationText {
    static String added(int photos,int videos,boolean moveVideos) {
        String counts=photos>0?"사진 "+photos+"장":"";
        if(moveVideos&&videos>0)counts+=(counts.isEmpty()?"":" · ")+"동영상 "+videos+"개";
        return counts.isEmpty()?"새로 넣을 항목 없음":"새로 넣을 "+counts;
    }
}

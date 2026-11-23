package com.example.gallerysorter;

import android.app.Activity;
import java.util.function.Consumer;

/** One policy is shared by the single-memory review and the subsequent album execution. */
final class MemoryAlbumConfirmationController {
    static void show(Activity activity,MemoryRecord record,String alias,
                     DiscoveryAlbumOrganizer.Preparation preparation,boolean videoPreference,
                     String currentKey,Consumer<String> notice,Consumer<String> detail,
                     Runnable albums,Consumer<String> start) {
        String key=record==null?currentKey:record.memoryKey;
        if(preparation==null||preparation.items.isEmpty()) {
            notice.accept("현재 접근할 수 있는 미디어가 없어요.");detail.accept(key);return;
        }
        boolean moveVideos=OrganizationMediaPolicy.shouldMoveDiscoveryVideos(videoPreference,preparation);
        if(preparation.actionableCount(moveVideos)<=0) {
            if(preparation.duplicateCount>0&&preparation.excludedVideoCount(moveVideos)==0) {
                notice.accept("새 파일은 만들지 않았어요. 기존 앨범은 위치 앨범에서 확인해 주세요.");albums.run();
            } else {
                notice.accept(preparation.excludedVideoCount(moveVideos)>0
                        ?"동영상 이동 설정과 위치기록 연결을 확인해 주세요. 파일은 변경하지 않았어요.":"새로 정리할 사진이 없어요.");
                detail.accept(key);
            }
            return;
        }
        new DiscoveryOrganizeConfirmDialog(activity).showSinglePlace(preparation,moveVideos,()->{
            String id=new MemoryIdentityRegistryStore(activity).resolveOrCreate(alias);
            if(id.isEmpty()){notice.accept("기억 정보를 준비하지 못했어요.");return;}
            start.accept(id);
        });
    }
}

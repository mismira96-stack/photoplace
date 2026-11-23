package com.example.gallerysorter;

import java.util.function.Consumer;

/** Called only after the asynchronous reservation and enqueue have succeeded. */
final class DiscoverySortStartFeedback {
    static void present(boolean discovery,OrganizationRequest request,Runnable showResult,Consumer<String> notice) {
        if(!discovery)return;
        if(request==null||request.subjectType!=OrganizationLink.SubjectType.MEMORY)showResult.run();
        else notice.accept("이 장소의 앨범을 만들고 있어요.");
    }
}

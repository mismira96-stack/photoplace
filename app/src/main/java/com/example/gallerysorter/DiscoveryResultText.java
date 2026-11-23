package com.example.gallerysorter;

/** Update counts describe places receiving new media, not newly identified locations. */
final class DiscoveryResultText {
    static String title(int updatedPlaces) {
        return updatedPlaces > 0 ? "새 사진이 발견 기록에 추가됐어요" : "사진 확인을 마쳤어요";
    }

    static String summary(int updatedPlaces, int discoverableItems, int noLocationItems) {
        String text = updatedPlaces > 0
                ? "새 사진·동영상이 추가된 장소 " + updatedPlaces + "곳"
                : "새로 추가된 항목 없음";
        return noLocationItems > 0 ? text + " · 위치 없는 항목 " + java.text.NumberFormat.getIntegerInstance(java.util.Locale.KOREA).format(noLocationItems) + "개" : text;
    }
}

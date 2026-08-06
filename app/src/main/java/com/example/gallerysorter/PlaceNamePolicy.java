package com.example.gallerysorter;

import java.util.Locale;

final class PlaceNamePolicy {
    static final String LOCATION_NONE = "위치없음";
    private static final String[] SEOUL_DISTRICTS = {"강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"};

    private PlaceNamePolicy() {
    }

    static boolean isSeoulAddressLine(String value) {
        String normalized = normalizeForMatch(value);
        if (normalized.contains("서울특별시") || normalized.contains("서울시") || normalized.contains("seoul")) {
            return true;
        }
        for (String district : SEOUL_DISTRICTS) {
            if (normalized.contains("서울" + normalizeForMatch(district))) {
                return true;
            }
        }
        return romanizedSeoulDistrictName(normalized) != null;
    }

    static String romanizedSeoulDistrictName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace("-", "").replace("_", "");
        String[][] districts = {
                {"gangnamgu", "강남구"}, {"gangdonggu", "강동구"}, {"gangbukgu", "강북구"}, {"gangseogu", "강서구"},
                {"gwanakgu", "관악구"}, {"gwangjingu", "광진구"}, {"gurogu", "구로구"}, {"geumcheongu", "금천구"},
                {"nowongu", "노원구"}, {"dobonggu", "도봉구"}, {"dongdaemungu", "동대문구"}, {"dongjakgu", "동작구"},
                {"mapogu", "마포구"}, {"seodaemungu", "서대문구"}, {"seochogu", "서초구"}, {"seongdonggu", "성동구"},
                {"seongbukgu", "성북구"}, {"songpagu", "송파구"}, {"yangcheongu", "양천구"}, {"yeongdeungpogu", "영등포구"},
                {"yongsangu", "용산구"}, {"eunpyeonggu", "은평구"}, {"jongnogu", "종로구"}, {"junggu", "중구"},
                {"jungnanggu", "중랑구"}
        };
        for (String[] district : districts) {
            if (normalized.contains(district[0])) {
                return district[1];
            }
        }
        return null;
    }

    static String firstDistrictName(String... values) {
        for (String value : values) {
            String district = extractDistrictName(value);
            if (district != null) {
                return district;
            }
        }
        return null;
    }

    static String extractDistrictName(String value) {
        if (value != null && !value.trim().isEmpty()) {
            String normalized = normalizeForMatch(value);
            String romanizedDistrict = romanizedSeoulDistrictName(normalized);
            if (romanizedDistrict != null) {
                return romanizedDistrict;
            }
            for (String district : SEOUL_DISTRICTS) {
                if (normalized.contains(normalizeForMatch(district))) {
                    return district;
                }
            }
            for (String part : value.trim().split("[\\s,]+")) {
                String folderName = safeFolderName(part);
                if (folderName.endsWith("구") && folderName.length() >= 2) {
                    return folderName;
                }
            }
        }
        return null;
    }

    static String cleanSeoulDetailName(String... values) {
        for (String value : values) {
            String detail = cleanSeoulDetailName(value);
            if (detail != null) {
                return detail;
            }
        }
        return null;
    }

    static String cleanSeoulDetailName(String value) {
        if (value != null && !value.trim().isEmpty()) {
            String cleaned = value.trim().replace("대한민국", "").replace("서울특별시", "").replace("서울시", "").replace("서울", "");
            for (String district : SEOUL_DISTRICTS) {
                cleaned = cleaned.replace(district, "");
            }
            String trimmed = cleaned.replaceAll("\\d+[\\-\\d]*", "").replaceAll("(대로|로|길)$", "").replaceAll("[,()\\[\\]]", " ").replaceAll("\\s+", " ").trim();
            if (normalizeForMatch(trimmed).contains("예술의전당")) {
                return "예술의전당";
            }
            String normalizedDetail = normalizeForMatch(trimmed);
            String knownPlaceName = knownPlaceName(normalizedDetail);
            if (knownPlaceName != null) {
                return knownPlaceName;
            }
            String romanizedDistrict = romanizedSeoulDistrictName(normalizeForMatch(trimmed));
            if (romanizedDistrict != null) {
                return romanizedDistrict;
            }
        }
        return null;
    }

    static boolean isNoisySeoulDetailName(String value) {
        String normalized = normalizeForMatch(value);
        return normalized.contains("mall") || normalized.contains("센터") || normalized.contains("건물") || normalized.contains("층") || normalized.contains("지하") || normalized.contains("대로") || normalized.contains("로") || normalized.contains("길") || normalized.contains("어린이집") || normalized.contains("아파트") || normalized.contains("오피스텔") || normalized.contains("상가") || hasAccessPointNoise(normalized) || normalized.contains("b1") || normalized.contains("b2") || normalized.contains("bf") || normalized.matches(".*\\d+f.*") || normalized.matches(".*\\d+호.*");
    }

    static String cleanPoiLocationName(String... values) {
        String best = null;
        int bestScore = -1;
        for (String value : values) {
            String candidate = cleanPoiLocationName(value);
            if (candidate != null) {
                int score = poiScore(candidate);
                if (score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    static String cleanPoiLocationName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String cleaned = value.trim().replace("대한민국", "").replaceAll("[,()\\[\\]]", " ").replaceAll("\\s+", " ");
        String knownPlaceName = knownPlaceName(normalizeForMatch(cleaned));
        if (knownPlaceName != null) {
            return knownPlaceName;
        }
        String[] parts = cleaned.split("\\s+");
        for (String part : parts) {
            String folderName = safeFolderName(part);
            if (looksLikePoiName(folderName)) {
                return folderName;
            }
        }
        String folderName = safeFolderName(cleaned);
        return looksLikePoiName(folderName) ? folderName : null;
    }

    static boolean looksLikePoiName(String value) {
        if (value == null || value.length() < 4 || value.length() > 24) {
            return false;
        }
        String normalized = normalizeForMatch(value);
        return !normalized.matches(".*\\d+.*") && !isAdministrativeOnlyName(normalized) && !isNoisyPlaceCandidate(normalized) && poiScore(value) > 0;
    }

    static boolean isNoisyPlaceCandidate(String value) {
        return value.contains("militopiacity") || value.contains("밀리토피아시티") || value.contains("지하") || value.contains("상가") || value.contains("아파트") || value.contains("오피스텔") || hasAccessPointNoise(value);
    }

    static boolean hasAccessPointNoise(String value) {
        return value.contains("북문") || value.contains("남문") || value.contains("동문") || value.contains("서문") || value.contains("출입구") || value.contains("입구") || value.contains("northgate") || value.contains("southgate") || value.contains("eastgate") || value.contains("westgate") || value.contains("gate");
    }

    static boolean isAdministrativeOnlyName(String value) {
        return value.endsWith("도") || value.endsWith("시") || value.endsWith("군") || value.endsWith("구") || value.endsWith("동") || value.endsWith("읍") || value.endsWith("면") || value.endsWith("리");
    }

    static int poiScore(String value) {
        return knownPlaceName(normalizeForMatch(value)) == null ? 0 : 110;
    }

    static String normalizeLocationKey(String value) {
        String knownPlaceName = knownPlaceName(normalizeForMatch(value));
        if (knownPlaceName != null) {
            return knownPlaceName;
        }
        String knownTravelPlaceName = knownTravelPlaceName(normalizeForMatch(value));
        if (knownTravelPlaceName != null) {
            return knownTravelPlaceName;
        }
        String districtName = extractDistrictName(value);
        if (districtName != null) {
            return districtName;
        }
        String folderName = safeFolderName(stripAdministrativeSuffix(value.trim().replace("대한민국", "").replace("특별자치도", "").replace("특별자치시", "").replace("특별시", "").replace("광역시", "").replace("자치시", "")).replaceAll("\\s+", ""));
        return folderName.isEmpty() ? LOCATION_NONE : folderName;
    }

    static String knownTravelPlaceName(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if (containsAny(normalized, "\u4e2d\u592e\u533a", "주오구", "chuo")
                && containsAny(normalized, "sapporo", "삿포로", "삿포로시", "\u672d\u5e4c", "hokkaido", "홋카이도", "\u5317\u6d77\u9053")) {
            return "삿포로";
        }
        if (containsAny(normalized, "sapporo", "삿포로", "삿포로시", "札幌", "札幌市", "札幌市中央区")) {
            return "삿포로";
        }
        if (containsAny(normalized, "fukuoka", "후쿠오카", "후쿠오카시", "福岡", "福岡市")) {
            return "후쿠오카";
        }
        if (containsAny(normalized, "chitose", "千歳", "千歳市")) {
            return "치토세";
        }
        if (containsAny(normalized, "biei", "美瑛", "美瑛町", "비에이조", "비에이町")) {
            return "비에이";
        }
        if (containsAny(normalized, "kamifurano", "上富良野", "上富良野町")) {
            return "가미후라노";
        }
        if (containsAny(normalized, "nakafurano", "中富良野", "中富良野町")) {
            return "나카후라노";
        }
        if (containsAny(normalized, "otaru", "小樽", "小樽市")) {
            return "오타루";
        }
        if (containsAny(normalized, "iwamizawa", "岩見沢", "岩見沢市")) {
            return "이와미자와";
        }
        return null;
    }

    static boolean containsAny(String normalized, String... aliases) {
        for (String alias : aliases) {
            String normalizedAlias = normalizeForMatch(alias);
            if (!normalizedAlias.isEmpty() && normalized.contains(normalizedAlias)) {
                return true;
            }
        }
        return false;
    }
    static String knownPlaceName(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if (hasAccessPointNoise(normalized)) {
            return null;
        }
        if (isKnownPlaceAlias(normalized, "에버랜드", "everland", "everlandresort")) {
            return "에버랜드";
        }
        if (isKnownPlaceAlias(normalized, "롯데월드몰", "롯데월드타워", "롯데호텔월드", "lotteworldmall", "lotteworldtower", "lottehotelworld")) {
            return null;
        }
        if (isKnownPlaceAlias(normalized, "롯데월드", "롯데월드어드벤처", "lotteworld", "lotteworldadventure")) {
            return "롯데월드";
        }
        if (isKnownPlaceAlias(normalized, "예술의전당", "서울예술의전당", "seoulartscenter")) {
            return "예술의전당";
        }
        if (isKnownPlaceAlias(normalized, "분당서울대병원", "분당서울대학교병원", "bundangseoulnationaluniversityhospital", "seoulnationaluniversitybundanghospital")) {
            return "분당서울대병원";
        }
        if (isKnownPlaceAlias(normalized, "인천공항", "인천국제공항", "인천공항제1여객터미널", "인천국제공항제1여객터미널", "인천공항제2여객터미널", "인천국제공항제2여객터미널", "incheonairport", "incheoninternationalairport", "icnairport")) {
            return "인천국제공항";
        }
        return null;
    }

    static boolean isKnownPlaceAlias(String normalized, String... aliases) {
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        for (String alias : aliases) {
            if (normalized.equals(normalizeForMatch(alias))) {
                return true;
            }
        }
        return false;
    }

    static String stripAdministrativeSuffix(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 2 ? trimmed : (!trimmed.endsWith("구") || trimmed.length() > 3) ? trimmed.replaceAll("(시|군|구)$", "") : trimmed;
    }

    static String normalizeForMatch(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.KOREA);
    }

    static String safeFolderName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LOCATION_NONE;
        }
        return value.trim().replace("/", "_").replace("\\", "_").replace(":", "_").replace("*", "_").replace("?", "_").replace("\"", "_").replace("<", "_").replace(">", "_").replace("|", "_");
    }
}

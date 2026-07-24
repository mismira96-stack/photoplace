package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class OverseasMemoryGrouper {
    private static final String[] KOREA_HINTS = {
            "대한민국", "한국", "southkorea", "republicofkorea", "korea",
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충청북", "충남", "충청남", "전북", "전라북",
            "전남", "전라남", "경북", "경상북", "경남", "경상남", "제주",
            "mapogu", "songpagu", "gangnamgu", "seochogu", "jongnogu", "yongsangu"
    };
    private static final String[] OVERSEAS_COUNTRY_HINTS = {
            "일본", "japan", "미국", "usa", "unitedstates", "태국", "thailand",
            "베트남", "vietnam", "프랑스", "france", "이탈리아", "italy",
            "스페인", "spain", "영국", "unitedkingdom", "중국", "china",
            "대만", "taiwan", "홍콩", "hongkong", "싱가포르", "singapore",
            "호주", "australia", "캐나다", "canada", "독일", "germany",
            "스위스", "switzerland", "오스트리아", "austria", "체코", "czech",
            "네덜란드", "netherlands", "괌", "guam"
    };
    private static final String[][] PLACE_COUNTRY_HINTS = {
            {"도쿄", "일본"}, {"오사카", "일본"}, {"교토", "일본"}, {"후쿠오카", "일본"}, {"삿포로", "일본"},
            {"뉴욕", "미국"}, {"로스앤젤레스", "미국"}, {"샌프란시스코", "미국"}, {"라스베가스", "미국"}, {"하와이", "미국"},
            {"방콕", "태국"}, {"푸켓", "태국"}, {"치앙마이", "태국"},
            {"다낭", "베트남"}, {"하노이", "베트남"}, {"호치민", "베트남"},
            {"파리", "프랑스"}, {"니스", "프랑스"},
            {"로마", "이탈리아"}, {"밀라노", "이탈리아"}, {"베네치아", "이탈리아"},
            {"바르셀로나", "스페인"}, {"마드리드", "스페인"},
            {"런던", "영국"}, {"타이베이", "대만"}, {"홍콩", "홍콩"}, {"싱가포르", "싱가포르"},
            {"시드니", "호주"}, {"멜버른", "호주"}, {"밴쿠버", "캐나다"}, {"토론토", "캐나다"},
            {"투몬", "괌"}, {"tumon", "괌"},
            {"fukuoka", "일본"}, {"kurume", "일본"}, {"tosu", "일본"}, {"yufu", "일본"}, {"tsushima", "일본"}, {"kiyama", "일본"},
            {"anglesea", "호주"}, {"belgrave", "호주"}, {"bilinga", "호주"}, {"capewoolamai", "호주"},
            {"cowes", "호주"}, {"docklands", "호주"}, {"easternview", "호주"}, {"flinders", "호주"},
            {"grantville", "호주"}, {"kennettriver", "호주"}, {"kiama", "호주"}, {"melbourne", "호주"},
            {"menziescreek", "호주"}, {"peterborough", "호주"}, {"portcampbell", "호주"}, {"princetown", "호주"},
            {"sherbrooke", "호주"}, {"skenescreek", "호주"}, {"southwharf", "호주"}, {"southbank", "호주"},
            {"stkilda", "호주"}, {"stanwelltops", "호주"}, {"summerlands", "호주"}, {"sydney", "호주"},
            {"unanderra", "호주"}, {"wollongong", "호주"}, {"포트캠벨", "호주"}
    };

    private OverseasMemoryGrouper() {
    }

    static List<MemoryGroup> buildOverseasGroups(List<MainActivity.StoredAlbumSummary> summaries) {
        LinkedHashMap<String, List<MemoryItem>> grouped = new LinkedHashMap<>();
        if (summaries == null) {
            return Collections.emptyList();
        }
        for (MainActivity.StoredAlbumSummary summary : summaries) {
            String country = overseasCountryName(summary);
            if (country.isEmpty()) {
                continue;
            }
            List<MemoryItem> items = grouped.get(country);
            if (items == null) {
                items = new ArrayList<>();
                grouped.put(country, items);
            }
            items.add(new MemoryItem(
                    summary.albumName,
                    summary.relativePath,
                    summary.itemCount,
                    summary.startDate,
                    summary.endDate,
                    summary.thumbnailUri,
                    summary.countryName,
                    summary.adminArea,
                    summary.addressLine));
        }
        ArrayList<MemoryGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<MemoryItem>> entry : grouped.entrySet()) {
            groups.add(new MemoryGroup(entry.getKey(), entry.getKey(), entry.getValue()));
        }
        Collections.sort(groups, new Comparator<MemoryGroup>() {
            @Override
            public int compare(MemoryGroup left, MemoryGroup right) {
                String rightDate = right.endDate == null ? "" : right.endDate;
                String leftDate = left.endDate == null ? "" : left.endDate;
                int dateCompare = rightDate.compareTo(leftDate);
                return dateCompare != 0 ? dateCompare : Integer.compare(right.itemCount, left.itemCount);
            }
        });
        return groups;
    }

    private static String overseasCountryName(MainActivity.StoredAlbumSummary summary) {
        if (summary == null) {
            return "";
        }
        String country = cleanCountry(summary.countryName);
        if (!country.isEmpty()) {
            return isKoreaText(country) ? "" : country;
        }
        String evidence = join(summary.addressLine, summary.adminArea, summary.albumName, summary.relativePath);
        if (isKoreaText(evidence)) {
            return "";
        }
        String normalized = normalize(evidence);
        for (String hint : OVERSEAS_COUNTRY_HINTS) {
            if (normalized.contains(normalize(hint))) {
                return displayCountry(hint);
            }
        }
        for (String[] hint : PLACE_COUNTRY_HINTS) {
            if (normalized.contains(normalize(hint[0]))) {
                return hint[1];
            }
        }
        return "";
    }

    private static boolean isKoreaText(String text) {
        String normalized = normalize(text);
        for (String hint : KOREA_HINTS) {
            if (normalized.contains(normalize(hint))) {
                return true;
            }
        }
        return false;
    }

    private static String cleanCountry(String countryName) {
        if (countryName == null) {
            return "";
        }
        String trimmed = countryName.trim();
        return trimmed.isEmpty() ? "" : displayCountry(trimmed);
    }

    private static String displayCountry(String value) {
        String normalized = normalize(value);
        if (normalized.contains("japan")) return "일본";
        if (normalized.contains("usa") || normalized.contains("unitedstates")) return "미국";
        if (normalized.contains("thailand")) return "태국";
        if (normalized.contains("vietnam")) return "베트남";
        if (normalized.contains("france")) return "프랑스";
        if (normalized.contains("italy")) return "이탈리아";
        if (normalized.contains("spain")) return "스페인";
        if (normalized.contains("unitedkingdom") || "uk".equals(normalized)) return "영국";
        if (normalized.contains("china")) return "중국";
        if (normalized.contains("taiwan")) return "대만";
        if (normalized.contains("hongkong")) return "홍콩";
        if (normalized.contains("singapore")) return "싱가포르";
        if (normalized.contains("australia") || normalized.contains("오스트레일리아")) return "호주";
        if (normalized.contains("canada")) return "캐나다";
        if (normalized.contains("germany")) return "독일";
        if (normalized.contains("switzerland")) return "스위스";
        if (normalized.contains("austria")) return "오스트리아";
        if (normalized.contains("czech")) return "체코";
        if (normalized.contains("netherlands")) return "네덜란드";
        if (normalized.contains("guam")) return "괌";
        return value.trim();
    }

    private static String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(part);
            }
        }
        return builder.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).replaceAll("[^0-9a-z가-힣]+", "");
    }
}

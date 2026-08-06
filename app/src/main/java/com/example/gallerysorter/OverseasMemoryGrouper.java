package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class OverseasMemoryGrouper {
    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^0-9a-z가-힣\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]+");
    private static final String[] KOREA_HINTS = {
            "대한민국", "한국", "southkorea", "republicofkorea",
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충청북도", "충남", "충청남도",
            "전북", "전라북도", "전남", "전라남도",
            "경북", "경상북도", "경남", "경상남도", "제주",
            "mapogu", "songpagu", "gangnamgu", "seochogu", "jongnogu", "yongsangu"
    };

    private static final String[][] COUNTRY_ALIASES = {
            {"일본", "일본", "japan", "日本", "にほん", "ニホン"},
            {"미국", "미국", "usa", "unitedstates", "unitedstatesofamerica", "america"},
            {"태국", "태국", "thailand"},
            {"베트남", "베트남", "vietnam"},
            {"프랑스", "프랑스", "france"},
            {"이탈리아", "이탈리아", "italy"},
            {"스페인", "스페인", "spain"},
            {"영국", "영국", "unitedkingdom", "uk", "greatbritain", "england"},
            {"중국", "중국", "china"},
            {"대만", "대만", "taiwan"},
            {"홍콩", "홍콩", "hongkong"},
            {"싱가포르", "싱가포르", "singapore"},
            {"호주", "호주", "australia"},
            {"캐나다", "캐나다", "canada"},
            {"독일", "독일", "germany"},
            {"스위스", "스위스", "switzerland"},
            {"오스트리아", "오스트리아", "austria"},
            {"체코", "체코", "czech", "czechia"},
            {"네덜란드", "네덜란드", "netherlands"},
            {"괌", "괌", "guam"}
    };

    private static final String[][] PLACE_COUNTRY_HINTS = {
            {"도쿄", "일본"}, {"tokyo", "일본"}, {"東京", "일본"},
            {"오사카", "일본"}, {"osaka", "일본"}, {"大阪", "일본"},
            {"교토", "일본"}, {"kyoto", "일본"}, {"京都", "일본"},
            {"후쿠오카", "일본"}, {"fukuoka", "일본"}, {"福岡", "일본"},
            {"삿포로", "일본"}, {"sapporo", "일본"}, {"札幌", "일본"}, {"札幌市", "일본"}, {"札幌市中央区", "일본"},
            {"치토세", "일본"}, {"chitose", "일본"}, {"千歳", "일본"}, {"千歳市", "일본"},
            {"비에이", "일본"}, {"biei", "일본"}, {"美瑛", "일본"}, {"美瑛町", "일본"},
            {"후라노", "일본"}, {"furano", "일본"}, {"富良野", "일본"}, {"富良野市", "일본"},
            {"가미후라노", "일본"}, {"kamifurano", "일본"}, {"上富良野", "일본"}, {"上富良野町", "일본"},
            {"나카후라노", "일본"}, {"nakafurano", "일본"}, {"中富良野", "일본"}, {"中富良野町", "일본"},
            {"오타루", "일본"}, {"otaru", "일본"}, {"小樽", "일본"}, {"小樽市", "일본"},
            {"이와미자와", "일본"}, {"iwamizawa", "일본"}, {"岩見沢", "일본"}, {"岩見沢市", "일본"},
            {"홋카이도", "일본"}, {"hokkaido", "일본"}, {"北海道", "일본"},
            {"kurume", "일본"}, {"久留米", "일본"}, {"tosu", "일본"}, {"鳥栖", "일본"},
            {"yufu", "일본"}, {"由布", "일본"}, {"tsushima", "일본"}, {"対馬", "일본"}, {"kiyama", "일본"}, {"基山", "일본"},

            {"newyork", "미국"}, {"losangeles", "미국"}, {"sanfrancisco", "미국"}, {"sanjose", "미국"},
            {"lasvegas", "미국"}, {"seattle", "미국"}, {"honolulu", "미국"}, {"hawaii", "미국"},
            {"boston", "미국"}, {"washingtondc", "미국"},

            {"bangkok", "태국"}, {"chiangmai", "태국"}, {"phuket", "태국"},
            {"danang", "베트남"}, {"hanoi", "베트남"}, {"hochiminh", "베트남"},
            {"paris", "프랑스"}, {"nice", "프랑스"},
            {"rome", "이탈리아"}, {"milano", "이탈리아"}, {"milan", "이탈리아"}, {"venice", "이탈리아"},
            {"barcelona", "스페인"}, {"madrid", "스페인"},
            {"london", "영국"}, {"taipei", "대만"}, {"香港", "홍콩"}, {"singapore", "싱가포르"},

            {"sydney", "호주"}, {"melbourne", "호주"}, {"anglesea", "호주"}, {"belgrave", "호주"},
            {"bilinga", "호주"}, {"capewoolamai", "호주"}, {"cowes", "호주"}, {"docklands", "호주"},
            {"easternview", "호주"}, {"flinders", "호주"}, {"grantville", "호주"}, {"kennettriver", "호주"},
            {"kiama", "호주"}, {"menziescreek", "호주"}, {"peterborough", "호주"}, {"portcampbell", "호주"},
            {"princetown", "호주"}, {"sherbrooke", "호주"}, {"skenescreek", "호주"}, {"southwharf", "호주"},
            {"southbank", "호주"}, {"stkilda", "호주"}, {"stanwelltops", "호주"}, {"summerlands", "호주"},
            {"unanderra", "호주"}, {"wollongong", "호주"}, {"포트캠벨", "호주"},

            {"vancouver", "캐나다"}, {"toronto", "캐나다"},
            {"zurich", "스위스"}, {"interlaken", "스위스"}, {"vienna", "오스트리아"},
            {"prague", "체코"}, {"amsterdam", "네덜란드"},
            {"tumon", "괌"}, {"투몬", "괌"}
    };
    private static final String[] NORMALIZED_KOREA_HINTS = normalizeValues(KOREA_HINTS);
    private static final String[][] NORMALIZED_COUNTRY_ALIASES = normalizeRules(COUNTRY_ALIASES);
    private static final String[][] NORMALIZED_PLACE_COUNTRY_HINTS = normalizeRules(PLACE_COUNTRY_HINTS);

    private OverseasMemoryGrouper() {
    }

    static List<MemoryGroup> buildOverseasGroups(List<StoredAlbumSummary> summaries) {
        LinkedHashMap<String, List<MemoryItem>> grouped = new LinkedHashMap<>();
        if (summaries == null) {
            return Collections.emptyList();
        }
        for (StoredAlbumSummary summary : summaries) {
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

    private static String overseasCountryName(StoredAlbumSummary summary) {
        if (summary == null) {
            return "";
        }
        String strongPlaceEvidence = join(summary.albumName, summary.relativePath);
        String strongPlaceCountry = countryFromPlaceHint(normalize(strongPlaceEvidence));
        if (!strongPlaceCountry.isEmpty()) {
            return strongPlaceCountry;
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
        for (String[] aliasGroup : NORMALIZED_COUNTRY_ALIASES) {
            for (int i = 1; i < aliasGroup.length; i++) {
                if (containsCountryAlias(normalized, aliasGroup[i])) {
                    return aliasGroup[0];
                }
            }
        }
        return countryFromPlaceHint(normalized);
    }

    private static String countryFromPlaceHint(String normalizedEvidence) {
        if (normalizedEvidence == null || normalizedEvidence.isEmpty()) {
            return "";
        }
        for (String[] hint : NORMALIZED_PLACE_COUNTRY_HINTS) {
            if (normalizedEvidence.contains(hint[0])) {
                return hint[1];
            }
        }
        return "";
    }

    private static boolean isKoreaText(String text) {
        String normalized = normalize(text);
        if (normalized.contains("northkorea")) {
            return false;
        }
        for (String hint : NORMALIZED_KOREA_HINTS) {
            if (normalized.contains(hint)) {
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
        for (String[] aliasGroup : NORMALIZED_COUNTRY_ALIASES) {
            for (int i = 1; i < aliasGroup.length; i++) {
                if (normalized.equals(aliasGroup[i])) {
                    return aliasGroup[0];
                }
            }
        }
        return value.trim();
    }

    private static boolean containsCountryAlias(String normalizedEvidence, String alias) {
        if (alias.length() <= 2) {
            return normalizedEvidence.equals(alias);
        }
        return normalizedEvidence.contains(alias);
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
        return value == null
                ? ""
                : NORMALIZE_PATTERN.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static String[] normalizeValues(String[] values) {
        String[] normalized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = normalize(values[i]);
        }
        return normalized;
    }

    private static String[][] normalizeRules(String[][] rules) {
        String[][] normalized = new String[rules.length][];
        for (int i = 0; i < rules.length; i++) {
            normalized[i] = new String[rules[i].length];
            normalized[i][0] = normalize(rules[i][0]);
            for (int j = 1; j < rules[i].length; j++) {
                normalized[i][j] = rules[i][j];
            }
        }
        return normalized;
    }
}

package com.example.gallerysorter;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class CountryIdentityNormalizer {
    private static final Pattern MARK_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern COMPACT_PATTERN = Pattern.compile("[^0-9a-z가-힣\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]+");

    private static final String[][] COUNTRIES = {
            {"KR", "대한민국", "대한민국", "한국", "south korea", "republic of korea", "korea", "kr", "kor"},
            {"JP", "일본", "일본", "japan", "日本", "にほん", "ニホン", "jp", "jpn"},
            {"US", "미국", "미국", "usa", "u.s.a", "united states", "united states of america", "america", "us", "usa"},
            {"TH", "태국", "태국", "thailand", "th", "tha"},
            {"VN", "베트남", "베트남", "vietnam", "viet nam", "vn", "vnm"},
            {"FR", "프랑스", "프랑스", "france", "fr", "fra"},
            {"IT", "이탈리아", "이탈리아", "italy", "italia", "it", "ita"},
            {"ES", "스페인", "스페인", "spain", "es", "esp"},
            {"GB", "영국", "영국", "united kingdom", "great britain", "england", "uk", "gb", "gbr"},
            {"CN", "중국", "중국", "china", "cn", "chn"},
            {"TW", "대만", "대만", "taiwan", "tw", "twn"},
            {"HK", "홍콩", "홍콩", "hong kong", "hongkong", "hk", "hkg"},
            {"SG", "싱가포르", "싱가포르", "singapore", "sg", "sgp"},
            {"AU", "호주", "호주", "australia", "au", "aus"},
            {"CA", "캐나다", "캐나다", "canada", "ca", "can"},
            {"DE", "독일", "독일", "germany", "deutschland", "de", "deu"},
            {"CH", "스위스", "스위스", "switzerland", "ch", "che"},
            {"AT", "오스트리아", "오스트리아", "austria", "at", "aut"},
            {"CZ", "체코", "체코", "czech", "czechia", "czech republic", "cz", "cze"},
            {"NL", "네덜란드", "네덜란드", "netherlands", "holland", "nl", "nld"},
            {"GU", "괌", "괌", "guam", "gu", "gum"},
            {"TR", "튀르키예", "튀르키예", "튀르키에", "터키", "turkey", "turkiye", "türkiye", "tr", "tur"}
    };

    private CountryIdentityNormalizer() {
    }

    static String countryCode(String countryCode, String countryName) {
        String code = normalizeCountryCode(countryCode);
        if (!code.isEmpty()) {
            return code;
        }
        return countryCodeFromLegacyName(countryName);
    }

    static String countryCodeFromLegacyName(String value) {
        String normalized = normalizeCompact(value);
        if (normalized.isEmpty()) {
            return "";
        }
        for (String[] country : COUNTRIES) {
            for (String alias : countryAliases(country)) {
                if (normalized.equals(normalizeCompact(alias))) {
                    return country[0];
                }
            }
        }
        return "";
    }

    static String countryCodeFromText(String value) {
        String normalized = normalizeCompact(value);
        if (normalized.isEmpty()) {
            return "";
        }
        for (String[] country : COUNTRIES) {
            for (String alias : countryAliases(country)) {
                String normalizedAlias = normalizeCompact(alias);
                if (normalizedAlias.length() <= 2) {
                    if (normalized.equals(normalizedAlias)) {
                        return country[0];
                    }
                } else if (normalized.contains(normalizedAlias)) {
                    return country[0];
                }
            }
        }
        return "";
    }

    static String displayName(String countryCode, String fallbackCountryName) {
        String code = countryCode(countryCode, fallbackCountryName);
        String display = displayNameForCode(code);
        if (!display.isEmpty()) {
            return display;
        }
        return PhotoItemJson.clean(fallbackCountryName);
    }

    static String displayNameForCode(String countryCode) {
        String code = normalizeCountryCode(countryCode);
        if (code.isEmpty()) {
            return "";
        }
        for (String[] country : COUNTRIES) {
            if (country[0].equals(code)) {
                return country[1];
            }
        }
        return "";
    }

    static boolean isKorea(String countryCode, String countryName) {
        String code = countryCode(countryCode, countryName);
        return "KR".equals(code);
    }

    static boolean matchesSearchQuery(String countryCode, String countryName, String query) {
        String normalizedQuery = normalizeSearch(query);
        String compactQuery = normalizeCompact(query);
        if (normalizedQuery.isEmpty() && compactQuery.isEmpty()) {
            return false;
        }
        String code = countryCode(countryCode, countryName);
        if (containsNormalized(code, normalizedQuery, compactQuery)) {
            return true;
        }
        String display = displayName(code, countryName);
        if (containsNormalized(display, normalizedQuery, compactQuery)) {
            return true;
        }
        for (String alias : searchAliases(code)) {
            if (containsNormalized(alias, normalizedQuery, compactQuery)) {
                return true;
            }
        }
        return containsNormalized(countryName, normalizedQuery, compactQuery);
    }

    static List<String> searchAliases(String countryCode) {
        String code = normalizeCountryCode(countryCode);
        if (code.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> aliases = new ArrayList<>();
        for (String[] country : COUNTRIES) {
            if (country[0].equals(code)) {
                Collections.addAll(aliases, countryAliases(country));
                break;
            }
        }
        return aliases;
    }

    private static boolean containsNormalized(String value, String normalizedQuery, String compactQuery) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String normalizedValue = normalizeSearch(value);
        String compactValue = normalizeCompact(value);
        return (!normalizedQuery.isEmpty() && normalizedValue.contains(normalizedQuery))
                || (!compactQuery.isEmpty() && compactValue.contains(compactQuery));
    }

    private static String normalizeCountryCode(String value) {
        String compact = normalizeCompact(value).toUpperCase(Locale.ROOT);
        if (compact.isEmpty()) {
            return "";
        }
        if ("UK".equals(compact)) {
            return "GB";
        }
        for (String[] country : COUNTRIES) {
            if (country[0].equals(compact)) {
                return country[0];
            }
            for (int i = 2; i < country.length; i++) {
                String alias = normalizeCompact(country[i]).toUpperCase(Locale.ROOT);
                if (alias.length() == 2 || alias.length() == 3) {
                    if (compact.equals(alias)) {
                        return country[0];
                    }
                }
            }
        }
        return "";
    }

    private static String[] countryAliases(String[] country) {
        String[] aliases = new String[country.length - 1];
        System.arraycopy(country, 1, aliases, 0, aliases.length);
        return aliases;
    }

    private static String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }
        return stripMarks(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCompact(String value) {
        if (value == null) {
            return "";
        }
        return COMPACT_PATTERN.matcher(stripMarks(value).toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static String stripMarks(String value) {
        String stripped = MARK_PATTERN.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        return Normalizer.normalize(stripped, Normalizer.Form.NFC);
    }
}

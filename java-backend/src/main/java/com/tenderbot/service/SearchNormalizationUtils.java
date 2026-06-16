package com.tenderbot.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class SearchNormalizationUtils {

    private SearchNormalizationUtils() {}

    // Common unit normalizations: Russian units → standard short forms
    private static final Map<Pattern, String> UNIT_REPLACEMENTS;
    static {
        Map<Pattern, String> map = new LinkedHashMap<>();
        map.put(Pattern.compile("(\\d+)\\s*гб\\b", Pattern.CASE_INSENSITIVE), "$1GB");
        map.put(Pattern.compile("(\\d+)\\s*гиг\\b", Pattern.CASE_INSENSITIVE), "$1GB");
        map.put(Pattern.compile("(\\d+)\\s*гигабайт[а-я]*\\b", Pattern.CASE_INSENSITIVE), "$1GB");
        map.put(Pattern.compile("(\\d+)\\s*мб\\b", Pattern.CASE_INSENSITIVE), "$1MB");
        map.put(Pattern.compile("(\\d+)\\s*мегабайт[а-я]*\\b", Pattern.CASE_INSENSITIVE), "$1MB");
        map.put(Pattern.compile("(\\d+)\\s*тб\\b", Pattern.CASE_INSENSITIVE), "$1TB");
        map.put(Pattern.compile("(\\d+)\\s*терабайт[а-я]*\\b", Pattern.CASE_INSENSITIVE), "$1TB");
        map.put(Pattern.compile("(\\d+)\\s*кг\\b", Pattern.CASE_INSENSITIVE), "$1kg");
        map.put(Pattern.compile("(\\d+)\\s*г\\b", Pattern.CASE_INSENSITIVE), "$1g");
        map.put(Pattern.compile("(\\d+)\\s*мм\\b", Pattern.CASE_INSENSITIVE), "$1mm");
        map.put(Pattern.compile("(\\d+)\\s*см\\b", Pattern.CASE_INSENSITIVE), "$1cm");
        map.put(Pattern.compile("(\\d+)\\s*м\\b", Pattern.CASE_INSENSITIVE), "$1m");
        map.put(Pattern.compile("(\\d+)\\s*л\\b", Pattern.CASE_INSENSITIVE), "$1L");
        map.put(Pattern.compile("(\\d+)\\s*°?\\s*[cс]\\b", Pattern.CASE_INSENSITIVE), "$1C");
        map.put(Pattern.compile("(\\d+)\\s*°?\\s*[fф]\\b", Pattern.CASE_INSENSITIVE), "$1F");
        map.put(Pattern.compile("(\\d+)\\s*в\\b", Pattern.CASE_INSENSITIVE), "$1V");
        map.put(Pattern.compile("(\\d+)\\s*вт\\b", Pattern.CASE_INSENSITIVE), "$1W");
        map.put(Pattern.compile("(\\d+[.,]?\\d*)\\s*квт\\b", Pattern.CASE_INSENSITIVE), "$1kW");
        map.put(Pattern.compile("(\\d+)\\s*об/мин\\b", Pattern.CASE_INSENSITIVE), "$1rpm");
        map.put(Pattern.compile("(\\d+)\\s*мпа\\b", Pattern.CASE_INSENSITIVE), "$1MPa");
        map.put(Pattern.compile("(\\d+)\\s*бар\\b", Pattern.CASE_INSENSITIVE), "$1bar");
        map.put(Pattern.compile("(\\d+)\\s*па\\b", Pattern.CASE_INSENSITIVE), "$1Pa");
        map.put(Pattern.compile("(\\d+)\\s*атм\\b", Pattern.CASE_INSENSITIVE), "$1atm");
        map.put(Pattern.compile("(\\d+)\\s*дюйм[а-я]*\\b", Pattern.CASE_INSENSITIVE), "$1in");
        map.put(Pattern.compile("(\\d+)\\s*фут[а-я]*\\b", Pattern.CASE_INSENSITIVE), "$1ft");
        map.put(Pattern.compile("(\\d+)\\s*пикс\\b", Pattern.CASE_INSENSITIVE), "$1px");
        map.put(Pattern.compile("(\\d+)\\s*точ[её]к\\b", Pattern.CASE_INSENSITIVE), "$1px");
        UNIT_REPLACEMENTS = Map.copyOf(map);
    }

    private static final Pattern MULTISPACE = Pattern.compile("\\s+");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-zа-я0-9\\-]");

    public static String normalize(String input) {
        if (input == null) return "";
        String s = input.toLowerCase().trim();
        s = normalizeUnits(s);
        s = MULTISPACE.matcher(s).replaceAll(" ");
        return s;
    }

    public static String normalizeUnits(String input) {
        if (input == null) return "";
        String s = input;
        for (Map.Entry<Pattern, String> entry : UNIT_REPLACEMENTS.entrySet()) {
            s = entry.getKey().matcher(s).replaceAll(entry.getValue());
        }
        return s;
    }

    public static String toKey(String input) {
        if (input == null) return "";
        return NON_ALPHANUM.matcher(normalize(input)).replaceAll("");
    }

    public static boolean exactMatch(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    public static boolean containsIgnoreCaseNormalized(String haystack, String needle) {
        String h = normalize(haystack);
        String n = normalize(needle);
        return h.contains(n);
    }

    public static double normalizedSimilarity(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isEmpty() || nb.isEmpty()) return 0.0;
        String[] wa = na.split("\\s+");
        String[] wb = nb.split("\\s+");
        int matches = 0;
        for (String x : wa) {
            if (x.length() < 2) continue;
            for (String y : wb) {
                if (y.length() < 2) continue;
                if (x.equals(y) || x.contains(y) || y.contains(x)) {
                    matches++;
                    break;
                }
            }
        }
        return (double) matches / Math.max(wa.length, wb.length);
    }
}

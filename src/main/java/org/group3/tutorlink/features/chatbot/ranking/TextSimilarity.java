package org.group3.tutorlink.features.chatbot.ranking;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Độ giống nhau giữa hai tên (0..1), bỏ dấu và hoa thường, chịu được gõ sai nhẹ. */
public final class TextSimilarity {

    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");

    private TextSimilarity() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String text = ACCENTS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return text.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    /** @param containsScore điểm khi một tên nằm trọn (theo từ) trong tên kia */
    public static double similarity(String a, String b, double containsScore) {
        String x = normalize(a);
        String y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) {
            return 0;
        }
        if (x.equals(y)) {
            return 1;
        }
        // "toan" nằm trọn trong "toan cao cap" (theo từ) -> rất giống
        if ((" " + y + " ").contains(" " + x + " ") || (" " + x + " ").contains(" " + y + " ")) {
            return containsScore;
        }
        return Math.max(editSimilarity(x, y), tokenOverlap(x, y));
    }

    private static double editSimilarity(String x, String y) {
        int distance = levenshtein(x, y);
        return 1.0 - (double) distance / Math.max(x.length(), y.length());
    }

    // Tỉ lệ từ chung: "tieng anh giao tiep" vs "tieng anh" -> 2/4... dùng Jaccard
    private static double tokenOverlap(String x, String y) {
        java.util.Set<String> a = new java.util.HashSet<>(java.util.Arrays.asList(x.split(" ")));
        java.util.Set<String> b = new java.util.HashSet<>(java.util.Arrays.asList(y.split(" ")));
        java.util.Set<String> union = new java.util.HashSet<>(a);
        union.addAll(b);
        a.retainAll(b);
        return union.isEmpty() ? 0 : (double) a.size() / union.size();
    }

    static int levenshtein(String x, String y) {
        int[] prev = new int[y.length() + 1];
        int[] curr = new int[y.length() + 1];
        for (int j = 0; j <= y.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= x.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= y.length(); j++) {
                int cost = x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[y.length()];
    }
}

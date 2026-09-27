package com.couplestory.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class SlugUtil {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTIDASH = Pattern.compile("-{2,}");
    private static final Pattern EMOJI = Pattern.compile("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{FE00}-\\x{FE0F}\\x{1F900}-\\x{1F9FF}\\x{200D}\\x{20E3}\\x{E0020}-\\x{E007F}]");

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "";
        // Remove emoji
        String result = EMOJI.matcher(input).replaceAll("");
        // Normalize Vietnamese diacritics: à -> a, ê -> e, etc.
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = result.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        // Replace đ/Đ
        result = result.replace('đ', 'd').replace('Đ', 'D');
        // Lowercase
        result = result.toLowerCase();
        // Replace whitespace with dash
        result = WHITESPACE.matcher(result).replaceAll("-");
        // Remove non-latin chars (keep letters, digits, dash)
        result = NONLATIN.matcher(result).replaceAll("");
        // Collapse multiple dashes
        result = MULTIDASH.matcher(result).replaceAll("-");
        // Trim leading/trailing dashes
        result = result.replaceAll("^-|-$", "");
        return result;
    }
}

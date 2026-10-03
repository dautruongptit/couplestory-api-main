package com.couplestory.util;

import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Pattern;

public class SlugUtil {

    /** DNS label limit: a slug becomes the subdomain <slug>.couplestory.site. */
    public static final int MAX_LENGTH = 63;
    public static final int MIN_LENGTH = 3;

    private static final Set<String> RESERVED = Set.of(
            "main", "www", "api", "admin", "app", "mail", "cdn", "static", "assets",
            "demo", "preview", "s", "login", "register", "dashboard", "home", "editor",
            "support", "help", "blog", "couplestory");

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTIDASH = Pattern.compile("-{2,}");
    private static final Pattern VALID = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
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

    /** Given name of a Vietnamese full name: the last word ("Đậu Trường" gives "Trường"). */
    public static String givenName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] words = fullName.trim().split("\\s+");
        return words[words.length - 1];
    }

    /** Slug that is safe to use as a subdomain: [a-z0-9-] only, at most 63 characters. */
    public static String toSubdomain(String input) {
        String result = toSlug(input).replace('_', '-');
        result = MULTIDASH.matcher(result).replaceAll("-").replaceAll("^-|-$", "");
        return truncate(result, MAX_LENGTH);
    }

    /** Cuts to maxLength without leaving a trailing dash. */
    public static String truncate(String slug, int maxLength) {
        if (slug.length() <= maxLength) return slug;
        return slug.substring(0, maxLength).replaceAll("-+$", "");
    }

    public static boolean isReserved(String slug) {
        return RESERVED.contains(slug);
    }

    /** Returns a user-facing reason when the slug cannot be used, or null when it is acceptable. */
    public static String validate(String slug) {
        if (slug == null || slug.length() < MIN_LENGTH) {
            return "Link cần có ít nhất " + MIN_LENGTH + " ký tự.";
        }
        if (slug.length() > MAX_LENGTH) {
            return "Link tối đa " + MAX_LENGTH + " ký tự.";
        }
        if (!VALID.matcher(slug).matches()) {
            return "Link chỉ gồm chữ thường, số và dấu gạch ngang.";
        }
        if (isReserved(slug)) {
            return "Link này được hệ thống giữ lại, vui lòng chọn tên khác.";
        }
        return null;
    }
}

package com.couplestory.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Real address of the visitor. Every request reaches the API through Cloudflare and nginx, so
 * getRemoteAddr() is an internal address shared by everybody. Cloudflare puts the visitor in
 * CF-Connecting-IP. The headers are only as trustworthy as the network path: the API must not be
 * reachable except through the tunnel (see the deployment notes in the plan).
 *
 * Used by everything that keys on or records an address: rate limiting, API logs, audit.
 */
@Component
public class ClientIpResolver {

    private static final int MAX_LENGTH = 45; // longest IPv6 text form
    private static final Pattern IPV4 =
            Pattern.compile("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$");
    private static final Pattern IPV6_CHARACTERS = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    public String resolve(HttpServletRequest request) {
        String candidate = validOrNull(request.getHeader("CF-Connecting-IP"));
        if (candidate == null) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null) {
                candidate = validOrNull(forwarded.split(",")[0]);
            }
        }
        if (candidate != null) return candidate;

        String remote = request.getRemoteAddr();
        if (remote == null || remote.isBlank()) return "unknown";
        return remote.length() > MAX_LENGTH ? remote.substring(0, MAX_LENGTH) : remote;
    }

    private static String validOrNull(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) return null;
        if (IPV4.matcher(value).matches()) return value;
        if (IPV6_CHARACTERS.matcher(value).matches() && value.chars().filter(c -> c == ':').count() >= 2) return value;
        return null;
    }
}

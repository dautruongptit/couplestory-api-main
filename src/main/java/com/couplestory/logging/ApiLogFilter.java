package com.couplestory.logging;

import com.couplestory.security.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Records one api_logs row per API call. It runs before Spring Security so rejected requests (401/403)
 * are logged too, which is why the user comes from the "auth.userId" request attribute that
 * AuthTokenFilter leaves behind: the SecurityContext is already cleared when this filter finishes.
 *
 * Only a route template and a few columns are kept: never the query string, headers or body.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiLogFilter.class);
    private static final int MAX_ROUTE = 200;
    private static final int MAX_USER_AGENT = 255;
    private static final Pattern UUID_SEGMENT =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("(?<=/)\\d+(?=/|$)");

    private final ApiLogWriter writer;
    private final ClientIpResolver clientIpResolver;

    public ApiLogFilter(ApiLogWriter writer, ClientIpResolver clientIpResolver) {
        this.writer = writer;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/actuator/health")
                || path.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        int status = 500; // stays 500 when the chain throws
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            record(request, status, start);
        }
    }

    private void record(HttpServletRequest request, int status, long startNanos) {
        try {
            Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE);
            Object userId = request.getAttribute("auth.userId");
            writer.offer(ApiLog.builder()
                    .requestId(requestId instanceof String id ? id : UUID.randomUUID().toString())
                    .userId(userId instanceof UUID uuid ? uuid : null)
                    .method(cut(request.getMethod(), 10))
                    .route(cut(routeOf(request), MAX_ROUTE))
                    .status((short) status)
                    .durationMs((int) ((System.nanoTime() - startNanos) / 1_000_000))
                    .ip(clientIpResolver.resolve(request))
                    .userAgent(cut(request.getHeader("User-Agent"), MAX_USER_AGENT))
                    .createdAt(OffsetDateTime.now())
                    .build());
        } catch (RuntimeException e) {
            // A logging problem must never turn into a failed request.
            log.warn("Could not record api log: {}", e.toString());
        }
    }

    private static String routeOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String template) return template;
        // Nothing matched (rejected before dispatch, or an unknown path): keep the shape, drop the identifiers.
        String path = UUID_SEGMENT.matcher(request.getRequestURI()).replaceAll("{id}");
        return NUMERIC_SEGMENT.matcher(path).replaceAll("{id}");
    }

    private static String cut(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}

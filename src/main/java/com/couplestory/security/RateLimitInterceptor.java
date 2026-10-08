package com.couplestory.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/** Applies RatePolicies to the matching endpoints; everything else passes untouched. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private enum Scope { IP, USER }

    private record Rule(HttpMethod method, String pattern, RatePolicy policy, Scope scope) {}

    private static final List<Rule> RULES = List.of(
            new Rule(HttpMethod.POST, "/api/auth/register", RatePolicies.AUTH_REGISTER, Scope.IP),
            new Rule(HttpMethod.POST, "/api/stories/*/photos", RatePolicies.MEDIA_UPLOAD, Scope.USER),
            new Rule(HttpMethod.POST, "/api/orders", RatePolicies.ORDER_CREATE, Scope.USER));

    /** Paths the interceptor is registered for (WebMvcConfig); keep in step with RULES. */
    public static final String[] PATHS = {"/api/auth/register", "/api/stories/*/photos", "/api/orders"};

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final RateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public RateLimitInterceptor(RateLimiter rateLimiter, ClientIpResolver clientIpResolver) {
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        for (Rule rule : RULES) {
            if (rule.method().matches(request.getMethod()) && matcher.match(rule.pattern(), path)) {
                rateLimiter.check(rule.policy(), keyFor(rule.scope(), request));
                break;
            }
        }
        return true;
    }

    private String keyFor(Scope scope, HttpServletRequest request) {
        if (scope == Scope.USER) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl principal) {
                return "u:" + principal.getId();
            }
        }
        return "ip:" + clientIpResolver.resolve(request);
    }
}

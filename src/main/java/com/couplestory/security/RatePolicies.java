package com.couplestory.security;

import java.time.Duration;

/** The limits in force. Login is separate: LoginRateLimiter counts failed attempts only. */
public final class RatePolicies {
    private RatePolicies() {}

    public static final RatePolicy AUTH_REGISTER = new RatePolicy("AUTH_REGISTER", 5, Duration.ofHours(1));
    public static final RatePolicy MEDIA_UPLOAD = new RatePolicy("MEDIA_UPLOAD", 30, Duration.ofMinutes(10));
    public static final RatePolicy ORDER_CREATE = new RatePolicy("ORDER_CREATE", 5, Duration.ofMinutes(10));
}

package com.couplestory.security;

import java.time.Duration;

/** At most {@code maxRequests} per {@code window} for one key (a user id or a client IP). */
public record RatePolicy(String name, int maxRequests, Duration window) {}

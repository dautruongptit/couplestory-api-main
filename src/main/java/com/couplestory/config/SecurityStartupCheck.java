package com.couplestory.config;

import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Refuses to start with a weak JWT secret, or with insecure cookies outside dev and test, so a
 * misconfigured deployment fails at boot instead of quietly running unsafe. Messages never contain
 * the secret.
 */
@Component
public class SecurityStartupCheck {

    private static final int MIN_SECRET_BYTES = 32;
    private static final Set<String> INSECURE_COOKIE_ALLOWED_PROFILES = Set.of("dev", "test");

    public SecurityStartupCheck(@Value("${jwt.secret}") String jwtSecret,
                                @Value("${app.cookie-secure:true}") boolean cookieSecure,
                                Environment env) {
        validate(jwtSecret, cookieSecure, Set.of(env.getActiveProfiles()));
    }

    static void validate(String jwtSecret, boolean cookieSecure, Set<String> activeProfiles) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("jwt.secret is not set");
        }
        byte[] key;
        try {
            // Same decoder JwtUtils uses, so a secret that works today is never rejected here.
            key = Decoders.BASE64.decode(jwtSecret.trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException("jwt.secret must be Base64 encoded");
        }
        if (key.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must decode to at least " + MIN_SECRET_BYTES
                    + " bytes (it decodes to " + key.length + ")");
        }
        boolean insecureCookiesAllowed = activeProfiles.stream().anyMatch(INSECURE_COOKIE_ALLOWED_PROFILES::contains);
        if (!cookieSecure && !insecureCookiesAllowed) {
            throw new IllegalStateException(
                    "app.cookie-secure (COOKIE_SECURE) must be true outside the dev and test profiles");
        }
    }
}

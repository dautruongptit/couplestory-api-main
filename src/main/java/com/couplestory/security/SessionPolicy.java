package com.couplestory.security;

/**
 * Decides whether a JWT still belongs to the user's current login session. Every login bumps
 * users.token_version, so a token carrying an older version has been replaced by a newer login,
 * and a token without the claim cannot be revoked at all, which is why it is never accepted.
 */
final class SessionPolicy {

    enum Result { VALID, TOKEN_INVALID, SESSION_REPLACED }

    private SessionPolicy() {
    }

    static Result check(Integer tokenVersion, int currentVersion) {
        if (tokenVersion == null) return Result.TOKEN_INVALID;
        if (tokenVersion < currentVersion) return Result.SESSION_REPLACED;
        return Result.VALID;
    }
}

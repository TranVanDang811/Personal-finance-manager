package com.pfm.personalfinancemanager.security;

import java.time.Duration;
import java.time.Instant;

public interface TokenBlacklistService {

    void blacklistToken(String jti, Duration ttl);

    boolean isTokenBlacklisted(String jti);

    void blacklistUserTokensBefore(String userId, Instant timestamp, Duration ttl);

    boolean isUserTokenRevoked(String userId, Instant issuedAt);
}

package com.pfm.personalfinancemanager.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("test")
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();
    private final Map<String, Instant> userRevocationMap = new ConcurrentHashMap<>();

    @Override
    public void blacklistToken(String jti, Duration ttl) {
        if (jti == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        blacklistedTokens.put(jti, Instant.now().plus(ttl));
    }

    @Override
    public boolean isTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Instant expiry = blacklistedTokens.get(jti);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            blacklistedTokens.remove(jti);
            return false;
        }
        return true;
    }

    @Override
    public void blacklistUserTokensBefore(String userId, Instant timestamp, Duration ttl) {
        if (userId == null || timestamp == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        userRevocationMap.put(userId, timestamp);
    }

    @Override
    public boolean isUserTokenRevoked(String userId, Instant issuedAt) {
        if (userId == null || issuedAt == null) {
            return false;
        }
        Instant revokedAt = userRevocationMap.get(userId);
        return revokedAt != null && issuedAt.isBefore(revokedAt);
    }
}

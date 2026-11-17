package com.pfm.personalfinancemanager.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Profile("!test")
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "pfm:auth:blacklist:";
    private static final String USER_REVOCATION_PREFIX = "pfm:auth:user_revoked_before:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void blacklistToken(String jti, Duration ttl) {
        if (jti == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "revoked", ttl);
    }

    @Override
    public boolean isTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Boolean hasKey = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
        return Boolean.TRUE.equals(hasKey);
    }

    @Override
    public void blacklistUserTokensBefore(String userId, java.time.Instant timestamp, Duration ttl) {
        if (userId == null || timestamp == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(
                USER_REVOCATION_PREFIX + userId,
                String.valueOf(timestamp.toEpochMilli()),
                ttl
        );
    }

    @Override
    public boolean isUserTokenRevoked(String userId, java.time.Instant issuedAt) {
        if (userId == null || issuedAt == null) {
            return false;
        }
        Object rawVal = redisTemplate.opsForValue().get(USER_REVOCATION_PREFIX + userId);
        if (rawVal == null) {
            return false;
        }
        try {
            long revokedEpochMilli = Long.parseLong(rawVal.toString());
            return issuedAt.toEpochMilli() < revokedEpochMilli;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}

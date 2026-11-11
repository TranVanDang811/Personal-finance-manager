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
}

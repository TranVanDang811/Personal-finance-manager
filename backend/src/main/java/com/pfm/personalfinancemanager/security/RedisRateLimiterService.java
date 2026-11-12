package com.pfm.personalfinancemanager.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
public class RedisRateLimiterService implements RateLimiterService {

    private static final String RATE_LIMIT_KEY_PREFIX = "pfm:auth:ratelimit:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RateLimitResult consume(String key, int maxRequests, Duration window) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;
        try {
            Long current = stringRedisTemplate.opsForValue().increment(redisKey);
            if (current == null) {
                return new RateLimitResult(true, maxRequests, 0L);
            }
            if (current == 1L) {
                stringRedisTemplate.expire(redisKey, window);
            }

            if (current > maxRequests) {
                Long ttlSeconds = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
                long retryAfter = (ttlSeconds != null && ttlSeconds > 0) ? ttlSeconds : window.toSeconds();
                return new RateLimitResult(false, 0L, retryAfter);
            }
            return new RateLimitResult(true, Math.max(0L, maxRequests - current), 0L);
        } catch (Exception ex) {
            // Fail-open if Redis is temporarily unreachable so legitimate auth is not blocked
            log.warn("Rate limiter fallback (Redis unavailable for key {}): {}", key, ex.getMessage());
            return new RateLimitResult(true, maxRequests, 0L);
        }
    }
}


package com.pfm.personalfinancemanager.security;

import java.time.Duration;

/**
 * Evaluates fixed-window rate limits for sensitive endpoints.
 */
public interface RateLimiterService {

    RateLimitResult consume(String key, int maxRequests, Duration window);

    record RateLimitResult(boolean allowed, long remaining, long retryAfterSeconds) {
    }
}


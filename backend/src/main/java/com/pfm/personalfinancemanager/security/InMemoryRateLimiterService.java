package com.pfm.personalfinancemanager.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Profile("test")
public class InMemoryRateLimiterService implements RateLimiterService {

    private static final class WindowCounter {
        private final Instant windowEnd;
        private final AtomicInteger count = new AtomicInteger(0);

        private WindowCounter(Instant windowEnd) {
            this.windowEnd = windowEnd;
        }
    }

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult consume(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || !now.isBefore(existing.windowEnd)) {
                return new WindowCounter(now.plus(window));
            }
            return existing;
        });

        int current = counter.count.incrementAndGet();
        long retryAfter = Math.max(1L, Duration.between(now, counter.windowEnd).toSeconds());

        if (current > maxRequests) {
            return new RateLimitResult(false, 0L, retryAfter);
        }
        return new RateLimitResult(true, Math.max(0L, (long) maxRequests - current), 0L);
    }

    public void clear() {
        counters.clear();
    }
}


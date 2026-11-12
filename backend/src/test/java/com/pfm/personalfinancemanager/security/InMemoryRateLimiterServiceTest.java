package com.pfm.personalfinancemanager.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterServiceTest {

    @Test
    void consume_ShouldAllowWithinLimitRejectExcessAndIsolateKeys() {
        InMemoryRateLimiterService limiter = new InMemoryRateLimiterService();
        Duration window = Duration.ofSeconds(60);

        RateLimiterService.RateLimitResult r1 = limiter.consume("10.0.0.1:/api/v1/auth/login", 2, window);
        RateLimiterService.RateLimitResult r2 = limiter.consume("10.0.0.1:/api/v1/auth/login", 2, window);
        RateLimiterService.RateLimitResult r3 = limiter.consume("10.0.0.1:/api/v1/auth/login", 2, window);
        RateLimiterService.RateLimitResult otherKey = limiter.consume("10.0.0.2:/api/v1/auth/login", 2, window);

        assertThat(r1.allowed()).isTrue();
        assertThat(r1.remaining()).isEqualTo(1L);

        assertThat(r2.allowed()).isTrue();
        assertThat(r2.remaining()).isZero();

        assertThat(r3.allowed()).isFalse();
        assertThat(r3.remaining()).isZero();
        assertThat(r3.retryAfterSeconds()).isGreaterThanOrEqualTo(1L);

        assertThat(otherKey.allowed()).isTrue();
        assertThat(otherKey.remaining()).isEqualTo(1L);
    }

    @Test
    void consume_ShouldResetAfterWindowElapses() throws InterruptedException {
        InMemoryRateLimiterService limiter = new InMemoryRateLimiterService();
        Duration shortWindow = Duration.ofMillis(60);

        assertThat(limiter.consume("ip:/login", 1, shortWindow).allowed()).isTrue();
        assertThat(limiter.consume("ip:/login", 1, shortWindow).allowed()).isFalse();

        Thread.sleep(90);

        RateLimiterService.RateLimitResult afterWindow = limiter.consume("ip:/login", 1, shortWindow);
        assertThat(afterWindow.allowed()).isTrue();
        assertThat(afterWindow.remaining()).isZero();
    }
}


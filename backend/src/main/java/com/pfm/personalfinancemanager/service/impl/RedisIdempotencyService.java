package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Profile("!test")
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

    private static final String KEY_PREFIX = "pfm:idempotency:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void acquireOrThrow(String scopeKey, Duration lockTtl) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + scopeKey, "PROCESSING", lockTtl);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(
                    ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Yêu cầu trùng lặp đang được xử lý hoặc đã hoàn tất trước đó"
            );
        }
    }

    @Override
    public void markCompleted(String scopeKey, String resourceId, Duration completedTtl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + scopeKey, "COMPLETED:" + resourceId, completedTtl);
    }

    @Override
    public void release(String scopeKey) {
        redisTemplate.delete(KEY_PREFIX + scopeKey);
    }
}


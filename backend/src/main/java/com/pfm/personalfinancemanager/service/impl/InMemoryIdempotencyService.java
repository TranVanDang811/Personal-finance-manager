package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.service.IdempotencyService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("test")
public class InMemoryIdempotencyService implements IdempotencyService {

    private record Entry(String value, Instant expiresAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void acquireOrThrow(String scopeKey, Duration lockTtl) {
        Instant now = Instant.now();
        AtomicBoolean acquired = new AtomicBoolean(false);

        store.compute(scopeKey, (key, existing) -> {
            if (existing == null || existing.expiresAt().isBefore(now)) {
                acquired.set(true);
                return new Entry("PROCESSING", now.plus(lockTtl));
            }
            return existing;
        });

        if (!acquired.get()) {
            throw new BusinessException(
                    ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Yêu cầu trùng lặp đang được xử lý hoặc đã hoàn tất trước đó"
            );
        }
    }

    @Override
    public void markCompleted(String scopeKey, String resourceId, Duration completedTtl) {
        store.put(scopeKey, new Entry("COMPLETED:" + resourceId, Instant.now().plus(completedTtl)));
    }

    @Override
    public void release(String scopeKey) {
        store.remove(scopeKey);
    }
}


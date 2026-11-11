package com.pfm.personalfinancemanager.service;

import java.time.Duration;

public interface IdempotencyService {

    void acquireOrThrow(String scopeKey, Duration lockTtl);

    void markCompleted(String scopeKey, String resourceId, Duration completedTtl);

    void release(String scopeKey);
}


package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.service.ReportCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service("reportCacheService")
@RequiredArgsConstructor
public class ReportCacheServiceImpl implements ReportCacheService {

    private static final String NO_CACHE_VERSION = "default";

    private final CacheManager cacheManager;

    @Override
    public String getUserCacheVersion(String userId) {
        if (!StringUtils.hasText(userId)) {
            return NO_CACHE_VERSION;
        }
        Cache versionsCache = cacheManager.getCache(REPORT_VERSIONS_CACHE);
        if (versionsCache == null) {
            return NO_CACHE_VERSION;
        }
        String existing = versionsCache.get(userId, String.class);
        if (StringUtils.hasText(existing)) {
            return existing;
        }

        String initialVersion = UUID.randomUUID().toString();
        Cache.ValueWrapper prior = versionsCache.putIfAbsent(userId, initialVersion);
        if (prior != null && prior.get() instanceof String priorVersion && StringUtils.hasText(priorVersion)) {
            return priorVersion;
        }
        // Check again: NoOpCacheManager ignores putIfAbsent and keeps null
        String stored = versionsCache.get(userId, String.class);
        return StringUtils.hasText(stored) ? stored : NO_CACHE_VERSION;
    }

    @Override
    public void evictUserReports(String userId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        Cache versionsCache = cacheManager.getCache(REPORT_VERSIONS_CACHE);
        if (versionsCache == null) {
            return;
        }
        String nextVersion = UUID.randomUUID().toString();
        versionsCache.put(userId, nextVersion);
        log.debug("Rotated report cache version for user {} -> {}", userId, nextVersion);
    }
}


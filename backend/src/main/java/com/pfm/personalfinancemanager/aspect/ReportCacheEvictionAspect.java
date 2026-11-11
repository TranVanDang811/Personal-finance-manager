package com.pfm.personalfinancemanager.aspect;

import com.pfm.personalfinancemanager.service.ReportCacheService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
public class ReportCacheEvictionAspect {

    private final ReportCacheService reportCacheService;

    @AfterReturning("@annotation(com.pfm.personalfinancemanager.annotation.EvictUserReports)")
    public void evictAfterWrite(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof String userId) || userId.isBlank()) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    reportCacheService.evictUserReports(userId);
                }
            });
        } else {
            reportCacheService.evictUserReports(userId);
        }
    }
}


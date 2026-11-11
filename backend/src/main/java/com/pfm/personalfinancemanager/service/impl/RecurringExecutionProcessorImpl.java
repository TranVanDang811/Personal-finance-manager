package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.entity.RecurringTransaction;
import com.pfm.personalfinancemanager.enums.NotificationType;
import com.pfm.personalfinancemanager.enums.RecurringStatus;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.repository.RecurringTransactionRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.service.IdempotencyService;
import com.pfm.personalfinancemanager.service.NotificationService;
import com.pfm.personalfinancemanager.service.RecurringExecutionProcessor;
import com.pfm.personalfinancemanager.service.TransactionService;
import com.pfm.personalfinancemanager.util.RecurringDateCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringExecutionProcessorImpl implements RecurringExecutionProcessor {

    private static final Duration EXEC_LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration EXEC_COMPLETED_TTL = Duration.ofHours(24);
    private static final int MAX_NOTIFICATION_MSG_LEN = 500;

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OccurrenceExecutionResult executeSingleOccurrence(String recurringId, LocalDate expectedExecutionDate) {
        String lockKey = "recurring:exec:" + recurringId + ":" + expectedExecutionDate;
        AtomicReference<String> executionOutcomeToken = new AtomicReference<>();

        idempotencyService.acquireOrThrow(lockKey, EXEC_LOCK_TTL);
        registerLockSynchronization(lockKey, executionOutcomeToken);

        try {
            Optional<RecurringTransaction> opt = recurringTransactionRepository.findByIdForUpdate(recurringId);
            if (opt.isEmpty()) {
                return OccurrenceExecutionResult.ofSkipped();
            }

            RecurringTransaction recurring = opt.get();
            if (recurring.getStatus() != RecurringStatus.ACTIVE) {
                log.debug("Skipping recurring {} because status is {}", recurringId, recurring.getStatus());
                return OccurrenceExecutionResult.ofSkipped();
            }
            if (!expectedExecutionDate.equals(recurring.getNextExecutionDate())) {
                log.debug("Skipping recurring {}: expected date {} != nextExecutionDate {}",
                        recurringId, expectedExecutionDate, recurring.getNextExecutionDate());
                return OccurrenceExecutionResult.ofSkipped();
            }
            if (recurring.getEndDate() != null && expectedExecutionDate.isAfter(recurring.getEndDate())) {
                log.debug("Skipping recurring {}: executionDate {} exceeds endDate {}",
                        recurringId, expectedExecutionDate, recurring.getEndDate());
                return OccurrenceExecutionResult.ofSkipped();
            }

            boolean alreadyExecutedInLedger = transactionRepository.existsRecurringExecutionOnDay(
                    recurringId,
                    RecurringDateCalculator.dayStartUtc(expectedExecutionDate),
                    RecurringDateCalculator.nextDayStartUtc(expectedExecutionDate)
            );
            if (alreadyExecutedInLedger) {
                log.warn("Duplicate prevention triggered by ledger guard for recurring {} on {}",
                        recurringId, expectedExecutionDate);
                executionOutcomeToken.set("ALREADY_EXECUTED");
                markCompletedIfNoSync(lockKey, "ALREADY_EXECUTED");
                return OccurrenceExecutionResult.ofSkipped();
            }

            Instant txTimestamp = RecurringDateCalculator.scheduledExecutionTimestamp(expectedExecutionDate);
            CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
                    .accountId(recurring.getAccount().getId())
                    .toAccountId(recurring.getToAccount() != null ? recurring.getToAccount().getId() : null)
                    .categoryId(recurring.getCategory() != null ? recurring.getCategory().getId() : null)
                    .transactionType(recurring.getTransactionType())
                    .amount(recurring.getAmount())
                    .description(recurring.getDescription())
                    .note("Tạo tự động từ giao dịch định kỳ (" + recurring.getFrequency() + ")")
                    .transactionDate(txTimestamp)
                    .build();

            TransactionResponse createdTx = transactionService.executeRecurringTransaction(
                    recurring.getUser().getId(),
                    txRequest,
                    recurringId
            );

            LocalDate nextDate = RecurringDateCalculator.calculateNextExecutionDate(
                    recurring.getStartDate(),
                    recurring.getFrequency(),
                    expectedExecutionDate
            );
            recurring.setLastExecutedAt(clock.instant());
            recurring.setNextExecutionDate(nextDate);
            recurringTransactionRepository.save(recurring);

            executionOutcomeToken.set(createdTx.getId());
            notifySuccess(recurring, createdTx, expectedExecutionDate);
            markCompletedIfNoSync(lockKey, createdTx.getId());

            log.info("Executed recurring {} occurrence {} -> tx {}, nextExecutionDate={}",
                    recurringId, expectedExecutionDate, createdTx.getId(), nextDate);

            return OccurrenceExecutionResult.ofExecuted(createdTx, nextDate, recurring.getEndDate());
        } catch (RuntimeException ex) {
            releaseIfNoSync(lockKey);
            throw ex;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyExecutionFailureOnce(String recurringId, LocalDate executionDate, String reason) {
        String alertKey = "recurring:fail-alert:" + recurringId + ":" + executionDate;
        try {
            idempotencyService.acquireOrThrow(alertKey, EXEC_LOCK_TTL);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.IDEMPOTENCY_CONFLICT) {
                return;
            }
            throw ex;
        }

        try {
            Optional<RecurringTransaction> opt = recurringTransactionRepository.findByIdWithDetails(recurringId);
            if (opt.isEmpty()) {
                idempotencyService.release(alertKey);
                return;
            }
            RecurringTransaction recurring = opt.get();
            String safeReason = reason != null && !reason.isBlank() ? reason : "Lỗi nghiệp vụ khi tạo giao dịch";
            String title = "Giao dịch định kỳ thất bại: " + truncate(recurring.getDescription(), 100);
            String message = truncate(
                    String.format("Không thể tự động thực hiện giao dịch định kỳ '%s' ngày %s. Lý do: %s",
                            recurring.getDescription(), executionDate, safeReason),
                    MAX_NOTIFICATION_MSG_LEN
            );

            notificationService.createNotification(
                    recurring.getUser(),
                    title,
                    message,
                    NotificationType.SYSTEM,
                    recurring.getId(),
                    "RECURRING_TRANSACTION"
            );
            idempotencyService.markCompleted(alertKey, recurringId, EXEC_COMPLETED_TTL);
        } catch (RuntimeException ex) {
            idempotencyService.release(alertKey);
            log.warn("Failed to record failure notification for recurring {}: {}", recurringId, ex.getMessage());
        }
    }

    private void notifySuccess(RecurringTransaction recurring, TransactionResponse tx, LocalDate executionDate) {
        DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        String title = "Đã thực hiện giao dịch định kỳ: " + truncate(recurring.getDescription(), 100);
        String message = String.format(
                "Hệ thống đã tự động ghi nhận giao dịch %s số tiền %s (%s) vào ngày %s.",
                recurring.getTransactionType(),
                df.format(recurring.getAmount()),
                recurring.getFrequency(),
                executionDate
        );

        notificationService.createNotification(
                recurring.getUser(),
                title,
                truncate(message, MAX_NOTIFICATION_MSG_LEN),
                NotificationType.RECURRING_EXECUTED,
                tx.getId(),
                "TRANSACTION"
        );
    }

    private void registerLockSynchronization(String lockKey, AtomicReference<String> outcomeToken) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String token = outcomeToken.get();
                if (status == STATUS_COMMITTED && token != null) {
                    idempotencyService.markCompleted(lockKey, token, EXEC_COMPLETED_TTL);
                } else {
                    idempotencyService.release(lockKey);
                }
            }
        });
    }

    private void markCompletedIfNoSync(String lockKey, String token) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            idempotencyService.markCompleted(lockKey, token, EXEC_COMPLETED_TTL);
        }
    }

    private void releaseIfNoSync(String lockKey) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            idempotencyService.release(lockKey);
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}


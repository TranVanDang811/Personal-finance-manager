package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.entity.RecurringTransaction;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.enums.NotificationType;
import com.pfm.personalfinancemanager.enums.RecurringFrequency;
import com.pfm.personalfinancemanager.enums.RecurringStatus;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.InsufficientBalanceException;
import com.pfm.personalfinancemanager.repository.RecurringTransactionRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.service.IdempotencyService;
import com.pfm.personalfinancemanager.service.NotificationService;
import com.pfm.personalfinancemanager.service.RecurringExecutionProcessor.OccurrenceExecutionResult;
import com.pfm.personalfinancemanager.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringExecutionProcessorImplTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionService transactionService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private NotificationService notificationService;

    private Clock fixedClock;
    private RecurringExecutionProcessorImpl processor;

    private User user;
    private Account account;
    private Category foodCategory;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-11T00:05:00Z"), ZoneOffset.UTC);
        processor = new RecurringExecutionProcessorImpl(
                recurringTransactionRepository,
                transactionRepository,
                transactionService,
                idempotencyService,
                notificationService,
                fixedClock
        );

        user = User.builder().id("user-1").email("u@pfm.local").fullName("User").password("h").build();
        account = Account.builder().id("acc-1").user(user).accountName("VCB").build();
        foodCategory = Category.builder().id("cat-food").name("Food").categoryType(CategoryType.EXPENSE).build();
    }

    @Test
    void executeSingleOccurrence_WhenValidAndDue_ShouldCreateTxAdvanceNextDateNotifyAndCompleteLock() {
        LocalDate execDate = LocalDate.of(2026, 11, 11);
        RecurringTransaction recurring = RecurringTransaction.builder()
                .id("rec-1")
                .user(user)
                .account(account)
                .category(foodCategory)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("250000"))
                .description("Tiền Internet")
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(execDate)
                .nextExecutionDate(execDate)
                .status(RecurringStatus.ACTIVE)
                .build();

        TransactionResponse createdTx = TransactionResponse.builder()
                .id("tx-1")
                .amount(new BigDecimal("250000"))
                .transactionType(TransactionType.EXPENSE)
                .build();

        when(recurringTransactionRepository.findByIdForUpdate("rec-1")).thenReturn(Optional.of(recurring));
        when(transactionRepository.existsRecurringExecutionOnDay(eq("rec-1"), any(), any())).thenReturn(false);
        when(transactionService.executeRecurringTransaction(eq("user-1"), any(CreateTransactionRequest.class), eq("rec-1")))
                .thenReturn(createdTx);

        OccurrenceExecutionResult result = processor.executeSingleOccurrence("rec-1", execDate);

        assertThat(result.executed()).isTrue();
        assertThat(result.skipped()).isFalse();
        assertThat(result.nextExecutionDate()).isEqualTo(LocalDate.of(2026, 12, 11));
        assertThat(recurring.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 12, 11));
        assertThat(recurring.getLastExecutedAt()).isEqualTo(Instant.parse("2026-11-11T00:05:00Z"));

        verify(recurringTransactionRepository).save(recurring);
        verify(notificationService).createNotification(
                eq(user), any(), any(), eq(NotificationType.RECURRING_EXECUTED), eq("tx-1"), eq("TRANSACTION")
        );
        verify(idempotencyService).markCompleted(eq("recurring:exec:rec-1:2026-11-11"), eq("tx-1"), any());
    }

    @Test
    void executeSingleOccurrence_WhenAlreadyExistsInLedger_ShouldSkipWithoutCreatingDuplicate() {
        LocalDate execDate = LocalDate.of(2026, 11, 11);
        RecurringTransaction recurring = RecurringTransaction.builder()
                .id("rec-1")
                .user(user)
                .account(account)
                .category(foodCategory)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("250000"))
                .description("Tiền Internet")
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(execDate)
                .nextExecutionDate(execDate)
                .status(RecurringStatus.ACTIVE)
                .build();

        when(recurringTransactionRepository.findByIdForUpdate("rec-1")).thenReturn(Optional.of(recurring));
        when(transactionRepository.existsRecurringExecutionOnDay(eq("rec-1"), any(), any())).thenReturn(true);

        OccurrenceExecutionResult result = processor.executeSingleOccurrence("rec-1", execDate);

        assertThat(result.executed()).isFalse();
        assertThat(result.skipped()).isTrue();
        verifyNoInteractions(transactionService, notificationService);
        verify(recurringTransactionRepository, never()).save(any());
        verify(idempotencyService).markCompleted(eq("recurring:exec:rec-1:2026-11-11"), eq("ALREADY_EXECUTED"), any());
    }

    @Test
    void executeSingleOccurrence_WhenTransactionThrowsInsufficientBalance_ShouldReleaseLockAndRethrow() {
        LocalDate execDate = LocalDate.of(2026, 11, 11);
        RecurringTransaction recurring = RecurringTransaction.builder()
                .id("rec-1")
                .user(user)
                .account(account)
                .category(foodCategory)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("5000000"))
                .description("Tiền nhà")
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(execDate)
                .nextExecutionDate(execDate)
                .status(RecurringStatus.ACTIVE)
                .build();

        when(recurringTransactionRepository.findByIdForUpdate("rec-1")).thenReturn(Optional.of(recurring));
        when(transactionRepository.existsRecurringExecutionOnDay(eq("rec-1"), any(), any())).thenReturn(false);
        when(transactionService.executeRecurringTransaction(eq("user-1"), any(), eq("rec-1")))
                .thenThrow(new InsufficientBalanceException("Không đủ tiền"));

        assertThatThrownBy(() -> processor.executeSingleOccurrence("rec-1", execDate))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(idempotencyService).release("recurring:exec:rec-1:2026-11-11");
        verify(recurringTransactionRepository, never()).save(any());
    }
}


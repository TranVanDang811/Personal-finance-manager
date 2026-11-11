package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.CreateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.RecurringExecutionBatchResult;
import com.pfm.personalfinancemanager.dto.response.RecurringTransactionResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.entity.RecurringTransaction;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.enums.RecurringFrequency;
import com.pfm.personalfinancemanager.enums.RecurringStatus;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.InsufficientBalanceException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.RecurringTransactionMapper;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import com.pfm.personalfinancemanager.repository.RecurringTransactionRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.RecurringExecutionProcessor;
import com.pfm.personalfinancemanager.service.RecurringExecutionProcessor.OccurrenceExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceImplTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RecurringExecutionProcessor executionProcessor;

    private RecurringTransactionServiceImpl service;

    private User userA;
    private User userB;
    private Account cashAccountA;
    private Category foodCategory;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-11T00:05:00Z"), ZoneOffset.UTC);
        RecurringTransactionMapper mapper = Mappers.getMapper(RecurringTransactionMapper.class);
        service = new RecurringTransactionServiceImpl(
                recurringTransactionRepository,
                accountRepository,
                categoryRepository,
                transactionRepository,
                userRepository,
                mapper,
                executionProcessor,
                clock
        );

        userA = User.builder().id("user-a").email("alice@pfm.local").fullName("Alice").password("hash").build();
        userB = User.builder().id("user-b").email("bob@pfm.local").fullName("Bob").password("hash").build();

        cashAccountA = Account.builder()
                .id("acc-cash-a")
                .user(userA)
                .accountName("Ví tiền mặt")
                .accountType(AccountType.CASH)
                .status(AccountStatus.ACTIVE)
                .build();

        foodCategory = Category.builder()
                .id("cat-food")
                .name("Food & Drink")
                .categoryType(CategoryType.EXPENSE)
                .isSystem(true)
                .isDeleted(false)
                .build();

        salaryCategory = Category.builder()
                .id("cat-salary")
                .name("Salary")
                .categoryType(CategoryType.INCOME)
                .isSystem(true)
                .isDeleted(false)
                .build();
    }

    @Test
    void createRecurringTransaction_WithValidExpense_ShouldInitializeActiveAndNextExecutionEqualsStartDate() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));
        when(categoryRepository.findByIdAndIsDeletedFalse("cat-food")).thenReturn(Optional.of(foodCategory));
        when(accountRepository.findByIdAndIsDeletedFalse("acc-cash-a")).thenReturn(Optional.of(cashAccountA));
        when(recurringTransactionRepository.save(any(RecurringTransaction.class))).thenAnswer(inv -> {
            RecurringTransaction r = inv.getArgument(0);
            r.setId("rec-1");
            return r;
        });

        LocalDate start = LocalDate.of(2026, 11, 15);
        CreateRecurringTransactionRequest req = CreateRecurringTransactionRequest.builder()
                .accountId("acc-cash-a")
                .categoryId("cat-food")
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("199000"))
                .description("  Netflix Monthly  ")
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(start)
                .build();

        RecurringTransactionResponse res = service.createRecurringTransaction("user-a", req);

        assertThat(res.getId()).isEqualTo("rec-1");
        assertThat(res.getDescription()).isEqualTo("Netflix Monthly");
        assertThat(res.getStatus()).isEqualTo(RecurringStatus.ACTIVE);
        assertThat(res.getNextExecutionDate()).isEqualTo(start);
    }

    @Test
    void createRecurringTransaction_WhenEndDateBeforeStartDate_ShouldThrowBusinessException() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));

        CreateRecurringTransactionRequest req = CreateRecurringTransactionRequest.builder()
                .accountId("acc-cash-a")
                .categoryId("cat-food")
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("100000"))
                .description("Invalid range")
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 11, 20))
                .endDate(LocalDate.of(2026, 11, 10))
                .build();

        assertThatThrownBy(() -> service.createRecurringTransaction("user-a", req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ngày kết thúc");
    }

    @Test
    void createRecurringTransaction_WhenCategoryTypeMismatches_ShouldThrowCategoryTypeMismatch() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));
        when(categoryRepository.findByIdAndIsDeletedFalse("cat-salary")).thenReturn(Optional.of(salaryCategory));

        CreateRecurringTransactionRequest req = CreateRecurringTransactionRequest.builder()
                .accountId("acc-cash-a")
                .categoryId("cat-salary")
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("100000"))
                .description("Mismatch")
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 11, 11))
                .build();

        assertThatThrownBy(() -> service.createRecurringTransaction("user-a", req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_TYPE_MISMATCH);
    }

    @Test
    void resumeRecurringTransaction_WhenPausedDateInPast_ShouldAdvanceToTodayOrFutureAndActivate() {
        RecurringTransaction paused = RecurringTransaction.builder()
                .id("rec-1")
                .user(userA)
                .account(cashAccountA)
                .category(foodCategory)
                .transactionType(TransactionType.EXPENSE)
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 1, 31))
                .nextExecutionDate(LocalDate.of(2026, 9, 30))
                .status(RecurringStatus.PAUSED)
                .build();

        when(recurringTransactionRepository.findByIdForUpdate("rec-1")).thenReturn(Optional.of(paused));
        when(recurringTransactionRepository.save(any(RecurringTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        RecurringTransactionResponse res = service.resumeRecurringTransaction("user-a", "rec-1");

        // Today from fixed clock is 2026-11-11 -> Oct 31 < Nov 11 -> next is Nov 30
        assertThat(res.getStatus()).isEqualTo(RecurringStatus.ACTIVE);
        assertThat(res.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void executeNow_WhenNextDateInFutureAndAllowEarlyFalse_ShouldReject() {
        RecurringTransaction futureRule = RecurringTransaction.builder()
                .id("rec-1")
                .user(userA)
                .status(RecurringStatus.ACTIVE)
                .nextExecutionDate(LocalDate.of(2026, 11, 20))
                .build();

        when(recurringTransactionRepository.findByIdWithDetails("rec-1")).thenReturn(Optional.of(futureRule));

        assertThatThrownBy(() -> service.executeNow("user-a", "rec-1", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa đến kỳ hạn");
        verifyNoInteractions(executionProcessor);
    }

    @Test
    void processDueRecurringTransactionsForDate_ShouldCatchUpDailyAndIsolateFailures() {
        LocalDate today = LocalDate.of(2026, 11, 11);
        RecurringTransaction ruleCatchUp = RecurringTransaction.builder()
                .id("rec-daily")
                .nextExecutionDate(LocalDate.of(2026, 11, 10))
                .build();
        RecurringTransaction ruleFailing = RecurringTransaction.builder()
                .id("rec-fail")
                .nextExecutionDate(today)
                .build();

        when(recurringTransactionRepository.findDueRecurringIds(today))
                .thenReturn(List.of("rec-daily", "rec-fail"));
        when(recurringTransactionRepository.findById("rec-daily")).thenReturn(Optional.of(ruleCatchUp));
        when(recurringTransactionRepository.findById("rec-fail")).thenReturn(Optional.of(ruleFailing));

        when(executionProcessor.executeSingleOccurrence("rec-daily", LocalDate.of(2026, 11, 10)))
                .thenReturn(OccurrenceExecutionResult.ofExecuted(
                        TransactionResponse.builder().id("tx-1").build(),
                        LocalDate.of(2026, 11, 11),
                        null
                ));
        when(executionProcessor.executeSingleOccurrence("rec-daily", LocalDate.of(2026, 11, 11)))
                .thenReturn(OccurrenceExecutionResult.ofExecuted(
                        TransactionResponse.builder().id("tx-2").build(),
                        LocalDate.of(2026, 11, 12),
                        null
                ));
        when(executionProcessor.executeSingleOccurrence("rec-fail", today))
                .thenThrow(new InsufficientBalanceException("Không đủ tiền"));

        RecurringExecutionBatchResult batchResult = service.processDueRecurringTransactionsForDate(today);

        assertThat(batchResult.getTotalRulesScanned()).isEqualTo(2);
        assertThat(batchResult.getExecutedCount()).isEqualTo(2);
        assertThat(batchResult.getSkippedCount()).isZero();
        assertThat(batchResult.getFailedCount()).isEqualTo(1);
        verify(executionProcessor).notifyExecutionFailureOnce("rec-fail", today, "Không đủ tiền");
    }

    @Test
    void getRecurringTransactionById_WhenForeignUser_ShouldThrowUnauthorized() {
        RecurringTransaction ownedByA = RecurringTransaction.builder().id("rec-1").user(userA).build();
        when(recurringTransactionRepository.findByIdWithDetails("rec-1")).thenReturn(Optional.of(ownedByA));

        assertThatThrownBy(() -> service.getRecurringTransactionById(userB.getId(), "rec-1"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void deleteRecurringTransaction_ShouldClearTransactionReferencesBeforeDeletingRule() {
        RecurringTransaction ownedByA = RecurringTransaction.builder().id("rec-1").user(userA).build();
        when(recurringTransactionRepository.findByIdForUpdate("rec-1")).thenReturn(Optional.of(ownedByA));

        service.deleteRecurringTransaction("user-a", "rec-1");

        verify(transactionRepository).clearRecurringTransactionReference("rec-1");
        verify(recurringTransactionRepository).delete(ownedByA);
    }
}


package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.CreateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.request.SavingGoalContributionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.response.SavingGoalResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.SavingGoal;
import com.pfm.personalfinancemanager.entity.SavingGoalContribution;
import com.pfm.personalfinancemanager.entity.Transaction;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.ContributionType;
import com.pfm.personalfinancemanager.enums.NotificationType;
import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.InsufficientBalanceException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.SavingGoalContributionMapper;
import com.pfm.personalfinancemanager.mapper.SavingGoalMapper;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.SavingGoalContributionRepository;
import com.pfm.personalfinancemanager.repository.SavingGoalRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingGoalServiceImplTest {

    @Mock
    private SavingGoalRepository savingGoalRepository;
    @Mock
    private SavingGoalContributionRepository contributionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private SavingGoalServiceImpl savingGoalService;

    private User userA;
    private User userB;
    private Account cashAccountA;

    @BeforeEach
    void setUp() {
        SavingGoalMapper goalMapper = Mappers.getMapper(SavingGoalMapper.class);
        SavingGoalContributionMapper contributionMapper = Mappers.getMapper(SavingGoalContributionMapper.class);
        savingGoalService = new SavingGoalServiceImpl(
                savingGoalRepository,
                contributionRepository,
                accountRepository,
                transactionRepository,
                userRepository,
                notificationService,
                goalMapper,
                contributionMapper
        );

        userA = User.builder()
                .id("user-a")
                .email("alice@pfm.local")
                .fullName("Alice")
                .password("hash")
                .build();
        userB = User.builder()
                .id("user-b")
                .email("bob@pfm.local")
                .fullName("Bob")
                .password("hash")
                .build();

        cashAccountA = Account.builder()
                .id("acc-cash-a")
                .user(userA)
                .accountName("Ví tiền mặt")
                .accountType(AccountType.CASH)
                .initialBalance(new BigDecimal("10000000"))
                .currentBalance(new BigDecimal("10000000"))
                .status(AccountStatus.ACTIVE)
                .allowNegative(false)
                .build();
    }

    @Test
    void createSavingGoal_WithNullColor_ShouldApplyDefaultColorAndZeroProgress() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));
        when(savingGoalRepository.save(any(SavingGoal.class))).thenAnswer(inv -> {
            SavingGoal g = inv.getArgument(0);
            g.setId("goal-1");
            return g;
        });

        CreateSavingGoalRequest request = CreateSavingGoalRequest.builder()
                .name("  Quỹ khẩn cấp  ")
                .targetAmount(new BigDecimal("20000000"))
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 12, 31))
                .description("6 tháng chi tiêu")
                .color(null)
                .build();

        SavingGoalResponse response = savingGoalService.createSavingGoal("user-a", request);

        assertThat(response.getId()).isEqualTo("goal-1");
        assertThat(response.getName()).isEqualTo("Quỹ khẩn cấp");
        assertThat(response.getColor()).isEqualTo("#10B981");
        assertThat(response.getStatus()).isEqualTo(SavingGoalStatus.IN_PROGRESS);
        assertThat(response.getCurrentAmount()).isEqualByComparingTo("0");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("20000000");
        assertThat(response.getProgressPercentage()).isEqualByComparingTo("0.00");
    }

    @Test
    void createSavingGoal_WhenTargetDateBeforeStartDate_ShouldThrowBusinessException() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));

        CreateSavingGoalRequest request = CreateSavingGoalRequest.builder()
                .name("Du lịch")
                .targetAmount(new BigDecimal("5000000"))
                .startDate(LocalDate.of(2026, 6, 10))
                .targetDate(LocalDate.of(2026, 6, 1))
                .build();

        assertThatThrownBy(() -> savingGoalService.createSavingGoal("user-a", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ngày dự kiến hoàn thành");
    }

    @Test
    void createSavingGoal_WhenTargetAmountNonPositive_ShouldThrowInvalidTransactionAmount() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));

        CreateSavingGoalRequest request = CreateSavingGoalRequest.builder()
                .name("Invalid")
                .targetAmount(BigDecimal.ZERO)
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 12, 31))
                .build();

        assertThatThrownBy(() -> savingGoalService.createSavingGoal("user-a", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TRANSACTION_AMOUNT);
    }

    @Test
    void deposit_WhenReachingTarget_ShouldDebitAccountCompleteGoalCreateLinkedTxAndNotify() {
        SavingGoal goal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .name("Mua MacBook")
                .targetAmount(new BigDecimal("5000000"))
                .currentAmount(new BigDecimal("2000000"))
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 12, 31))
                .status(SavingGoalStatus.IN_PROGRESS)
                .build();

        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(goal));
        when(accountRepository.findByIdForUpdate("acc-cash-a")).thenReturn(Optional.of(cashAccountA));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(savingGoalRepository.save(any(SavingGoal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            tx.setId("tx-linked-1");
            return tx;
        });
        when(contributionRepository.save(any(SavingGoalContribution.class))).thenAnswer(inv -> inv.getArgument(0));

        SavingGoalContributionRequest req = SavingGoalContributionRequest.builder()
                .accountId("acc-cash-a")
                .amount(new BigDecimal("3000000"))
                .note("Đóng góp hoàn thành mục tiêu")
                .contributionDate(Instant.parse("2026-11-11T05:00:00Z"))
                .build();

        SavingGoalResponse res = savingGoalService.deposit("user-a", "goal-1", req);

        assertThat(cashAccountA.getCurrentBalance()).isEqualByComparingTo("7000000");
        assertThat(res.getCurrentAmount()).isEqualByComparingTo("5000000");
        assertThat(res.getRemainingAmount()).isEqualByComparingTo("0");
        assertThat(res.getProgressPercentage()).isEqualByComparingTo("100.00");
        assertThat(res.getStatus()).isEqualTo(SavingGoalStatus.COMPLETED);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction linkedTx = txCaptor.getValue();
        assertThat(linkedTx.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(linkedTx.getAccount()).isSameAs(cashAccountA);
        assertThat(linkedTx.getToAccount()).isNull();
        assertThat(linkedTx.getCategory()).isNull();
        assertThat(linkedTx.getAmount()).isEqualByComparingTo("3000000");

        ArgumentCaptor<SavingGoalContribution> contribCaptor = ArgumentCaptor.forClass(SavingGoalContribution.class);
        verify(contributionRepository).save(contribCaptor.capture());
        assertThat(contribCaptor.getValue().getContributionType()).isEqualTo(ContributionType.DEPOSIT);
        assertThat(contribCaptor.getValue().getTransaction()).isSameAs(linkedTx);

        verify(notificationService).createNotification(
                eq(userA),
                any(String.class),
                any(String.class),
                eq(NotificationType.SAVING_GOAL_REACHED),
                eq("goal-1"),
                eq("SAVING_GOAL")
        );
    }

    @Test
    void deposit_WhenInsufficientBalance_ShouldThrowInsufficientBalanceException() {
        cashAccountA.setCurrentBalance(new BigDecimal("500000"));
        SavingGoal goal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .name("Xe máy")
                .targetAmount(new BigDecimal("30000000"))
                .currentAmount(BigDecimal.ZERO)
                .status(SavingGoalStatus.IN_PROGRESS)
                .build();

        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(goal));
        when(accountRepository.findByIdForUpdate("acc-cash-a")).thenReturn(Optional.of(cashAccountA));

        SavingGoalContributionRequest req = SavingGoalContributionRequest.builder()
                .accountId("acc-cash-a")
                .amount(new BigDecimal("1000000"))
                .build();

        assertThatThrownBy(() -> savingGoalService.deposit("user-a", "goal-1", req))
                .isInstanceOf(InsufficientBalanceException.class);
        verifyNoInteractions(transactionRepository, contributionRepository, notificationService);
    }

    @Test
    void deposit_WhenGoalCancelled_ShouldThrowBusinessException() {
        SavingGoal cancelledGoal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .name("Đã hủy")
                .targetAmount(new BigDecimal("5000000"))
                .currentAmount(BigDecimal.ZERO)
                .status(SavingGoalStatus.CANCELLED)
                .build();

        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(cancelledGoal));
        when(accountRepository.findByIdForUpdate("acc-cash-a")).thenReturn(Optional.of(cashAccountA));

        SavingGoalContributionRequest req = SavingGoalContributionRequest.builder()
                .accountId("acc-cash-a")
                .amount(new BigDecimal("100000"))
                .build();

        assertThatThrownBy(() -> savingGoalService.deposit("user-a", "goal-1", req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã bị hủy");
    }

    @Test
    void deposit_WhenAccountArchived_ShouldThrowBusinessException() {
        cashAccountA.setStatus(AccountStatus.ARCHIVED);
        SavingGoal goal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .name("Mục tiêu")
                .targetAmount(new BigDecimal("5000000"))
                .currentAmount(BigDecimal.ZERO)
                .status(SavingGoalStatus.IN_PROGRESS)
                .build();

        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(goal));
        when(accountRepository.findByIdForUpdate("acc-cash-a")).thenReturn(Optional.of(cashAccountA));

        SavingGoalContributionRequest req = SavingGoalContributionRequest.builder()
                .accountId("acc-cash-a")
                .amount(new BigDecimal("100000"))
                .build();

        assertThatThrownBy(() -> savingGoalService.deposit("user-a", "goal-1", req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("lưu trữ");
    }

    @Test
    void withdraw_WhenCompletedDropsBelowTarget_ShouldRefundAccountAndRevertToInProgress() {
        SavingGoal completedGoal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .name("Mua MacBook")
                .targetAmount(new BigDecimal("5000000"))
                .currentAmount(new BigDecimal("5000000"))
                .status(SavingGoalStatus.COMPLETED)
                .build();

        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(completedGoal));
        when(accountRepository.findByIdForUpdate("acc-cash-a")).thenReturn(Optional.of(cashAccountA));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(savingGoalRepository.save(any(SavingGoal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contributionRepository.save(any(SavingGoalContribution.class))).thenAnswer(inv -> inv.getArgument(0));

        SavingGoalContributionRequest req = SavingGoalContributionRequest.builder()
                .accountId("acc-cash-a")
                .amount(new BigDecimal("1500000"))
                .note("Rút bớt chi tiêu")
                .build();

        SavingGoalResponse res = savingGoalService.withdraw("user-a", "goal-1", req);

        assertThat(cashAccountA.getCurrentBalance()).isEqualByComparingTo("11500000");
        assertThat(res.getCurrentAmount()).isEqualByComparingTo("3500000");
        assertThat(res.getRemainingAmount()).isEqualByComparingTo("1500000");
        assertThat(res.getProgressPercentage()).isEqualByComparingTo("70.00");
        assertThat(res.getStatus()).isEqualTo(SavingGoalStatus.IN_PROGRESS);

        ArgumentCaptor<SavingGoalContribution> contribCaptor = ArgumentCaptor.forClass(SavingGoalContribution.class);
        verify(contributionRepository).save(contribCaptor.capture());
        assertThat(contribCaptor.getValue().getContributionType()).isEqualTo(ContributionType.WITHDRAW);
        verifyNoInteractions(notificationService);
    }

    @Test
    void withdraw_WhenAmountExceedsCurrentAmount_ShouldThrowInsufficientBalanceException() {
        SavingGoal goal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .name("Quỹ")
                .targetAmount(new BigDecimal("5000000"))
                .currentAmount(new BigDecimal("500000"))
                .status(SavingGoalStatus.IN_PROGRESS)
                .build();

        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(goal));
        when(accountRepository.findByIdForUpdate("acc-cash-a")).thenReturn(Optional.of(cashAccountA));

        SavingGoalContributionRequest req = SavingGoalContributionRequest.builder()
                .accountId("acc-cash-a")
                .amount(new BigDecimal("600000"))
                .build();

        assertThatThrownBy(() -> savingGoalService.withdraw("user-a", "goal-1", req))
                .isInstanceOf(InsufficientBalanceException.class);
        verifyNoInteractions(transactionRepository, contributionRepository);
    }

    @Test
    void updateSavingGoal_TargetAdjusted_ShouldTransitionStatusAndNotifyWhenCompleted() {
        SavingGoal goal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .name("Quỹ du lịch")
                .targetAmount(new BigDecimal("10000000"))
                .currentAmount(new BigDecimal("6000000"))
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 12, 31))
                .color("#10B981")
                .status(SavingGoalStatus.IN_PROGRESS)
                .build();

        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(goal));
        when(savingGoalRepository.save(any(SavingGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        // Lower target <= currentAmount -> transitions IN_PROGRESS -> COMPLETED & notifies
        UpdateSavingGoalRequest lowerReq = UpdateSavingGoalRequest.builder()
                .name("Quỹ du lịch")
                .targetAmount(new BigDecimal("5000000"))
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 12, 31))
                .build();

        SavingGoalResponse completedRes = savingGoalService.updateSavingGoal("user-a", "goal-1", lowerReq);
        assertThat(completedRes.getStatus()).isEqualTo(SavingGoalStatus.COMPLETED);
        assertThat(completedRes.getRemainingAmount()).isEqualByComparingTo("0");
        verify(notificationService).createNotification(
                eq(userA), any(), any(), eq(NotificationType.SAVING_GOAL_REACHED), eq("goal-1"), eq("SAVING_GOAL")
        );

        // Raise target above currentAmount -> transitions COMPLETED -> IN_PROGRESS
        UpdateSavingGoalRequest raiseReq = UpdateSavingGoalRequest.builder()
                .name("Quỹ du lịch mở rộng")
                .targetAmount(new BigDecimal("12000000"))
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 12, 31))
                .color("#3B82F6")
                .build();

        SavingGoalResponse reopenedRes = savingGoalService.updateSavingGoal("user-a", "goal-1", raiseReq);
        assertThat(reopenedRes.getStatus()).isEqualTo(SavingGoalStatus.IN_PROGRESS);
        assertThat(reopenedRes.getColor()).isEqualTo("#3B82F6");
        assertThat(reopenedRes.getProgressPercentage()).isEqualByComparingTo("50.00");
    }

    @Test
    void updateSavingGoal_WhenCancelled_ShouldThrowBusinessException() {
        SavingGoal cancelledGoal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .name("Mục tiêu cũ")
                .status(SavingGoalStatus.CANCELLED)
                .build();
        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(cancelledGoal));

        UpdateSavingGoalRequest req = UpdateSavingGoalRequest.builder()
                .name("Cố sửa")
                .targetAmount(new BigDecimal("1000000"))
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 12, 31))
                .build();

        assertThatThrownBy(() -> savingGoalService.updateSavingGoal("user-a", "goal-1", req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã hủy");
    }

    @Test
    void deleteSavingGoal_WhenCurrentAmountPositive_ShouldThrowBusinessException() {
        SavingGoal goal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .currentAmount(new BigDecimal("100000"))
                .status(SavingGoalStatus.IN_PROGRESS)
                .build();
        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> savingGoalService.deleteSavingGoal("user-a", "goal-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rút hết tiền");
    }

    @Test
    void deleteSavingGoal_WhenCurrentAmountZero_ShouldDeleteContributionsAndGoal() {
        SavingGoal goal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .currentAmount(BigDecimal.ZERO)
                .status(SavingGoalStatus.CANCELLED)
                .build();
        when(savingGoalRepository.findByIdForUpdate("goal-1")).thenReturn(Optional.of(goal));

        savingGoalService.deleteSavingGoal("user-a", "goal-1");

        verify(contributionRepository).deleteAllBySavingGoalId("goal-1");
        verify(savingGoalRepository).delete(goal);
    }

    @Test
    void getSavingGoalById_WhenForeignUser_ShouldThrowUnauthorizedAccessException() {
        SavingGoal goal = SavingGoal.builder()
                .id("goal-1")
                .user(userA)
                .targetAmount(new BigDecimal("1000000"))
                .currentAmount(BigDecimal.ZERO)
                .build();
        when(savingGoalRepository.findByIdWithUser("goal-1")).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> savingGoalService.getSavingGoalById(userB.getId(), "goal-1"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }
}


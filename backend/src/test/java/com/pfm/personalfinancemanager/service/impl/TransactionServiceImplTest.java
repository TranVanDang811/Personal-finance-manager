package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.entity.Transaction;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.InsufficientBalanceException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.TransactionMapper;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.service.BudgetAlertService;
import com.pfm.personalfinancemanager.service.FileStorageService;
import com.pfm.personalfinancemanager.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private BudgetAlertService budgetAlertService;

    private TransactionServiceImpl transactionService;

    private User userA;
    private User userB;
    private Account cashAccount;
    private Account bankAccount;
    private Category salaryCategory;
    private Category foodCategory;

    @BeforeEach
    void setUp() {
        TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);
        transactionService = new TransactionServiceImpl(
                transactionRepository,
                accountRepository,
                categoryRepository,
                mapper,
                idempotencyService,
                fileStorageService,
                budgetAlertService
        );

        userA = User.builder().id("user-a").email("a@pfm.local").fullName("User A").password("hash").build();
        userB = User.builder().id("user-b").email("b@pfm.local").fullName("User B").password("hash").build();

        cashAccount = Account.builder()
                .id("acc-z-cash")
                .user(userA)
                .accountName("Tiền mặt")
                .accountType(AccountType.CASH)
                .currentBalance(new BigDecimal("1000000.0000"))
                .allowNegative(false)
                .status(AccountStatus.ACTIVE)
                .isDeleted(false)
                .build();

        bankAccount = Account.builder()
                .id("acc-a-bank")
                .user(userA)
                .accountName("Vietcombank")
                .accountType(AccountType.BANK_ACCOUNT)
                .currentBalance(new BigDecimal("500000.0000"))
                .allowNegative(true)
                .status(AccountStatus.ACTIVE)
                .isDeleted(false)
                .build();

        salaryCategory = Category.builder()
                .id("cat-salary")
                .name("Salary")
                .categoryType(CategoryType.INCOME)
                .isSystem(true)
                .isDeleted(false)
                .build();

        foodCategory = Category.builder()
                .id("cat-food")
                .name("Food & Drink")
                .categoryType(CategoryType.EXPENSE)
                .isSystem(true)
                .isDeleted(false)
                .build();
    }

    @Test
    void createTransaction_Income_ShouldIncreaseBalance() {
        when(categoryRepository.findByIdAndIsDeletedFalse("cat-salary")).thenReturn(Optional.of(salaryCategory));
        when(accountRepository.findByIdForUpdate("acc-z-cash")).thenReturn(Optional.of(cashAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId("tx-1");
            return t;
        });

        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId("acc-z-cash")
                .categoryId("cat-salary")
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("500000.0000"))
                .description("Thưởng dự án")
                .transactionDate(Instant.parse("2026-11-10T08:00:00Z"))
                .build();

        TransactionResponse resp = transactionService.createTransaction("user-a", req, null);

        assertThat(resp.getId()).isEqualTo("tx-1");
        assertThat(cashAccount.getCurrentBalance()).isEqualByComparingTo("1500000.0000");
        verify(accountRepository).saveAll(anyCollection());
    }

    @Test
    void createTransaction_Transfer_ShouldLockAccountsInSortedOrderAndMoveMoney() {
        when(accountRepository.findByIdForUpdate("acc-a-bank")).thenReturn(Optional.of(bankAccount));
        when(accountRepository.findByIdForUpdate("acc-z-cash")).thenReturn(Optional.of(cashAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId("tx-transfer");
            return t;
        });

        // Source is "acc-z-cash", Target is "acc-a-bank" -> sorted lock order must acquire "acc-a-bank" first!
        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId("acc-z-cash")
                .toAccountId("acc-a-bank")
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("300000.0000"))
                .description("Nạp vào ngân hàng")
                .transactionDate(Instant.parse("2026-11-10T09:00:00Z"))
                .build();

        TransactionResponse resp = transactionService.createTransaction("user-a", req, "idem-key-1");

        assertThat(resp.getId()).isEqualTo("tx-transfer");
        assertThat(cashAccount.getCurrentBalance()).isEqualByComparingTo("700000.0000");
        assertThat(bankAccount.getCurrentBalance()).isEqualByComparingTo("800000.0000");

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByIdForUpdate("acc-a-bank");
        order.verify(accountRepository).findByIdForUpdate("acc-z-cash");

        verify(idempotencyService).acquireOrThrow(eq("tx:user-a:idem-key-1"), any());
        verify(idempotencyService).markCompleted(eq("tx:user-a:idem-key-1"), eq("tx-transfer"), any());
    }

    @Test
    void createTransaction_ExceedingBalanceWhenDisallowed_ShouldThrowAndReleaseIdempotencyKey() {
        when(categoryRepository.findByIdAndIsDeletedFalse("cat-food")).thenReturn(Optional.of(foodCategory));
        when(accountRepository.findByIdForUpdate("acc-z-cash")).thenReturn(Optional.of(cashAccount));

        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId("acc-z-cash")
                .categoryId("cat-food")
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("1200000.0000"))
                .description("Chi vượt số dư")
                .transactionDate(Instant.now())
                .build();

        assertThatThrownBy(() -> transactionService.createTransaction("user-a", req, "idem-fail"))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(transactionRepository, never()).save(any());
        verify(idempotencyService).release("tx:user-a:idem-fail");
    }

    @Test
    void createTransaction_CategoryTypeMismatch_ShouldThrowBusinessException() {
        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId("acc-z-cash")
                .categoryId("cat-food")
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("100000"))
                .description("Sai loại danh mục")
                .transactionDate(Instant.now())
                .build();

        when(categoryRepository.findByIdAndIsDeletedFalse("cat-food")).thenReturn(Optional.of(foodCategory));

        assertThatThrownBy(() -> transactionService.createTransaction("user-a", req, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_TYPE_MISMATCH);
    }

    @Test
    void createTransaction_TransferToForeignAccount_ShouldThrowUnauthorized() {
        Account foreignAccount = Account.builder()
                .id("acc-foreign")
                .user(userB)
                .accountName("Ví của B")
                .accountType(AccountType.CASH)
                .currentBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .isDeleted(false)
                .build();

        when(accountRepository.findByIdForUpdate("acc-foreign")).thenReturn(Optional.of(foreignAccount));

        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId("acc-z-cash")
                .toAccountId("acc-foreign")
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("100000"))
                .description("Chuyển sang user khác")
                .transactionDate(Instant.now())
                .build();

        assertThatThrownBy(() -> transactionService.createTransaction("user-a", req, null))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void createTransaction_ZeroOrNegativeAmount_ShouldThrowInvalidAmount() {
        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId("acc-z-cash")
                .categoryId("cat-food")
                .transactionType(TransactionType.EXPENSE)
                .amount(BigDecimal.ZERO)
                .description("Số tiền 0")
                .transactionDate(Instant.now())
                .build();

        assertThatThrownBy(() -> transactionService.createTransaction("user-a", req, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TRANSACTION_AMOUNT);
    }

    @Test
    void createTransaction_SameAccountTransfer_ShouldThrowSameAccountTransfer() {
        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId("acc-z-cash")
                .toAccountId("acc-z-cash")
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("50000"))
                .description("Tự chuyển cho chính mình")
                .transactionDate(Instant.now())
                .build();

        assertThatThrownBy(() -> transactionService.createTransaction("user-a", req, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_ACCOUNT_TRANSFER);
    }

    @Test
    void updateTransaction_ChangeExpenseToTransfer_ShouldRevertOldAndApplyNewBalances() {
        // Suppose cash currently has 1,000,000 after previous EXPENSE 300,000; bank has 500,000
        Transaction existing = Transaction.builder()
                .id("tx-edit")
                .user(userA)
                .account(cashAccount)
                .category(foodCategory)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("300000.0000"))
                .description("Ăn tối")
                .transactionDate(Instant.parse("2026-11-10T10:00:00Z"))
                .build();

        when(transactionRepository.findByIdWithDetails("tx-edit")).thenReturn(Optional.of(existing));
        when(accountRepository.findByIdForUpdate("acc-a-bank")).thenReturn(Optional.of(bankAccount));
        when(accountRepository.findByIdForUpdate("acc-z-cash")).thenReturn(Optional.of(cashAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Change to TRANSFER 200,000 from bank -> cash
        UpdateTransactionRequest req = UpdateTransactionRequest.builder()
                .accountId("acc-a-bank")
                .toAccountId("acc-z-cash")
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("200000.0000"))
                .description("Chuyển sang tiền mặt")
                .transactionDate(Instant.parse("2026-11-10T11:00:00Z"))
                .build();

        TransactionResponse updated = transactionService.updateTransaction("user-a", "tx-edit", req);

        // Cash: 1,000,000 + 300,000 (revert old EXPENSE) + 200,000 (transfer in) = 1,500,000
        // Bank: 500,000 - 200,000 (transfer out) = 300,000
        assertThat(cashAccount.getCurrentBalance()).isEqualByComparingTo("1500000.0000");
        assertThat(bankAccount.getCurrentBalance()).isEqualByComparingTo("300000.0000");
        assertThat(updated.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(updated.getCategoryId()).isNull();
    }

    @Test
    void updateTransaction_ReduceIncomeWithPositiveNetBalance_ShouldSucceed() {
        // Cash currently has 200,000 (after +1,000,000 income and -800,000 expense).
        // Reducing old +1,000,000 income to +900,000 produces final +100,000 (>= 0), must NOT reject intermediate step!
        cashAccount.setCurrentBalance(new BigDecimal("200000.0000"));
        Transaction existing = Transaction.builder()
                .id("tx-income-reduce")
                .user(userA)
                .account(cashAccount)
                .category(salaryCategory)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("1000000.0000"))
                .description("Lương")
                .transactionDate(Instant.parse("2026-11-01T08:00:00Z"))
                .build();

        when(transactionRepository.findByIdWithDetails("tx-income-reduce")).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndIsDeletedFalse("cat-salary")).thenReturn(Optional.of(salaryCategory));
        when(accountRepository.findByIdForUpdate("acc-z-cash")).thenReturn(Optional.of(cashAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTransactionRequest req = UpdateTransactionRequest.builder()
                .accountId("acc-z-cash")
                .categoryId("cat-salary")
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("900000.0000"))
                .description("Lương điều chỉnh")
                .transactionDate(Instant.parse("2026-11-01T08:00:00Z"))
                .build();

        transactionService.updateTransaction("user-a", "tx-income-reduce", req);

        assertThat(cashAccount.getCurrentBalance()).isEqualByComparingTo("100000.0000");
    }

    @Test
    void deleteTransaction_Income_ShouldRevertBalanceAndDelete() {
        Transaction existing = Transaction.builder()
                .id("tx-del")
                .user(userA)
                .account(cashAccount)
                .category(salaryCategory)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("400000.0000"))
                .description("Thu nhập nhầm")
                .transactionDate(Instant.now())
                .build();

        when(transactionRepository.findByIdWithDetails("tx-del")).thenReturn(Optional.of(existing));
        when(accountRepository.findByIdForUpdate("acc-z-cash")).thenReturn(Optional.of(cashAccount));

        transactionService.deleteTransaction("user-a", "tx-del");

        assertThat(cashAccount.getCurrentBalance()).isEqualByComparingTo("600000.0000");
        verify(transactionRepository).delete(existing);
    }
}


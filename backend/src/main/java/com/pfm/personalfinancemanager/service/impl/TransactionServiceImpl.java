package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.annotation.EvictUserReports;
import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.TransactionFilterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.entity.Transaction;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.InsufficientBalanceException;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.TransactionMapper;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.spec.TransactionSpecification;
import com.pfm.personalfinancemanager.service.BudgetAlertService;
import com.pfm.personalfinancemanager.service.FileStorageService;
import com.pfm.personalfinancemanager.service.IdempotencyService;
import com.pfm.personalfinancemanager.service.TransactionService;
import com.pfm.personalfinancemanager.util.TransactionRuleSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final long MAX_ATTACHMENT_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final String ATTACHMENT_SUB_DIR = "attachments";
    private static final Duration IDEMPOTENCY_LOCK_TTL = Duration.ofMinutes(2);
    private static final Duration IDEMPOTENCY_COMPLETED_TTL = Duration.ofMinutes(15);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;
    private final IdempotencyService idempotencyService;
    private final FileStorageService fileStorageService;
    private final BudgetAlertService budgetAlertService;

    private record TransactionCommand(
            String accountId,
            String toAccountId,
            String categoryId,
            TransactionType transactionType,
            BigDecimal amount,
            String description,
            String note,
            Instant transactionDate
    ) {
    }

    @Override
    @Transactional
    @EvictUserReports
    public TransactionResponse createTransaction(String userId, CreateTransactionRequest request, String idempotencyKey) {
        return createTransactionInternal(userId, request, idempotencyKey, null);
    }

    @Override
    @Transactional
    @EvictUserReports
    public TransactionResponse executeRecurringTransaction(
            String userId,
            CreateTransactionRequest request,
            String recurringTransactionId
    ) {
        return createTransactionInternal(userId, request, null, recurringTransactionId);
    }

    private TransactionResponse createTransactionInternal(
            String userId,
            CreateTransactionRequest request,
            String idempotencyKey,
            String recurringTransactionId
    ) {
        String scopeKey = StringUtils.hasText(idempotencyKey)
                ? "tx:" + userId + ":" + idempotencyKey.trim()
                : null;
        AtomicReference<String> createdTransactionIdRef = new AtomicReference<>();

        if (scopeKey != null) {
            idempotencyService.acquireOrThrow(scopeKey, IDEMPOTENCY_LOCK_TTL);
            registerIdempotencySynchronization(scopeKey, createdTransactionIdRef);
        }

        try {
            TransactionCommand cmd = toCommand(
                    request.getAccountId(),
                    request.getToAccountId(),
                    request.getCategoryId(),
                    request.getTransactionType(),
                    request.getAmount(),
                    request.getDescription(),
                    request.getNote(),
                    request.getTransactionDate()
            );

            Category category = validateCommandAndLoadCategory(userId, cmd);
            Map<String, Account> lockedAccounts = lockAccountsInOrder(userId, cmd.accountId(), cmd.toAccountId());

            Account account = lockedAccounts.get(cmd.accountId());
            Account toAccount = cmd.toAccountId() != null ? lockedAccounts.get(cmd.toAccountId()) : null;
            TransactionRuleSupport.ensureAccountsActive(account, toAccount);

            Map<String, Account> modifiedAccounts = new LinkedHashMap<>();
            applyEffect(cmd.transactionType(), account, toAccount, cmd.amount(), false, modifiedAccounts);
            validateBalancesAndSave(modifiedAccounts);

            Transaction transaction = Transaction.builder()
                    .user(account.getUser())
                    .account(account)
                    .toAccount(toAccount)
                    .category(category)
                    .transactionType(cmd.transactionType())
                    .amount(cmd.amount())
                    .description(cmd.description())
                    .note(cmd.note())
                    .transactionDate(cmd.transactionDate())
                    .recurringTransactionId(recurringTransactionId)
                    .build();

            Transaction saved = transactionRepository.save(transaction);
            transactionRepository.flush();
            createdTransactionIdRef.set(saved.getId());

            if (saved.getTransactionType() == TransactionType.EXPENSE && saved.getCategory() != null) {
                budgetAlertService.evaluateBudgetAlertForTransaction(
                        userId, saved.getCategory().getId(), saved.getTransactionDate()
                );
            }

            if (scopeKey != null && !TransactionSynchronizationManager.isActualTransactionActive()) {
                idempotencyService.markCompleted(scopeKey, saved.getId(), IDEMPOTENCY_COMPLETED_TTL);
            }

            log.info("Created {} transaction {} (amount={}) for user {}",
                    saved.getTransactionType(), saved.getId(), saved.getAmount(), userId);
            return transactionMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            if (scopeKey != null && !TransactionSynchronizationManager.isActualTransactionActive()) {
                idempotencyService.release(scopeKey);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    @EvictUserReports
    public TransactionResponse updateTransaction(String userId, String transactionId, UpdateTransactionRequest request) {
        Transaction existing = findOwnedTransactionOrThrow(userId, transactionId);
        ensureMutableDirectly(existing);

        TransactionCommand cmd = toCommand(
                request.getAccountId(),
                request.getToAccountId(),
                request.getCategoryId(),
                request.getTransactionType(),
                request.getAmount(),
                request.getDescription(),
                request.getNote(),
                request.getTransactionDate()
        );

        Category newCategory = validateCommandAndLoadCategory(userId, cmd);

        String oldAccountId = existing.getAccount().getId();
        String oldToAccountId = existing.getToAccount() != null ? existing.getToAccount().getId() : null;

        Map<String, Account> lockedAccounts = lockAccountsInOrder(
                userId,
                oldAccountId,
                oldToAccountId,
                cmd.accountId(),
                cmd.toAccountId()
        );

        Account oldAccount = lockedAccounts.get(oldAccountId);
        Account oldToAccount = oldToAccountId != null ? lockedAccounts.get(oldToAccountId) : null;
        Account newAccount = lockedAccounts.get(cmd.accountId());
        Account newToAccount = cmd.toAccountId() != null ? lockedAccounts.get(cmd.toAccountId()) : null;

        TransactionRuleSupport.ensureAccountsActive(newAccount, newToAccount);

        Map<String, Account> modifiedAccounts = new LinkedHashMap<>();
        // 1. Revert old transaction balance impact
        applyEffect(existing.getTransactionType(), oldAccount, oldToAccount, existing.getAmount(), true, modifiedAccounts);
        // 2. Apply new transaction balance impact
        applyEffect(cmd.transactionType(), newAccount, newToAccount, cmd.amount(), false, modifiedAccounts);
        // 3. Validate final net balances on all touched accounts
        validateBalancesAndSave(modifiedAccounts);

        existing.setAccount(newAccount);
        existing.setToAccount(newToAccount);
        existing.setCategory(newCategory);
        existing.setTransactionType(cmd.transactionType());
        existing.setAmount(cmd.amount());
        existing.setDescription(cmd.description());
        existing.setNote(cmd.note());
        existing.setTransactionDate(cmd.transactionDate());

        Transaction updated = transactionRepository.save(existing);
        transactionRepository.flush();
        if (updated.getTransactionType() == TransactionType.EXPENSE && updated.getCategory() != null) {
            budgetAlertService.evaluateBudgetAlertForTransaction(
                    userId, updated.getCategory().getId(), updated.getTransactionDate()
            );
        }
        log.info("Updated transaction {} for user {}", updated.getId(), userId);
        return transactionMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @EvictUserReports
    public void deleteTransaction(String userId, String transactionId) {
        Transaction existing = findOwnedTransactionOrThrow(userId, transactionId);
        ensureMutableDirectly(existing);

        String accountId = existing.getAccount().getId();
        String toAccountId = existing.getToAccount() != null ? existing.getToAccount().getId() : null;

        Map<String, Account> lockedAccounts = lockAccountsInOrder(userId, accountId, toAccountId);
        Account account = lockedAccounts.get(accountId);
        Account toAccount = toAccountId != null ? lockedAccounts.get(toAccountId) : null;

        Map<String, Account> modifiedAccounts = new LinkedHashMap<>();
        applyEffect(existing.getTransactionType(), account, toAccount, existing.getAmount(), true, modifiedAccounts);
        validateBalancesAndSave(modifiedAccounts);

        String attachmentUrl = existing.getAttachmentUrl();
        transactionRepository.delete(existing);

        if (StringUtils.hasText(attachmentUrl)) {
            scheduleFileDeletionAfterCommit(attachmentUrl);
        }

        log.info("Deleted transaction {} with balance reversal for user {}", transactionId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(String userId, String transactionId) {
        Transaction transaction = findOwnedTransactionOrThrow(userId, transactionId);
        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(
            String userId,
            TransactionFilterRequest filter,
            Pageable pageable
    ) {
        Page<TransactionResponse> page = transactionRepository
                .findAll(TransactionSpecification.withFilter(userId, filter), pageable)
                .map(transactionMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactionsByAccount(
            String userId,
            String accountId,
            Pageable pageable
    ) {
        Account account = accountRepository.findById(accountId)
                .filter(acc -> !Boolean.TRUE.equals(acc.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền truy cập tài khoản này");
        }

        Page<TransactionResponse> page = transactionRepository
                .findAllByUserIdAndInvolvingAccountId(userId, accountId, pageable)
                .map(transactionMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    @EvictUserReports
    public TransactionResponse uploadAttachment(String userId, String transactionId, MultipartFile file) {
        Transaction transaction = findOwnedTransactionOrThrow(userId, transactionId);
        String oldAttachmentUrl = transaction.getAttachmentUrl();

        String newAttachmentUrl = fileStorageService.storeImage(
                file,
                ATTACHMENT_SUB_DIR,
                transactionId,
                MAX_ATTACHMENT_SIZE_BYTES
        );
        transaction.setAttachmentUrl(newAttachmentUrl);
        Transaction saved = transactionRepository.save(transaction);

        if (StringUtils.hasText(oldAttachmentUrl)) {
            scheduleFileDeletionAfterCommit(oldAttachmentUrl);
        }

        log.info("Uploaded attachment for transaction {} by user {}", transactionId, userId);
        return transactionMapper.toResponse(saved);
    }

    private TransactionCommand toCommand(
            String accountId,
            String toAccountId,
            String categoryId,
            TransactionType transactionType,
            BigDecimal amount,
            String description,
            String note,
            Instant transactionDate
    ) {
        return new TransactionCommand(
                accountId != null ? accountId.trim() : null,
                StringUtils.hasText(toAccountId) ? toAccountId.trim() : null,
                StringUtils.hasText(categoryId) ? categoryId.trim() : null,
                transactionType,
                amount,
                description != null ? description.trim() : null,
                StringUtils.hasText(note) ? note.trim() : null,
                transactionDate
        );
    }

    private Category validateCommandAndLoadCategory(String userId, TransactionCommand cmd) {
        return TransactionRuleSupport.validateAndLoadCategory(
                userId,
                cmd.transactionType(),
                cmd.accountId(),
                cmd.toAccountId(),
                cmd.categoryId(),
                cmd.amount(),
                categoryRepository
        );
    }

    private Map<String, Account> lockAccountsInOrder(String userId, String... accountIds) {
        Set<String> sortedDistinctIds = new TreeSet<>();
        for (String id : accountIds) {
            if (StringUtils.hasText(id)) {
                sortedDistinctIds.add(id);
            }
        }

        Map<String, Account> lockedAccounts = new LinkedHashMap<>();
        for (String id : sortedDistinctIds) {
            Account account = accountRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", id));
            if (!account.getUser().getId().equals(userId)) {
                throw new UnauthorizedAccessException("Bạn không có quyền thao tác trên tài khoản này");
            }
            lockedAccounts.put(id, account);
        }
        return lockedAccounts;
    }

    private void applyEffect(
            TransactionType type,
            Account sourceAccount,
            Account targetAccount,
            BigDecimal amount,
            boolean revert,
            Map<String, Account> modifiedAccounts
    ) {
        BigDecimal signedAmount = revert ? amount.negate() : amount;
        switch (type) {
            case INCOME -> {
                sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().add(signedAmount));
                modifiedAccounts.put(sourceAccount.getId(), sourceAccount);
            }
            case EXPENSE -> {
                sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().subtract(signedAmount));
                modifiedAccounts.put(sourceAccount.getId(), sourceAccount);
            }
            case TRANSFER -> {
                sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().subtract(signedAmount));
                targetAccount.setCurrentBalance(targetAccount.getCurrentBalance().add(signedAmount));
                modifiedAccounts.put(sourceAccount.getId(), sourceAccount);
                modifiedAccounts.put(targetAccount.getId(), targetAccount);
            }
        }
    }

    private void validateBalancesAndSave(Map<String, Account> modifiedAccounts) {
        for (Account account : modifiedAccounts.values()) {
            if (!Boolean.TRUE.equals(account.getAllowNegative())
                    && account.getCurrentBalance().compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Số dư tài khoản '%s' không đủ để thực hiện giao dịch",
                                account.getAccountName())
                );
            }
        }
        accountRepository.saveAll(modifiedAccounts.values());
    }

    private Transaction findOwnedTransactionOrThrow(String userId, String transactionId) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch", "id", transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền truy cập giao dịch này");
        }
        return transaction;
    }

    private void ensureMutableDirectly(Transaction transaction) {
        if (transaction.getTransactionType() == TransactionType.TRANSFER && transaction.getToAccount() == null) {
            throw new BusinessException(
                    "Giao dịch nội bộ gắn với mục tiêu tiết kiệm không thể sửa hoặc xóa trực tiếp"
            );
        }
    }

    private void registerIdempotencySynchronization(String scopeKey, AtomicReference<String> createdTransactionIdRef) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String txId = createdTransactionIdRef.get();
                if (status == STATUS_COMMITTED && txId != null) {
                    idempotencyService.markCompleted(scopeKey, txId, IDEMPOTENCY_COMPLETED_TTL);
                } else {
                    idempotencyService.release(scopeKey);
                }
            }
        });
    }

    private void scheduleFileDeletionAfterCommit(String publicUrl) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStorageService.deleteByPublicUrl(publicUrl);
                }
            });
        } else {
            fileStorageService.deleteByPublicUrl(publicUrl);
        }
    }
}


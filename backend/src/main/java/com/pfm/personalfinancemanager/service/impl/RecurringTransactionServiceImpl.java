package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.CreateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.RecurringExecutionBatchResult;
import com.pfm.personalfinancemanager.dto.response.RecurringTransactionResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.entity.RecurringTransaction;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.RecurringFrequency;
import com.pfm.personalfinancemanager.enums.RecurringStatus;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.RecurringTransactionMapper;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import com.pfm.personalfinancemanager.repository.RecurringTransactionRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.RecurringExecutionProcessor;
import com.pfm.personalfinancemanager.service.RecurringExecutionProcessor.OccurrenceExecutionResult;
import com.pfm.personalfinancemanager.service.RecurringTransactionService;
import com.pfm.personalfinancemanager.util.RecurringDateCalculator;
import com.pfm.personalfinancemanager.util.TransactionRuleSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private static final int MAX_CATCH_UP_STEPS_PER_RULE = 366;

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final RecurringTransactionMapper recurringMapper;
    private final RecurringExecutionProcessor executionProcessor;
    private final Clock clock;

    private record RuleCommand(
            String accountId,
            String toAccountId,
            String categoryId,
            TransactionType transactionType,
            BigDecimal amount,
            String description,
            RecurringFrequency frequency,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    private record ValidatedReferences(
            Account account,
            Account toAccount,
            Category category
    ) {
    }

    @Override
    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(
            String userId,
            CreateRecurringTransactionRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        RuleCommand cmd = toRuleCommand(
                request.getAccountId(),
                request.getToAccountId(),
                request.getCategoryId(),
                request.getTransactionType(),
                request.getAmount(),
                request.getDescription(),
                request.getFrequency(),
                request.getStartDate(),
                request.getEndDate()
        );

        ValidatedReferences refs = validateRuleAndLoadReferences(userId, cmd);

        RecurringTransaction entity = RecurringTransaction.builder()
                .user(user)
                .account(refs.account())
                .toAccount(refs.toAccount())
                .category(refs.category())
                .transactionType(cmd.transactionType())
                .amount(cmd.amount())
                .description(cmd.description())
                .frequency(cmd.frequency())
                .startDate(cmd.startDate())
                .endDate(cmd.endDate())
                .nextExecutionDate(cmd.startDate())
                .status(RecurringStatus.ACTIVE)
                .build();

        RecurringTransaction saved = recurringTransactionRepository.save(entity);
        log.info("Created recurring transaction {} ({}, {}) for user {}",
                saved.getId(), saved.getTransactionType(), saved.getFrequency(), userId);
        return recurringMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(
            String userId,
            String id,
            UpdateRecurringTransactionRequest request
    ) {
        RecurringTransaction existing = findOwnedRecurringOrThrow(userId, id, true);
        if (existing.getStatus() == RecurringStatus.CANCELLED) {
            throw new BusinessException("Không thể chỉnh sửa giao dịch định kỳ đã hủy");
        }

        RuleCommand cmd = toRuleCommand(
                request.getAccountId(),
                request.getToAccountId(),
                request.getCategoryId(),
                request.getTransactionType(),
                request.getAmount(),
                request.getDescription(),
                request.getFrequency(),
                request.getStartDate(),
                request.getEndDate()
        );

        ValidatedReferences refs = validateRuleAndLoadReferences(userId, cmd);

        existing.setAccount(refs.account());
        existing.setToAccount(refs.toAccount());
        existing.setCategory(refs.category());
        existing.setTransactionType(cmd.transactionType());
        existing.setAmount(cmd.amount());
        existing.setDescription(cmd.description());
        existing.setFrequency(cmd.frequency());
        existing.setStartDate(cmd.startDate());
        existing.setEndDate(cmd.endDate());

        if (existing.getLastExecutedAt() == null || existing.getNextExecutionDate().isBefore(cmd.startDate())) {
            existing.setNextExecutionDate(cmd.startDate());
        }

        RecurringTransaction saved = recurringTransactionRepository.save(existing);
        log.info("Updated recurring transaction {} for user {}", id, userId);
        return recurringMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse pauseRecurringTransaction(String userId, String id) {
        RecurringTransaction existing = findOwnedRecurringOrThrow(userId, id, true);
        if (existing.getStatus() != RecurringStatus.ACTIVE) {
            throw new BusinessException("Chỉ có thể tạm dừng giao dịch định kỳ đang hoạt động");
        }
        existing.setStatus(RecurringStatus.PAUSED);
        RecurringTransaction saved = recurringTransactionRepository.save(existing);
        log.info("Paused recurring transaction {} for user {}", id, userId);
        return recurringMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse resumeRecurringTransaction(String userId, String id) {
        RecurringTransaction existing = findOwnedRecurringOrThrow(userId, id, true);
        if (existing.getStatus() != RecurringStatus.PAUSED) {
            throw new BusinessException("Chỉ có thể kích hoạt lại giao dịch định kỳ đang tạm dừng");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate nextDate = existing.getNextExecutionDate();
        if (nextDate.isBefore(today)) {
            nextDate = RecurringDateCalculator.advanceToOnOrAfter(
                    existing.getStartDate(),
                    existing.getFrequency(),
                    nextDate,
                    today
            );
        }
        if (existing.getEndDate() != null && nextDate.isAfter(existing.getEndDate())) {
            throw new BusinessException("Giao dịch định kỳ đã quá ngày kết thúc, không thể tiếp tục");
        }

        existing.setNextExecutionDate(nextDate);
        existing.setStatus(RecurringStatus.ACTIVE);
        RecurringTransaction saved = recurringTransactionRepository.save(existing);
        log.info("Resumed recurring transaction {} with nextExecutionDate={} for user {}", id, nextDate, userId);
        return recurringMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse cancelRecurringTransaction(String userId, String id) {
        RecurringTransaction existing = findOwnedRecurringOrThrow(userId, id, true);
        if (existing.getStatus() == RecurringStatus.CANCELLED) {
            throw new BusinessException("Giao dịch định kỳ đã ở trạng thái hủy");
        }
        existing.setStatus(RecurringStatus.CANCELLED);
        RecurringTransaction saved = recurringTransactionRepository.save(existing);
        log.info("Cancelled recurring transaction {} for user {}", id, userId);
        return recurringMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRecurringTransaction(String userId, String id) {
        RecurringTransaction existing = findOwnedRecurringOrThrow(userId, id, true);
        transactionRepository.clearRecurringTransactionReference(id);
        recurringTransactionRepository.delete(existing);
        log.info("Deleted recurring transaction {} for user {}", id, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransactionById(String userId, String id) {
        RecurringTransaction recurring = findOwnedRecurringOrThrow(userId, id, false);
        return recurringMapper.toResponse(recurring);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getRecurringTransactions(String userId, RecurringStatus status) {
        return recurringTransactionRepository.findAllByUserIdAndStatus(userId, status)
                .stream()
                .map(recurringMapper::toResponse)
                .toList();
    }

    @Override
    public TransactionResponse executeNow(String userId, String id, boolean allowEarly) {
        RecurringTransaction recurring = findOwnedRecurringOrThrow(userId, id, false);
        if (recurring.getStatus() != RecurringStatus.ACTIVE) {
            throw new BusinessException("Chỉ có thể thực thi giao dịch định kỳ ở trạng thái ACTIVE");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate nextDate = recurring.getNextExecutionDate();
        if (recurring.getEndDate() != null && nextDate.isAfter(recurring.getEndDate())) {
            throw new BusinessException("Giao dịch định kỳ đã kết thúc toàn bộ chu kỳ");
        }
        if (!allowEarly && nextDate.isAfter(today)) {
            throw new BusinessException(
                    String.format("Giao dịch định kỳ chưa đến kỳ hạn tiếp theo (%s)", nextDate)
            );
        }

        OccurrenceExecutionResult result = executionProcessor.executeSingleOccurrence(id, nextDate);
        if (result.skipped() || result.transaction() == null) {
            throw new BusinessException(
                    ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Kỳ giao dịch định kỳ này đã được xử lý hoặc trạng thái đã thay đổi"
            );
        }
        return result.transaction();
    }

    @Override
    public RecurringExecutionBatchResult processDueRecurringTransactions() {
        return processDueRecurringTransactionsForDate(LocalDate.now(clock));
    }

    @Override
    public RecurringExecutionBatchResult processDueRecurringTransactionsForDate(LocalDate executionDate) {
        List<String> dueIds = recurringTransactionRepository.findDueRecurringIds(executionDate);
        int executed = 0;
        int skipped = 0;
        int failed = 0;

        for (String recurringId : dueIds) {
            LocalDate nextCandidate = recurringTransactionRepository.findById(recurringId)
                    .map(RecurringTransaction::getNextExecutionDate)
                    .orElse(null);

            int steps = 0;
            while (nextCandidate != null
                    && !nextCandidate.isAfter(executionDate)
                    && steps < MAX_CATCH_UP_STEPS_PER_RULE) {
                steps++;
                try {
                    OccurrenceExecutionResult result = executionProcessor.executeSingleOccurrence(
                            recurringId, nextCandidate
                    );
                    if (result.executed()) {
                        executed++;
                        nextCandidate = result.nextExecutionDate();
                        if (result.endDate() != null && nextCandidate != null
                                && nextCandidate.isAfter(result.endDate())) {
                            break;
                        }
                    } else {
                        skipped++;
                        break;
                    }
                } catch (BusinessException ex) {
                    if (ex.getErrorCode() == ErrorCode.IDEMPOTENCY_CONFLICT) {
                        skipped++;
                    } else {
                        failed++;
                        log.warn("Recurring {} business error on {}: {}", recurringId, nextCandidate, ex.getMessage());
                        executionProcessor.notifyExecutionFailureOnce(recurringId, nextCandidate, ex.getMessage());
                    }
                    break;
                } catch (RuntimeException ex) {
                    failed++;
                    log.warn("Recurring {} failed on {}: {}", recurringId, nextCandidate, ex.getMessage());
                    executionProcessor.notifyExecutionFailureOnce(recurringId, nextCandidate, ex.getMessage());
                    break;
                }
            }
        }

        log.info("Recurring batch completed for {}: totalRules={}, executed={}, skipped={}, failed={}",
                executionDate, dueIds.size(), executed, skipped, failed);

        return RecurringExecutionBatchResult.builder()
                .executionDate(executionDate)
                .totalRulesScanned(dueIds.size())
                .executedCount(executed)
                .skippedCount(skipped)
                .failedCount(failed)
                .build();
    }

    private RecurringTransaction findOwnedRecurringOrThrow(String userId, String id, boolean forUpdate) {
        RecurringTransaction entity = (forUpdate
                ? recurringTransactionRepository.findByIdForUpdate(id)
                : recurringTransactionRepository.findByIdWithDetails(id))
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch định kỳ", "id", id));

        if (!entity.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền truy cập giao dịch định kỳ này");
        }
        return entity;
    }

    private ValidatedReferences validateRuleAndLoadReferences(String userId, RuleCommand cmd) {
        if (cmd.endDate() != null && cmd.endDate().isBefore(cmd.startDate())) {
            throw new BusinessException("Ngày kết thúc không được nhỏ hơn ngày bắt đầu");
        }

        Category category = TransactionRuleSupport.validateAndLoadCategory(
                userId,
                cmd.transactionType(),
                cmd.accountId(),
                cmd.toAccountId(),
                cmd.categoryId(),
                cmd.amount(),
                categoryRepository
        );

        Account account = TransactionRuleSupport.validateOwnedActiveAccount(
                userId, cmd.accountId(), accountRepository
        );
        Account toAccount = cmd.toAccountId() != null
                ? TransactionRuleSupport.validateOwnedActiveAccount(userId, cmd.toAccountId(), accountRepository)
                : null;

        return new ValidatedReferences(account, toAccount, category);
    }

    private RuleCommand toRuleCommand(
            String accountId,
            String toAccountId,
            String categoryId,
            TransactionType transactionType,
            BigDecimal amount,
            String description,
            RecurringFrequency frequency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new RuleCommand(
                accountId != null ? accountId.trim() : null,
                StringUtils.hasText(toAccountId) ? toAccountId.trim() : null,
                StringUtils.hasText(categoryId) ? categoryId.trim() : null,
                transactionType,
                amount,
                description != null ? description.trim() : null,
                frequency,
                startDate,
                endDate
        );
    }
}


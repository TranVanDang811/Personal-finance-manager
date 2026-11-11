package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.annotation.EvictUserReports;
import com.pfm.personalfinancemanager.dto.request.CreateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.request.SavingGoalContributionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.SavingGoalContributionResponse;
import com.pfm.personalfinancemanager.dto.response.SavingGoalResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.SavingGoal;
import com.pfm.personalfinancemanager.entity.SavingGoalContribution;
import com.pfm.personalfinancemanager.entity.Transaction;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.ContributionType;
import com.pfm.personalfinancemanager.enums.NotificationType;
import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.InsufficientBalanceException;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.SavingGoalContributionMapper;
import com.pfm.personalfinancemanager.mapper.SavingGoalMapper;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.SavingGoalContributionRepository;
import com.pfm.personalfinancemanager.repository.SavingGoalRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.NotificationService;
import com.pfm.personalfinancemanager.service.SavingGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingGoalServiceImpl implements SavingGoalService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String DEFAULT_COLOR = "#10B981";
    private static final int MAX_TX_DESC_LEN = 255;

    private final SavingGoalRepository savingGoalRepository;
    private final SavingGoalContributionRepository contributionRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SavingGoalMapper savingGoalMapper;
    private final SavingGoalContributionMapper contributionMapper;

    @Override
    @Transactional
    @EvictUserReports
    public SavingGoalResponse createSavingGoal(String userId, CreateSavingGoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        validateGoalDatesAndTarget(request.getTargetAmount(), request.getStartDate(), request.getTargetDate());

        SavingGoal goal = SavingGoal.builder()
                .user(user)
                .name(request.getName().trim())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .startDate(request.getStartDate())
                .targetDate(request.getTargetDate())
                .description(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null)
                .color(StringUtils.hasText(request.getColor()) ? request.getColor().trim() : DEFAULT_COLOR)
                .status(SavingGoalStatus.IN_PROGRESS)
                .build();

        SavingGoal saved = savingGoalRepository.save(goal);
        log.info("Created saving goal {} for user {}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    @EvictUserReports
    public SavingGoalResponse updateSavingGoal(String userId, String goalId, UpdateSavingGoalRequest request) {
        SavingGoal goal = findOwnedGoalOrThrow(userId, goalId, true);

        if (goal.getStatus() == SavingGoalStatus.CANCELLED) {
            throw new BusinessException("Không thể chỉnh sửa mục tiêu tiết kiệm đã hủy");
        }

        validateGoalDatesAndTarget(request.getTargetAmount(), request.getStartDate(), request.getTargetDate());

        SavingGoalStatus previousStatus = goal.getStatus();
        goal.setName(request.getName().trim());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setStartDate(request.getStartDate());
        goal.setTargetDate(request.getTargetDate());
        goal.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        if (StringUtils.hasText(request.getColor())) {
            goal.setColor(request.getColor().trim());
        }

        boolean justCompleted = false;
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(SavingGoalStatus.COMPLETED);
            justCompleted = previousStatus != SavingGoalStatus.COMPLETED;
        } else if (previousStatus == SavingGoalStatus.COMPLETED) {
            goal.setStatus(SavingGoalStatus.IN_PROGRESS);
        }

        SavingGoal updated = savingGoalRepository.save(goal);
        if (justCompleted) {
            notifyGoalCompleted(updated);
        }

        log.info("Updated saving goal {} for user {}", updated.getId(), userId);
        return toResponse(updated);
    }

    @Override
    @Transactional
    @EvictUserReports
    public SavingGoalResponse cancelSavingGoal(String userId, String goalId) {
        SavingGoal goal = findOwnedGoalOrThrow(userId, goalId, true);
        if (goal.getStatus() == SavingGoalStatus.CANCELLED) {
            throw new BusinessException("Mục tiêu tiết kiệm đã ở trạng thái hủy");
        }
        goal.setStatus(SavingGoalStatus.CANCELLED);
        SavingGoal saved = savingGoalRepository.save(goal);
        log.info("Cancelled saving goal {} for user {}", goalId, userId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    @EvictUserReports
    public void deleteSavingGoal(String userId, String goalId) {
        SavingGoal goal = findOwnedGoalOrThrow(userId, goalId, true);
        if (goal.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(
                    "Không thể xóa mục tiêu khi vẫn còn số tiền tích lũy. Vui lòng rút hết tiền về tài khoản trước"
            );
        }
        contributionRepository.deleteAllBySavingGoalId(goalId);
        savingGoalRepository.delete(goal);
        log.info("Deleted saving goal {} for user {}", goalId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGoalResponse getSavingGoalById(String userId, String goalId) {
        SavingGoal goal = findOwnedGoalOrThrow(userId, goalId, false);
        return toResponse(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingGoalResponse> getSavingGoals(String userId, SavingGoalStatus status) {
        return savingGoalRepository.findAllByUserIdAndStatus(userId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @EvictUserReports
    public SavingGoalResponse deposit(String userId, String goalId, SavingGoalContributionRequest request) {
        return processContribution(userId, goalId, request, ContributionType.DEPOSIT);
    }

    @Override
    @Transactional
    @EvictUserReports
    public SavingGoalResponse withdraw(String userId, String goalId, SavingGoalContributionRequest request) {
        return processContribution(userId, goalId, request, ContributionType.WITHDRAW);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SavingGoalContributionResponse> getContributions(String userId, String goalId, Pageable pageable) {
        findOwnedGoalOrThrow(userId, goalId, false);
        Page<SavingGoalContributionResponse> page = contributionRepository
                .findBySavingGoalIdAndUserId(goalId, userId, pageable)
                .map(contributionMapper::toResponse);
        return PageResponse.from(page);
    }

    private SavingGoalResponse processContribution(
            String userId,
            String goalId,
            SavingGoalContributionRequest request,
            ContributionType type
    ) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_AMOUNT, "Số tiền giao dịch phải lớn hơn 0");
        }

        SavingGoal goal = findOwnedGoalOrThrow(userId, goalId, true);

        Account account = accountRepository.findByIdForUpdate(request.getAccountId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", request.getAccountId()));
        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền thao tác trên tài khoản này");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    String.format("Tài khoản '%s' đang ở trạng thái lưu trữ, không thể thực hiện giao dịch",
                            account.getAccountName())
            );
        }

        BigDecimal amount = request.getAmount();
        boolean justCompleted = false;

        if (type == ContributionType.DEPOSIT) {
            if (goal.getStatus() == SavingGoalStatus.CANCELLED) {
                throw new BusinessException("Không thể nạp tiền vào mục tiêu đã bị hủy");
            }
            BigDecimal newAccountBalance = account.getCurrentBalance().subtract(amount);
            if (!Boolean.TRUE.equals(account.getAllowNegative())
                    && newAccountBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Số dư tài khoản '%s' không đủ để nạp vào mục tiêu tiết kiệm",
                                account.getAccountName())
                );
            }
            account.setCurrentBalance(newAccountBalance);

            BigDecimal previousAmount = goal.getCurrentAmount();
            goal.setCurrentAmount(previousAmount.add(amount));
            if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
                justCompleted = previousAmount.compareTo(goal.getTargetAmount()) < 0
                        || goal.getStatus() != SavingGoalStatus.COMPLETED;
                goal.setStatus(SavingGoalStatus.COMPLETED);
            }
        } else {
            if (amount.compareTo(goal.getCurrentAmount()) > 0) {
                throw new InsufficientBalanceException("Số dư hiện tại trong mục tiêu tiết kiệm không đủ để rút");
            }
            goal.setCurrentAmount(goal.getCurrentAmount().subtract(amount));
            account.setCurrentBalance(account.getCurrentBalance().add(amount));

            if (goal.getStatus() == SavingGoalStatus.COMPLETED
                    && goal.getCurrentAmount().compareTo(goal.getTargetAmount()) < 0) {
                goal.setStatus(SavingGoalStatus.IN_PROGRESS);
            }
        }

        accountRepository.save(account);
        SavingGoal savedGoal = savingGoalRepository.save(goal);

        Instant effectiveDate = request.getContributionDate() != null ? request.getContributionDate() : Instant.now();
        String trimmedNote = StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null;
        String txPrefix = type == ContributionType.DEPOSIT
                ? "Nạp vào mục tiêu tiết kiệm: "
                : "Rút từ mục tiêu tiết kiệm: ";
        String txDescription = truncate(txPrefix + savedGoal.getName(), MAX_TX_DESC_LEN);

        Transaction linkedTransaction = transactionRepository.save(Transaction.builder()
                .user(account.getUser())
                .account(account)
                .toAccount(null)
                .category(null)
                .transactionType(TransactionType.TRANSFER)
                .amount(amount)
                .description(txDescription)
                .note(trimmedNote)
                .transactionDate(effectiveDate)
                .build());

        contributionRepository.save(SavingGoalContribution.builder()
                .savingGoal(savedGoal)
                .user(account.getUser())
                .account(account)
                .transaction(linkedTransaction)
                .contributionType(type)
                .amount(amount)
                .note(trimmedNote)
                .contributionDate(effectiveDate)
                .build());

        if (justCompleted) {
            notifyGoalCompleted(savedGoal);
        }

        log.info("Processed saving goal {} ({} amount={}) for user {}", goalId, type, amount, userId);
        return toResponse(savedGoal);
    }

    private SavingGoal findOwnedGoalOrThrow(String userId, String goalId, boolean forUpdate) {
        SavingGoal goal = (forUpdate
                ? savingGoalRepository.findByIdForUpdate(goalId)
                : savingGoalRepository.findByIdWithUser(goalId))
                .orElseThrow(() -> new ResourceNotFoundException("Mục tiêu tiết kiệm", "id", goalId));

        if (!goal.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền truy cập mục tiêu tiết kiệm này");
        }
        return goal;
    }

    private void validateGoalDatesAndTarget(BigDecimal targetAmount, LocalDate startDate, LocalDate targetDate) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_AMOUNT, "Số tiền mục tiêu phải lớn hơn 0");
        }
        if (targetDate.isBefore(startDate)) {
            throw new BusinessException("Ngày dự kiến hoàn thành không được nhỏ hơn ngày bắt đầu");
        }
    }

    private void notifyGoalCompleted(SavingGoal goal) {
        DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        String title = String.format("Hoàn thành mục tiêu tiết kiệm: %s", goal.getName());
        String message = String.format(
                "Chúc mừng! Bạn đã đạt 100%% mục tiêu '%s' với số tiền %s / %s.",
                goal.getName(), df.format(goal.getCurrentAmount()), df.format(goal.getTargetAmount())
        );
        notificationService.createNotification(
                goal.getUser(),
                title,
                message,
                NotificationType.SAVING_GOAL_REACHED,
                goal.getId(),
                "SAVING_GOAL"
        );
    }

    private SavingGoalResponse toResponse(SavingGoal goal) {
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        BigDecimal progress = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                ? goal.getCurrentAmount().multiply(HUNDRED).divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return savingGoalMapper.toResponse(goal, remaining, progress);
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}

package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.annotation.EvictUserReports;
import com.pfm.personalfinancemanager.dto.request.CreateBudgetRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateBudgetRequest;
import com.pfm.personalfinancemanager.dto.response.BudgetResponse;
import com.pfm.personalfinancemanager.dto.response.BudgetSummaryResponse;
import com.pfm.personalfinancemanager.entity.Budget;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.BudgetAlertLevel;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.DuplicateResourceException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.BudgetMapper;
import com.pfm.personalfinancemanager.repository.BudgetRepository;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository.CategoryExpenseSum;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.BudgetAlertService;
import com.pfm.personalfinancemanager.service.BudgetService;
import com.pfm.personalfinancemanager.util.BudgetMetricsCalculator;
import com.pfm.personalfinancemanager.util.BudgetMetricsCalculator.BudgetMetrics;
import com.pfm.personalfinancemanager.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;
    private final BudgetAlertService budgetAlertService;

    @Override
    @Transactional
    @EvictUserReports
    public BudgetResponse createBudget(String userId, CreateBudgetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Category category = categoryRepository.findByIdAndIsDeletedFalse(request.getCategoryId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", request.getCategoryId()));

        if (!Boolean.TRUE.equals(category.getIsSystem())
                && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
            throw new UnauthorizedAccessException("Bạn không có quyền sử dụng danh mục này");
        }

        if (category.getCategoryType() != CategoryType.EXPENSE) {
            throw new BusinessException(
                    ErrorCode.CATEGORY_TYPE_MISMATCH,
                    "Ngân sách chỉ có thể thiết lập cho danh mục chi tiêu (EXPENSE)"
            );
        }

        if (request.getLimitAmount() == null || request.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_AMOUNT, "Hạn mức ngân sách phải lớn hơn 0");
        }

        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                userId, category.getId(), request.getMonth(), request.getYear())) {
            throw new DuplicateResourceException(
                    "Ngân sách",
                    "danh mục và kỳ",
                    String.format("%s (%02d/%d)", category.getName(), request.getMonth(), request.getYear())
            );
        }

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .month(request.getMonth())
                .year(request.getYear())
                .limitAmount(request.getLimitAmount())
                .build();

        Budget saved = budgetRepository.save(budget);
        BigDecimal spent = calculateSpentForSingleBudget(saved);
        budgetAlertService.evaluateBudgetAlert(saved, spent);

        log.info("Created budget {} for user {} category {} ({}/{})",
                saved.getId(), userId, category.getName(), saved.getMonth(), saved.getYear());
        return toBudgetResponse(saved, spent);
    }

    @Override
    @Transactional
    @EvictUserReports
    public BudgetResponse updateBudget(String userId, String budgetId, UpdateBudgetRequest request) {
        Budget budget = findOwnedBudgetOrThrow(userId, budgetId);

        if (request.getLimitAmount() == null || request.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_AMOUNT, "Hạn mức ngân sách phải lớn hơn 0");
        }

        budget.setLimitAmount(request.getLimitAmount());
        Budget updated = budgetRepository.save(budget);

        BigDecimal spent = calculateSpentForSingleBudget(updated);
        budgetAlertService.evaluateBudgetAlert(updated, spent);

        log.info("Updated budget {} limit to {} for user {}", budgetId, updated.getLimitAmount(), userId);
        return toBudgetResponse(updated, spent);
    }

    @Override
    @Transactional
    @EvictUserReports
    public void deleteBudget(String userId, String budgetId) {
        Budget budget = findOwnedBudgetOrThrow(userId, budgetId);
        budgetRepository.delete(budget);
        log.info("Deleted budget {} for user {}", budgetId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(String userId, String budgetId) {
        Budget budget = findOwnedBudgetOrThrow(userId, budgetId);
        BigDecimal spent = calculateSpentForSingleBudget(budget);
        return toBudgetResponse(budget, spent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(String userId, Integer month, Integer year) {
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException("Tháng phải nằm trong khoảng từ 1 đến 12");
        }
        if (year != null && (year < 2000 || year > 2100)) {
            throw new BusinessException("Năm không hợp lệ");
        }

        List<Budget> budgets = budgetRepository.findAllByUserIdAndPeriod(userId, month, year);
        if (budgets.isEmpty()) {
            return List.of();
        }

        Map<YearMonth, Map<String, BigDecimal>> spentByPeriodAndCategory = new HashMap<>();
        budgets.stream()
                .map(b -> YearMonth.of(b.getYear(), b.getMonth()))
                .distinct()
                .forEach(ym -> {
                    List<CategoryExpenseSum> sums = transactionRepository.sumExpensesGroupedByCategoryForPeriod(
                            userId,
                            DateTimeUtils.startOfMonthUtc(ym.getYear(), ym.getMonthValue()),
                            DateTimeUtils.startOfNextMonthUtc(ym.getYear(), ym.getMonthValue())
                    );
                    Map<String, BigDecimal> categoryMap = sums.stream()
                            .collect(Collectors.toMap(
                                    CategoryExpenseSum::getCategoryId,
                                    CategoryExpenseSum::getTotalAmount,
                                    BigDecimal::add
                            ));
                    spentByPeriodAndCategory.put(ym, categoryMap);
                });

        return budgets.stream()
                .map(budget -> {
                    YearMonth ym = YearMonth.of(budget.getYear(), budget.getMonth());
                    BigDecimal spent = spentByPeriodAndCategory
                            .getOrDefault(ym, Map.of())
                            .getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
                    return toBudgetResponse(budget, spent);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse getBudgetSummary(String userId, int month, int year) {
        List<BudgetResponse> budgets = getBudgets(userId, month, year);

        BigDecimal totalLimit = budgets.stream()
                .map(BudgetResponse::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(BudgetResponse::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = totalLimit.subtract(totalSpent);

        BigDecimal overallPercentage = totalLimit.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.multiply(HUNDRED).divide(totalLimit, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        long warningCount = budgets.stream().filter(b -> b.getAlertLevel() == BudgetAlertLevel.WARNING).count();
        long reachedCount = budgets.stream().filter(b -> b.getAlertLevel() == BudgetAlertLevel.REACHED).count();
        long exceededCount = budgets.stream().filter(b -> b.getAlertLevel() == BudgetAlertLevel.EXCEEDED).count();

        return BudgetSummaryResponse.builder()
                .month(month)
                .year(year)
                .totalLimitAmount(totalLimit)
                .totalSpentAmount(totalSpent)
                .totalRemainingAmount(totalRemaining)
                .overallUsagePercentage(overallPercentage)
                .warningCount(warningCount)
                .reachedCount(reachedCount)
                .exceededCount(exceededCount)
                .budgets(budgets)
                .build();
    }

    private Budget findOwnedBudgetOrThrow(String userId, String budgetId) {
        Budget budget = budgetRepository.findByIdWithDetails(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Ngân sách", "id", budgetId));

        if (!budget.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền truy cập ngân sách này");
        }
        return budget;
    }

    private BigDecimal calculateSpentForSingleBudget(Budget budget) {
        return transactionRepository.sumExpenseAmountByUserAndCategoryAndPeriod(
                budget.getUser().getId(),
                budget.getCategory().getId(),
                DateTimeUtils.startOfMonthUtc(budget.getYear(), budget.getMonth()),
                DateTimeUtils.startOfNextMonthUtc(budget.getYear(), budget.getMonth())
        );
    }

    private BudgetResponse toBudgetResponse(Budget budget, BigDecimal spentAmount) {
        BudgetMetrics metrics = BudgetMetricsCalculator.calculate(budget.getLimitAmount(), spentAmount);
        return budgetMapper.toResponse(
                budget,
                metrics.spentAmount(),
                metrics.remainingAmount(),
                metrics.usagePercentage(),
                metrics.alertLevel()
        );
    }
}


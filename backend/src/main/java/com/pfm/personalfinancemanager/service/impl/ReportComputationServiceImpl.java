package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.response.BudgetSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.CashflowTrendResponse;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownItem;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownResponse;
import com.pfm.personalfinancemanager.dto.response.DailyCashflowPoint;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse.BudgetOverview;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse.SavingGoalOverview;
import com.pfm.personalfinancemanager.dto.response.MonthlyCashflowPoint;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.PeriodSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.SavingGoalRepository;
import com.pfm.personalfinancemanager.repository.SavingGoalRepository.SavingGoalAggregateProjection;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository.CashflowEntryProjection;
import com.pfm.personalfinancemanager.repository.TransactionRepository.CategoryBreakdownProjection;
import com.pfm.personalfinancemanager.repository.TransactionRepository.TransactionTypeSum;
import com.pfm.personalfinancemanager.service.BudgetService;
import com.pfm.personalfinancemanager.service.ReportCacheService;
import com.pfm.personalfinancemanager.service.ReportComputationService;
import com.pfm.personalfinancemanager.service.TransactionService;
import com.pfm.personalfinancemanager.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportComputationServiceImpl implements ReportComputationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int TOP_EXPENSE_CATEGORIES_LIMIT = 5;
    private static final int RECENT_TRANSACTIONS_LIMIT = 5;

    private final AccountRepository accountRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final TransactionService transactionService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = ReportCacheService.DASHBOARD_SUMMARY_CACHE,
            key = "#userId + ':' + #cacheVersion + ':' + #year + ':' + #month"
    )
    public DashboardSummaryResponse computeDashboardSummary(
            String userId,
            String cacheVersion,
            int month,
            int year
    ) {
        BigDecimal totalActiveAccountBalance = nonNull(
                accountRepository.sumBalanceByUserIdAndStatus(userId, AccountStatus.ACTIVE)
        );
        SavingGoalOverview goalOverview = buildSavingGoalOverview(userId);
        BigDecimal totalSavedInGoals = goalOverview.getTotalCurrentAmount();
        BigDecimal netWorth = totalActiveAccountBalance.add(totalSavedInGoals);

        Instant monthStart = DateTimeUtils.startOfMonthUtc(year, month);
        Instant monthEnd = DateTimeUtils.startOfNextMonthUtc(year, month);
        Map<TransactionType, BigDecimal> currentTotals = loadTotalsByType(userId, monthStart, monthEnd);

        BigDecimal monthlyIncome = currentTotals.get(TransactionType.INCOME);
        BigDecimal monthlyExpense = currentTotals.get(TransactionType.EXPENSE);
        BigDecimal netCashflow = monthlyIncome.subtract(monthlyExpense);
        BigDecimal savingRate = calculateSavingRate(monthlyIncome, monthlyExpense);

        YearMonth prevYm = YearMonth.of(year, month).minusMonths(1);
        Map<TransactionType, BigDecimal> prevTotals = loadTotalsByType(
                userId,
                DateTimeUtils.startOfMonthUtc(prevYm.getYear(), prevYm.getMonthValue()),
                DateTimeUtils.startOfNextMonthUtc(prevYm.getYear(), prevYm.getMonthValue())
        );
        BigDecimal previousMonthIncome = prevTotals.get(TransactionType.INCOME);
        BigDecimal previousMonthExpense = prevTotals.get(TransactionType.EXPENSE);

        BudgetSummaryResponse budgetSummary = budgetService.getBudgetSummary(userId, month, year);
        BudgetOverview budgetOverview = BudgetOverview.builder()
                .totalBudgets(budgetSummary.getBudgets() != null ? budgetSummary.getBudgets().size() : 0)
                .totalLimitAmount(nonNull(budgetSummary.getTotalLimitAmount()))
                .totalSpentAmount(nonNull(budgetSummary.getTotalSpentAmount()))
                .overallUsagePercentage(nonNull(budgetSummary.getOverallUsagePercentage()))
                .warningCount(budgetSummary.getWarningCount())
                .reachedCount(budgetSummary.getReachedCount())
                .exceededCount(budgetSummary.getExceededCount())
                .build();

        List<CategoryBreakdownItem> allExpenseCategories = buildBreakdownItems(
                userId, TransactionType.EXPENSE, monthStart, monthEnd, monthlyExpense
        );
        List<CategoryBreakdownItem> topCategories = new ArrayList<>(
                allExpenseCategories.subList(0, Math.min(TOP_EXPENSE_CATEGORIES_LIMIT, allExpenseCategories.size()))
        );

        PageResponse<TransactionResponse> recentPage = transactionService.getTransactions(
                userId,
                null,
                PageRequest.of(
                        0,
                        RECENT_TRANSACTIONS_LIMIT,
                        Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"))
                )
        );
        List<TransactionResponse> recentTransactions = recentPage.getContent() != null
                ? new ArrayList<>(recentPage.getContent())
                : new ArrayList<>();

        return DashboardSummaryResponse.builder()
                .month(month)
                .year(year)
                .totalActiveAccountBalance(totalActiveAccountBalance)
                .totalSavedInGoals(totalSavedInGoals)
                .netWorth(netWorth)
                .monthlyIncome(monthlyIncome)
                .monthlyExpense(monthlyExpense)
                .netCashflow(netCashflow)
                .savingRatePercentage(savingRate)
                .previousMonthIncome(previousMonthIncome)
                .previousMonthExpense(previousMonthExpense)
                .incomeChangePercentage(calculateChangePercentage(monthlyIncome, previousMonthIncome))
                .expenseChangePercentage(calculateChangePercentage(monthlyExpense, previousMonthExpense))
                .budgetOverview(budgetOverview)
                .savingGoalOverview(goalOverview)
                .topExpenseCategories(topCategories)
                .recentTransactions(recentTransactions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = ReportCacheService.REPORT_ANALYTICS_CACHE,
            key = "'breakdown:' + #userId + ':' + #cacheVersion + ':' + #type.name() + ':' + #startDate + ':' + #endDate"
    )
    public CategoryBreakdownResponse computeCategoryBreakdown(
            String userId,
            String cacheVersion,
            TransactionType type,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<CategoryBreakdownProjection> rows = transactionRepository.sumCategoryBreakdownForPeriod(
                userId, type, startInstant, endExclusive
        );

        BigDecimal totalAmount = BigDecimal.ZERO;
        long totalTxCount = 0;
        for (CategoryBreakdownProjection row : rows) {
            totalAmount = totalAmount.add(nonNull(row.getTotalAmount()));
            totalTxCount += row.getTransactionCount() != null ? row.getTransactionCount() : 0L;
        }

        List<CategoryBreakdownItem> items = mapBreakdownProjections(rows, totalAmount);

        return CategoryBreakdownResponse.builder()
                .transactionType(type)
                .startDate(startDate)
                .endDate(endDate)
                .totalAmount(totalAmount)
                .totalTransactionCount(totalTxCount)
                .categories(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = ReportCacheService.REPORT_ANALYTICS_CACHE,
            key = "'trend:' + #userId + ':' + #cacheVersion + ':' + #endYear + ':' + #endMonth + ':' + #months"
    )
    public CashflowTrendResponse computeCashflowTrend(
            String userId,
            String cacheVersion,
            int months,
            int endMonth,
            int endYear
    ) {
        YearMonth endYm = YearMonth.of(endYear, endMonth);
        YearMonth startYm = endYm.minusMonths(months - 1L);

        Instant rangeStart = DateTimeUtils.startOfMonthUtc(startYm.getYear(), startYm.getMonthValue());
        Instant rangeEnd = DateTimeUtils.startOfNextMonthUtc(endYm.getYear(), endYm.getMonthValue());

        List<CashflowEntryProjection> entries = transactionRepository.findCashflowEntriesForPeriod(
                userId, rangeStart, rangeEnd
        );

        Map<YearMonth, BigDecimal[]> monthlyBuckets = new HashMap<>();
        for (CashflowEntryProjection entry : entries) {
            YearMonth bucketYm = DateTimeUtils.yearMonthUtc(entry.getTransactionDate());
            accumulateEntry(monthlyBuckets, bucketYm, entry.getTransactionType(), nonNull(entry.getAmount()));
        }

        List<MonthlyCashflowPoint> points = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        YearMonth cursor = startYm;
        while (!cursor.isAfter(endYm)) {
            BigDecimal[] pair = monthlyBuckets.getOrDefault(cursor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal income = pair[0];
            BigDecimal expense = pair[1];
            BigDecimal net = income.subtract(expense);

            totalIncome = totalIncome.add(income);
            totalExpense = totalExpense.add(expense);

            points.add(MonthlyCashflowPoint.builder()
                    .year(cursor.getYear())
                    .month(cursor.getMonthValue())
                    .label(String.format("%02d/%d", cursor.getMonthValue(), cursor.getYear()))
                    .income(income)
                    .expense(expense)
                    .netSavings(net)
                    .savingRatePercentage(calculateSavingRate(income, expense))
                    .build());

            cursor = cursor.plusMonths(1);
        }

        BigDecimal monthDivisor = BigDecimal.valueOf(months);
        BigDecimal totalNet = totalIncome.subtract(totalExpense);

        return CashflowTrendResponse.builder()
                .monthsCount(months)
                .startPeriod(startYm.toString())
                .endPeriod(endYm.toString())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .totalNetSavings(totalNet)
                .averageMonthlyIncome(totalIncome.divide(monthDivisor, 2, RoundingMode.HALF_UP))
                .averageMonthlyExpense(totalExpense.divide(monthDivisor, 2, RoundingMode.HALF_UP))
                .averageSavingRatePercentage(calculateSavingRate(totalIncome, totalExpense))
                .points(points)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = ReportCacheService.REPORT_ANALYTICS_CACHE,
            key = "'period:' + #userId + ':' + #cacheVersion + ':' + #startDate + ':' + #endDate"
    )
    public PeriodSummaryResponse computePeriodSummary(
            String userId,
            String cacheVersion,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<TransactionType, BigDecimal> totals = loadTotalsByType(userId, startInstant, endExclusive);
        BigDecimal totalIncome = totals.get(TransactionType.INCOME);
        BigDecimal totalExpense = totals.get(TransactionType.EXPENSE);
        BigDecimal totalTransfer = totals.get(TransactionType.TRANSFER);

        List<CashflowEntryProjection> entries = transactionRepository.findCashflowEntriesForPeriod(
                userId, startInstant, endExclusive
        );

        Map<LocalDate, BigDecimal[]> dailyBuckets = new HashMap<>();
        for (CashflowEntryProjection entry : entries) {
            LocalDate day = entry.getTransactionDate().atOffset(ZoneOffset.UTC).toLocalDate();
            accumulateEntry(dailyBuckets, day, entry.getTransactionType(), nonNull(entry.getAmount()));
        }

        List<DailyCashflowPoint> dailySeries = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            BigDecimal[] pair = dailyBuckets.getOrDefault(cursor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            dailySeries.add(DailyCashflowPoint.builder()
                    .date(cursor)
                    .income(pair[0])
                    .expense(pair[1])
                    .netCashflow(pair[0].subtract(pair[1]))
                    .build());
            cursor = cursor.plusDays(1);
        }

        return PeriodSummaryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .totalTransfer(totalTransfer)
                .netCashflow(totalIncome.subtract(totalExpense))
                .savingRatePercentage(calculateSavingRate(totalIncome, totalExpense))
                .dailySeries(dailySeries)
                .build();
    }

    private SavingGoalOverview buildSavingGoalOverview(String userId) {
        List<SavingGoalAggregateProjection> aggregates = savingGoalRepository.aggregateGoalsByStatusForUser(userId);
        long inProgressCount = 0;
        long completedCount = 0;
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;

        for (SavingGoalAggregateProjection agg : aggregates) {
            if (agg.getStatus() == SavingGoalStatus.CANCELLED) {
                continue;
            }
            long count = agg.getGoalCount() != null ? agg.getGoalCount() : 0L;
            if (agg.getStatus() == SavingGoalStatus.IN_PROGRESS) {
                inProgressCount += count;
            } else if (agg.getStatus() == SavingGoalStatus.COMPLETED) {
                completedCount += count;
            }
            totalTarget = totalTarget.add(nonNull(agg.getTotalTargetAmount()));
            totalCurrent = totalCurrent.add(nonNull(agg.getTotalCurrentAmount()));
        }

        BigDecimal progressPct = totalTarget.compareTo(BigDecimal.ZERO) > 0
                ? totalCurrent.multiply(HUNDRED).divide(totalTarget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return SavingGoalOverview.builder()
                .inProgressCount(inProgressCount)
                .completedCount(completedCount)
                .totalTargetAmount(totalTarget)
                .totalCurrentAmount(totalCurrent)
                .overallProgressPercentage(progressPct)
                .build();
    }

    private Map<TransactionType, BigDecimal> loadTotalsByType(String userId, Instant start, Instant endExclusive) {
        EnumMap<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            totals.put(type, BigDecimal.ZERO);
        }
        List<TransactionTypeSum> rows = transactionRepository.sumAmountsByTypeForPeriod(userId, start, endExclusive);
        for (TransactionTypeSum row : rows) {
            totals.put(row.getTransactionType(), nonNull(row.getTotalAmount()));
        }
        return totals;
    }

    private List<CategoryBreakdownItem> buildBreakdownItems(
            String userId,
            TransactionType type,
            Instant start,
            Instant endExclusive,
            BigDecimal totalAmount
    ) {
        List<CategoryBreakdownProjection> rows = transactionRepository.sumCategoryBreakdownForPeriod(
                userId, type, start, endExclusive
        );
        return mapBreakdownProjections(rows, totalAmount);
    }

    private List<CategoryBreakdownItem> mapBreakdownProjections(
            List<CategoryBreakdownProjection> rows,
            BigDecimal totalAmount
    ) {
        List<CategoryBreakdownItem> items = new ArrayList<>();
        for (CategoryBreakdownProjection row : rows) {
            BigDecimal amount = nonNull(row.getTotalAmount());
            BigDecimal pct = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(HUNDRED).divide(totalAmount, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            items.add(CategoryBreakdownItem.builder()
                    .categoryId(row.getCategoryId())
                    .categoryName(row.getCategoryName())
                    .categoryIcon(row.getCategoryIcon())
                    .categoryColor(row.getCategoryColor())
                    .amount(amount)
                    .percentage(pct)
                    .transactionCount(row.getTransactionCount() != null ? row.getTransactionCount() : 0L)
                    .build());
        }
        return items;
    }

    private <K> void accumulateEntry(
            Map<K, BigDecimal[]> buckets,
            K key,
            TransactionType type,
            BigDecimal amount
    ) {
        BigDecimal[] pair = buckets.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        if (type == TransactionType.INCOME) {
            pair[0] = pair[0].add(amount);
        } else if (type == TransactionType.EXPENSE) {
            pair[1] = pair[1].add(amount);
        }
    }

    private BigDecimal calculateSavingRate(BigDecimal income, BigDecimal expense) {
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return income.subtract(expense).multiply(HUNDRED).divide(income, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChangePercentage(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : new BigDecimal("100.00");
        }
        return current.subtract(previous).multiply(HUNDRED).divide(previous, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}


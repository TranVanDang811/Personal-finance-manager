package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.response.BudgetSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.CashflowTrendResponse;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownResponse;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.PeriodSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceAndComputationTest {

    private static final String USER_ID = "user-123";
    private static final String CACHE_VERSION = "v-abc";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-11T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ReportComputationService computationServiceMock;
    @Mock
    private ReportCacheService reportCacheServiceMock;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SavingGoalRepository savingGoalRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BudgetService budgetService;
    @Mock
    private TransactionService transactionService;

    private ReportServiceImpl reportService;
    private ReportComputationServiceImpl computationService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(computationServiceMock, reportCacheServiceMock, FIXED_CLOCK);
        computationService = new ReportComputationServiceImpl(
                accountRepository,
                savingGoalRepository,
                transactionRepository,
                budgetService,
                transactionService
        );
    }

    @Test
    void reportService_GetDashboardAndDefaults_ShouldUseClockCurrentMonthAndVersion() {
        DashboardSummaryResponse stub = DashboardSummaryResponse.builder().month(11).year(2026).build();
        when(reportCacheServiceMock.getUserCacheVersion(USER_ID)).thenReturn(CACHE_VERSION);
        when(computationServiceMock.computeDashboardSummary(USER_ID, CACHE_VERSION, 11, 2026)).thenReturn(stub);

        DashboardSummaryResponse result = reportService.getDashboardSummary(USER_ID, null, null);

        assertThat(result.getMonth()).isEqualTo(11);
        verify(computationServiceMock).computeDashboardSummary(USER_ID, CACHE_VERSION, 11, 2026);
    }

    @Test
    void reportService_ValidationRules_ShouldRejectInvalidParameters() {
        assertThatThrownBy(() -> reportService.getDashboardSummary(USER_ID, 13, 2026))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reportService.getDashboardSummary(USER_ID, 11, 1999))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reportService.getCategoryBreakdown(
                USER_ID, TransactionType.TRANSFER, null, null, 11, 2026))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reportService.getCashflowTrend(USER_ID, 0, 11, 2026))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reportService.getCashflowTrend(USER_ID, 25, 11, 2026))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reportService.getPeriodSummary(
                USER_ID, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reportService.getPeriodSummary(
                USER_ID, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2), null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reportService_CategoryBreakdownNullParams_ShouldDefaultToExpenseAndCurrentMonth() {
        CategoryBreakdownResponse stub = CategoryBreakdownResponse.builder()
                .transactionType(TransactionType.EXPENSE)
                .build();
        when(reportCacheServiceMock.getUserCacheVersion(USER_ID)).thenReturn(CACHE_VERSION);
        when(computationServiceMock.computeCategoryBreakdown(
                USER_ID,
                CACHE_VERSION,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        )).thenReturn(stub);

        CategoryBreakdownResponse result = reportService.getCategoryBreakdown(USER_ID, null, null, null, null, null);
        assertThat(result.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void computeDashboardSummary_ShouldComputeNetWorthChangesBudgetsGoalsAndTop5Categories() {
        when(accountRepository.sumBalanceByUserIdAndStatus(USER_ID, AccountStatus.ACTIVE))
                .thenReturn(new BigDecimal("15000000"));
        when(savingGoalRepository.aggregateGoalsByStatusForUser(USER_ID))
                .thenReturn(List.of(
                        goalAggregate(SavingGoalStatus.IN_PROGRESS, "5000000", "20000000", 1L),
                        goalAggregate(SavingGoalStatus.COMPLETED, "3000000", "3000000", 1L),
                        goalAggregate(SavingGoalStatus.CANCELLED, "0", "10000000", 1L)
                ));

        Instant novStart = Instant.parse("2026-11-01T00:00:00Z");
        Instant novEnd = Instant.parse("2026-12-01T00:00:00Z");
        Instant octStart = Instant.parse("2026-10-01T00:00:00Z");
        Instant octEnd = Instant.parse("2026-11-01T00:00:00Z");

        when(transactionRepository.sumAmountsByTypeForPeriod(USER_ID, novStart, novEnd))
                .thenReturn(List.of(
                        typeSum(TransactionType.INCOME, "20000000"),
                        typeSum(TransactionType.EXPENSE, "12000000")
                ));
        when(transactionRepository.sumAmountsByTypeForPeriod(USER_ID, octStart, octEnd))
                .thenReturn(List.of(
                        typeSum(TransactionType.EXPENSE, "10000000")
                ));

        when(budgetService.getBudgetSummary(USER_ID, 11, 2026))
                .thenReturn(BudgetSummaryResponse.builder()
                        .month(11)
                        .year(2026)
                        .totalLimitAmount(new BigDecimal("15000000"))
                        .totalSpentAmount(new BigDecimal("12000000"))
                        .overallUsagePercentage(new BigDecimal("80.00"))
                        .warningCount(1)
                        .reachedCount(0)
                        .exceededCount(0)
                        .budgets(List.of())
                        .build());

        List<CategoryBreakdownProjection> sixCategories = List.of(
                catRow("c1", "Ăn uống", "4000000", 5L),
                catRow("c2", "Nhà ở", "3000000", 1L),
                catRow("c3", "Di chuyển", "2000000", 3L),
                catRow("c4", "Mua sắm", "1500000", 2L),
                catRow("c5", "Giải trí", "1000000", 1L),
                catRow("c6", "Khác", "500000", 1L)
        );
        when(transactionRepository.sumCategoryBreakdownForPeriod(USER_ID, TransactionType.EXPENSE, novStart, novEnd))
                .thenReturn(sixCategories);

        when(transactionService.getTransactions(eq(USER_ID), eq(null), any()))
                .thenReturn(PageResponse.<TransactionResponse>builder()
                        .content(List.of(TransactionResponse.builder().id("tx-1").build()))
                        .build());

        DashboardSummaryResponse response = computationService.computeDashboardSummary(USER_ID, CACHE_VERSION, 11, 2026);

        assertThat(response.getTotalActiveAccountBalance()).isEqualByComparingTo("15000000");
        assertThat(response.getTotalSavedInGoals()).isEqualByComparingTo("8000000");
        assertThat(response.getNetWorth()).isEqualByComparingTo("23000000");

        assertThat(response.getMonthlyIncome()).isEqualByComparingTo("20000000");
        assertThat(response.getMonthlyExpense()).isEqualByComparingTo("12000000");
        assertThat(response.getNetCashflow()).isEqualByComparingTo("8000000");
        assertThat(response.getSavingRatePercentage()).isEqualByComparingTo("40.00");

        // Previous month income was 0 while current is 20M -> 100.00; expense 10M -> 12M = 20.00
        assertThat(response.getIncomeChangePercentage()).isEqualByComparingTo("100.00");
        assertThat(response.getExpenseChangePercentage()).isEqualByComparingTo("20.00");

        assertThat(response.getSavingGoalOverview().getInProgressCount()).isEqualTo(1);
        assertThat(response.getSavingGoalOverview().getCompletedCount()).isEqualTo(1);
        assertThat(response.getSavingGoalOverview().getOverallProgressPercentage()).isEqualByComparingTo("34.78");
        assertThat(response.getBudgetOverview().getWarningCount()).isEqualTo(1);

        assertThat(response.getTopExpenseCategories()).hasSize(5).isInstanceOf(ArrayList.class);
        assertThat(response.getTopExpenseCategories().get(0).getPercentage()).isEqualByComparingTo("33.33");
        assertThat(response.getRecentTransactions()).hasSize(1).isInstanceOf(ArrayList.class);
    }

    @Test
    void computeDashboardSummary_ZeroIncome_ShouldReturnZeroSavingRateAndZeroPreviousChangeWhenAlsoZero() {
        when(accountRepository.sumBalanceByUserIdAndStatus(USER_ID, AccountStatus.ACTIVE))
                .thenReturn(BigDecimal.ZERO);
        when(savingGoalRepository.aggregateGoalsByStatusForUser(USER_ID)).thenReturn(List.of());
        when(transactionRepository.sumAmountsByTypeForPeriod(eq(USER_ID), any(), any()))
                .thenReturn(List.of());
        when(budgetService.getBudgetSummary(USER_ID, 11, 2026))
                .thenReturn(BudgetSummaryResponse.builder().budgets(List.of()).build());
        when(transactionRepository.sumCategoryBreakdownForPeriod(eq(USER_ID), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of());
        when(transactionService.getTransactions(eq(USER_ID), eq(null), any()))
                .thenReturn(PageResponse.<TransactionResponse>builder().content(List.of()).build());

        DashboardSummaryResponse response = computationService.computeDashboardSummary(USER_ID, CACHE_VERSION, 11, 2026);

        assertThat(response.getSavingRatePercentage()).isEqualByComparingTo("0.00");
        assertThat(response.getIncomeChangePercentage()).isEqualByComparingTo("0.00");
        assertThat(response.getExpenseChangePercentage()).isEqualByComparingTo("0.00");
        assertThat(response.getTopExpenseCategories()).isEmpty();
    }

    @Test
    void computeCashflowTrend_ShouldZeroFillMissingMonthsAndIgnoreTransfers() {
        Instant start = Instant.parse("2026-09-01T00:00:00Z");
        Instant end = Instant.parse("2026-12-01T00:00:00Z");

        when(transactionRepository.findCashflowEntriesForPeriod(USER_ID, start, end))
                .thenReturn(List.of(
                        cashflowEntry(TransactionType.INCOME, "10000000", "2026-09-05T10:00:00Z"),
                        cashflowEntry(TransactionType.EXPENSE, "4000000", "2026-09-20T10:00:00Z"),
                        cashflowEntry(TransactionType.TRANSFER, "5000000", "2026-10-15T10:00:00Z"),
                        cashflowEntry(TransactionType.INCOME, "20000000", "2026-11-01T10:00:00Z"),
                        cashflowEntry(TransactionType.EXPENSE, "5000000", "2026-11-10T10:00:00Z")
                ));

        CashflowTrendResponse trend = computationService.computeCashflowTrend(USER_ID, CACHE_VERSION, 3, 11, 2026);

        assertThat(trend.getMonthsCount()).isEqualTo(3);
        assertThat(trend.getStartPeriod()).isEqualTo("2026-09");
        assertThat(trend.getEndPeriod()).isEqualTo("2026-11");
        assertThat(trend.getTotalIncome()).isEqualByComparingTo("30000000");
        assertThat(trend.getTotalExpense()).isEqualByComparingTo("9000000");
        assertThat(trend.getTotalNetSavings()).isEqualByComparingTo("21000000");
        assertThat(trend.getAverageMonthlyIncome()).isEqualByComparingTo("10000000.00");
        assertThat(trend.getAverageMonthlyExpense()).isEqualByComparingTo("3000000.00");
        assertThat(trend.getAverageSavingRatePercentage()).isEqualByComparingTo("70.00");

        assertThat(trend.getPoints()).hasSize(3).isInstanceOf(ArrayList.class);
        assertThat(trend.getPoints().get(0).getLabel()).isEqualTo("09/2026");
        assertThat(trend.getPoints().get(0).getSavingRatePercentage()).isEqualByComparingTo("60.00");
        assertThat(trend.getPoints().get(1).getLabel()).isEqualTo("10/2026");
        assertThat(trend.getPoints().get(1).getIncome()).isEqualByComparingTo("0");
        assertThat(trend.getPoints().get(1).getExpense()).isEqualByComparingTo("0");
        assertThat(trend.getPoints().get(2).getLabel()).isEqualTo("11/2026");
        assertThat(trend.getPoints().get(2).getNetSavings()).isEqualByComparingTo("15000000");
    }

    @Test
    void computePeriodSummary_ShouldIncludeTransferTotalAndDailySeries() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 3);
        Instant startInstant = Instant.parse("2026-11-01T00:00:00Z");
        Instant endExclusive = Instant.parse("2026-11-04T00:00:00Z");

        when(transactionRepository.sumAmountsByTypeForPeriod(USER_ID, startInstant, endExclusive))
                .thenReturn(List.of(
                        typeSum(TransactionType.INCOME, "5000000"),
                        typeSum(TransactionType.EXPENSE, "1500000"),
                        typeSum(TransactionType.TRANSFER, "2000000")
                ));
        when(transactionRepository.findCashflowEntriesForPeriod(USER_ID, startInstant, endExclusive))
                .thenReturn(List.of(
                        cashflowEntry(TransactionType.INCOME, "5000000", "2026-11-01T08:00:00Z"),
                        cashflowEntry(TransactionType.TRANSFER, "2000000", "2026-11-02T08:00:00Z"),
                        cashflowEntry(TransactionType.EXPENSE, "1500000", "2026-11-03T18:00:00Z")
                ));

        PeriodSummaryResponse summary = computationService.computePeriodSummary(USER_ID, CACHE_VERSION, start, end);

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("5000000");
        assertThat(summary.getTotalExpense()).isEqualByComparingTo("1500000");
        assertThat(summary.getTotalTransfer()).isEqualByComparingTo("2000000");
        assertThat(summary.getNetCashflow()).isEqualByComparingTo("3500000");
        assertThat(summary.getSavingRatePercentage()).isEqualByComparingTo("70.00");
        assertThat(summary.getDailySeries()).hasSize(3).isInstanceOf(ArrayList.class);
        assertThat(summary.getDailySeries().get(1).getIncome()).isEqualByComparingTo("0");
        assertThat(summary.getDailySeries().get(1).getExpense()).isEqualByComparingTo("0");
        assertThat(summary.getDailySeries().get(2).getNetCashflow()).isEqualByComparingTo("-1500000");
    }

    private TransactionTypeSum typeSum(TransactionType type, String amount) {
        BigDecimal bd = new BigDecimal(amount);
        return new TransactionTypeSum() {
            @Override
            public TransactionType getTransactionType() {
                return type;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return bd;
            }
        };
    }

    private CategoryBreakdownProjection catRow(String id, String name, String amount, long count) {
        BigDecimal bd = new BigDecimal(amount);
        return new CategoryBreakdownProjection() {
            @Override
            public String getCategoryId() {
                return id;
            }

            @Override
            public String getCategoryName() {
                return name;
            }

            @Override
            public String getCategoryIcon() {
                return "icon-" + id;
            }

            @Override
            public String getCategoryColor() {
                return "#000000";
            }

            @Override
            public BigDecimal getTotalAmount() {
                return bd;
            }

            @Override
            public Long getTransactionCount() {
                return count;
            }
        };
    }

    private SavingGoalAggregateProjection goalAggregate(
            SavingGoalStatus status, String current, String target, long count) {
        BigDecimal cur = new BigDecimal(current);
        BigDecimal tgt = new BigDecimal(target);
        return new SavingGoalAggregateProjection() {
            @Override
            public SavingGoalStatus getStatus() {
                return status;
            }

            @Override
            public BigDecimal getTotalCurrentAmount() {
                return cur;
            }

            @Override
            public BigDecimal getTotalTargetAmount() {
                return tgt;
            }

            @Override
            public Long getGoalCount() {
                return count;
            }
        };
    }

    private CashflowEntryProjection cashflowEntry(TransactionType type, String amount, String isoInstant) {
        BigDecimal bd = new BigDecimal(amount);
        Instant instant = Instant.parse(isoInstant);
        return new CashflowEntryProjection() {
            @Override
            public TransactionType getTransactionType() {
                return type;
            }

            @Override
            public BigDecimal getAmount() {
                return bd;
            }

            @Override
            public Instant getTransactionDate() {
                return instant;
            }
        };
    }
}


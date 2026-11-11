package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.configuration.RedisConfig;
import com.pfm.personalfinancemanager.dto.response.CashflowTrendResponse;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownItem;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownResponse;
import com.pfm.personalfinancemanager.dto.response.DailyCashflowPoint;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse.BudgetOverview;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse.SavingGoalOverview;
import com.pfm.personalfinancemanager.dto.response.MonthlyCashflowPoint;
import com.pfm.personalfinancemanager.dto.response.PeriodSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.service.ReportCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCacheServiceImplTest {

    @Test
    void getUserCacheVersion_WithConcurrentCache_ShouldGenerateOnceAndStayStableUntilEvicted() {
        CacheManager cacheManager = new ConcurrentMapCacheManager(ReportCacheService.REPORT_VERSIONS_CACHE);
        ReportCacheServiceImpl service = new ReportCacheServiceImpl(cacheManager);

        String v1First = service.getUserCacheVersion("user-1");
        String v1Second = service.getUserCacheVersion("user-1");
        String v2First = service.getUserCacheVersion("user-2");

        assertThat(v1First).isNotBlank();
        assertThat(v1Second).isEqualTo(v1First);
        assertThat(v2First).isNotBlank().isNotEqualTo(v1First);

        service.evictUserReports("user-1");

        String v1AfterEvict = service.getUserCacheVersion("user-1");
        String v2AfterUser1Evict = service.getUserCacheVersion("user-2");

        assertThat(v1AfterEvict).isNotBlank().isNotEqualTo(v1First);
        assertThat(v2AfterUser1Evict).isEqualTo(v2First);
    }

    @Test
    void getUserCacheVersion_WithNoOpCacheManagerOrBlankUser_ShouldReturnDefaultVersionSafely() {
        CacheManager noOp = new NoOpCacheManager();
        ReportCacheServiceImpl service = new ReportCacheServiceImpl(noOp);

        assertThat(service.getUserCacheVersion("user-1")).isEqualTo("default");
        assertThat(service.getUserCacheVersion(" ")).isEqualTo("default");

        // Must not throw
        service.evictUserReports("user-1");
        service.evictUserReports(null);
        assertThat(service.getUserCacheVersion("user-1")).isEqualTo("default");
    }

    @Test
    void redisJsonSerializer_AllReportDtosAndVersionStrings_ShouldRoundTripCleanly() {
        GenericJackson2JsonRedisSerializer serializer = RedisConfig.createJsonRedisSerializer();

        DashboardSummaryResponse dash = DashboardSummaryResponse.builder()
                .month(11)
                .year(2026)
                .totalActiveAccountBalance(new BigDecimal("15000000"))
                .totalSavedInGoals(new BigDecimal("4000000"))
                .netWorth(new BigDecimal("19000000"))
                .monthlyIncome(new BigDecimal("20000000"))
                .monthlyExpense(new BigDecimal("5000000"))
                .netCashflow(new BigDecimal("15000000"))
                .savingRatePercentage(new BigDecimal("75.00"))
                .previousMonthIncome(BigDecimal.ZERO)
                .previousMonthExpense(BigDecimal.ZERO)
                .incomeChangePercentage(new BigDecimal("100.00"))
                .expenseChangePercentage(new BigDecimal("100.00"))
                .budgetOverview(BudgetOverview.builder()
                        .totalBudgets(1)
                        .totalLimitAmount(new BigDecimal("6000000"))
                        .totalSpentAmount(new BigDecimal("5000000"))
                        .overallUsagePercentage(new BigDecimal("83.33"))
                        .warningCount(1)
                        .build())
                .savingGoalOverview(SavingGoalOverview.builder()
                        .inProgressCount(1)
                        .totalTargetAmount(new BigDecimal("10000000"))
                        .totalCurrentAmount(new BigDecimal("4000000"))
                        .overallProgressPercentage(new BigDecimal("40.00"))
                        .build())
                .topExpenseCategories(new ArrayList<>(List.of(CategoryBreakdownItem.builder()
                        .categoryId("cat-1")
                        .categoryName("Ăn uống")
                        .amount(new BigDecimal("5000000"))
                        .percentage(new BigDecimal("100.00"))
                        .transactionCount(2L)
                        .build())))
                .recentTransactions(new ArrayList<>(List.of(TransactionResponse.builder()
                        .id("tx-1")
                        .accountId("acc-1")
                        .accountType(AccountType.BANK_ACCOUNT)
                        .transactionType(TransactionType.EXPENSE)
                        .amount(new BigDecimal("5000000"))
                        .transactionDate(Instant.parse("2026-11-11T10:00:00Z"))
                        .createdAt(Instant.parse("2026-11-11T10:00:00Z"))
                        .build())))
                .build();

        CategoryBreakdownResponse breakdown = CategoryBreakdownResponse.builder()
                .transactionType(TransactionType.EXPENSE)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .totalAmount(new BigDecimal("5000000"))
                .totalTransactionCount(2L)
                .categories(new ArrayList<>(dash.getTopExpenseCategories()))
                .build();

        CashflowTrendResponse trend = CashflowTrendResponse.builder()
                .monthsCount(1)
                .startPeriod("2026-11")
                .endPeriod("2026-11")
                .totalIncome(new BigDecimal("20000000"))
                .totalExpense(new BigDecimal("5000000"))
                .totalNetSavings(new BigDecimal("15000000"))
                .averageMonthlyIncome(new BigDecimal("20000000.00"))
                .averageMonthlyExpense(new BigDecimal("5000000.00"))
                .averageSavingRatePercentage(new BigDecimal("75.00"))
                .points(new ArrayList<>(List.of(MonthlyCashflowPoint.builder()
                        .year(2026)
                        .month(11)
                        .label("11/2026")
                        .income(new BigDecimal("20000000"))
                        .expense(new BigDecimal("5000000"))
                        .netSavings(new BigDecimal("15000000"))
                        .savingRatePercentage(new BigDecimal("75.00"))
                        .build())))
                .build();

        PeriodSummaryResponse period = PeriodSummaryResponse.builder()
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 1))
                .totalIncome(new BigDecimal("20000000"))
                .totalExpense(new BigDecimal("5000000"))
                .totalTransfer(new BigDecimal("1000000"))
                .netCashflow(new BigDecimal("15000000"))
                .savingRatePercentage(new BigDecimal("75.00"))
                .dailySeries(new ArrayList<>(List.of(DailyCashflowPoint.builder()
                        .date(LocalDate.of(2026, 11, 1))
                        .income(new BigDecimal("20000000"))
                        .expense(new BigDecimal("5000000"))
                        .netCashflow(new BigDecimal("15000000"))
                        .build())))
                .build();

        Object dashCopy = serializer.deserialize(serializer.serialize(dash));
        Object breakdownCopy = serializer.deserialize(serializer.serialize(breakdown));
        Object trendCopy = serializer.deserialize(serializer.serialize(trend));
        Object periodCopy = serializer.deserialize(serializer.serialize(period));
        Object versionCopy = serializer.deserialize(serializer.serialize("v-uuid-123"));

        assertThat(dashCopy).isEqualTo(dash);
        assertThat(breakdownCopy).isEqualTo(breakdown);
        assertThat(trendCopy).isEqualTo(trend);
        assertThat(periodCopy).isEqualTo(period);
        assertThat(versionCopy).isEqualTo("v-uuid-123");
    }
}


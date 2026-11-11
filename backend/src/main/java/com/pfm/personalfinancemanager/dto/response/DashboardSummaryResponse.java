package com.pfm.personalfinancemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private int month;
    private int year;

    private BigDecimal totalActiveAccountBalance;
    private BigDecimal totalSavedInGoals;
    private BigDecimal netWorth;

    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal netCashflow;
    private BigDecimal savingRatePercentage;

    private BigDecimal previousMonthIncome;
    private BigDecimal previousMonthExpense;
    private BigDecimal incomeChangePercentage;
    private BigDecimal expenseChangePercentage;

    private BudgetOverview budgetOverview;
    private SavingGoalOverview savingGoalOverview;
    private List<CategoryBreakdownItem> topExpenseCategories;
    private List<TransactionResponse> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetOverview {
        private int totalBudgets;
        private BigDecimal totalLimitAmount;
        private BigDecimal totalSpentAmount;
        private BigDecimal overallUsagePercentage;
        private long warningCount;
        private long reachedCount;
        private long exceededCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavingGoalOverview {
        private long inProgressCount;
        private long completedCount;
        private BigDecimal totalTargetAmount;
        private BigDecimal totalCurrentAmount;
        private BigDecimal overallProgressPercentage;
    }
}


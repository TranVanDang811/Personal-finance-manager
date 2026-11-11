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
public class BudgetSummaryResponse {

    private Integer month;
    private Integer year;

    private BigDecimal totalLimitAmount;
    private BigDecimal totalSpentAmount;
    private BigDecimal totalRemainingAmount;
    private BigDecimal overallUsagePercentage;

    private long warningCount;
    private long reachedCount;
    private long exceededCount;

    private List<BudgetResponse> budgets;
}


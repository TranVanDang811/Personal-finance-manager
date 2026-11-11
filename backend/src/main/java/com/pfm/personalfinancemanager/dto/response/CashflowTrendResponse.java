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
public class CashflowTrendResponse {

    private int monthsCount;
    private String startPeriod;
    private String endPeriod;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalNetSavings;
    private BigDecimal averageMonthlyIncome;
    private BigDecimal averageMonthlyExpense;
    private BigDecimal averageSavingRatePercentage;
    private List<MonthlyCashflowPoint> points;
}


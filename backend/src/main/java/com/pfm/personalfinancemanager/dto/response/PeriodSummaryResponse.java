package com.pfm.personalfinancemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodSummaryResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalTransfer;
    private BigDecimal netCashflow;
    private BigDecimal savingRatePercentage;
    private List<DailyCashflowPoint> dailySeries;
}


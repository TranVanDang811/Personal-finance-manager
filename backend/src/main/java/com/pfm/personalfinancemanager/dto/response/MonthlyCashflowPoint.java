package com.pfm.personalfinancemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyCashflowPoint {

    private int year;
    private int month;
    private String label;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal netSavings;
    private BigDecimal savingRatePercentage;
}


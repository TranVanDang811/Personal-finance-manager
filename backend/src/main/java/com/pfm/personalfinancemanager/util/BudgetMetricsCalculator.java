package com.pfm.personalfinancemanager.util;

import com.pfm.personalfinancemanager.enums.BudgetAlertLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BudgetMetricsCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("80.00");

    public record BudgetMetrics(
            BigDecimal spentAmount,
            BigDecimal remainingAmount,
            BigDecimal usagePercentage,
            BudgetAlertLevel alertLevel
    ) {
    }

    private BudgetMetricsCalculator() {
    }

    public static BudgetMetrics calculate(BigDecimal limitAmount, BigDecimal rawSpentAmount) {
        BigDecimal safeSpent = rawSpentAmount != null ? rawSpentAmount : BigDecimal.ZERO;
        BigDecimal remaining = limitAmount.subtract(safeSpent);

        BigDecimal percentage;
        if (limitAmount.compareTo(BigDecimal.ZERO) <= 0) {
            percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            percentage = safeSpent
                    .multiply(HUNDRED)
                    .divide(limitAmount, 2, RoundingMode.HALF_UP);
        }

        BudgetAlertLevel alertLevel;
        if (safeSpent.compareTo(limitAmount) > 0) {
            alertLevel = BudgetAlertLevel.EXCEEDED;
        } else if (safeSpent.compareTo(limitAmount) == 0) {
            alertLevel = BudgetAlertLevel.REACHED;
        } else if (percentage.compareTo(WARNING_THRESHOLD) >= 0) {
            alertLevel = BudgetAlertLevel.WARNING;
        } else {
            alertLevel = BudgetAlertLevel.NORMAL;
        }

        return new BudgetMetrics(safeSpent, remaining, percentage, alertLevel);
    }
}


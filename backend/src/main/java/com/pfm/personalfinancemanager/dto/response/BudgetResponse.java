package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.BudgetAlertLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private String id;
    private String userId;

    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;

    private Integer month;
    private Integer year;

    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercentage;
    private BudgetAlertLevel alertLevel;

    private Instant createdAt;
    private Instant updatedAt;
}


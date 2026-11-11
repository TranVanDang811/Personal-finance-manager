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
public class CategoryBreakdownItem {

    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private BigDecimal amount;
    private BigDecimal percentage;
    private long transactionCount;
}


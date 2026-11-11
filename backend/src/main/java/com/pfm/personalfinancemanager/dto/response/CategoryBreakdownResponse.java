package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.TransactionType;
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
public class CategoryBreakdownResponse {

    private TransactionType transactionType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAmount;
    private long totalTransactionCount;
    private List<CategoryBreakdownItem> categories;
}


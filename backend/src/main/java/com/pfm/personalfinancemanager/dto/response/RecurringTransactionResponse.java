package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.RecurringFrequency;
import com.pfm.personalfinancemanager.enums.RecurringStatus;
import com.pfm.personalfinancemanager.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionResponse {

    private String id;
    private String userId;

    private String accountId;
    private String accountName;

    private String toAccountId;
    private String toAccountName;

    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;

    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private RecurringFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextExecutionDate;
    private Instant lastExecutedAt;
    private RecurringStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}


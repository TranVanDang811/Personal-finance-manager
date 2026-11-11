package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.TransactionType;
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
public class TransactionResponse {

    private String id;
    private String userId;

    private String accountId;
    private String accountName;
    private AccountType accountType;

    private String toAccountId;
    private String toAccountName;

    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;

    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private String note;
    private Instant transactionDate;
    private String attachmentUrl;
    private String recurringTransactionId;

    private Instant createdAt;
    private Instant updatedAt;
}


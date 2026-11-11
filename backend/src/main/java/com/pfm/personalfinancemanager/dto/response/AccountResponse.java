package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
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
public class AccountResponse {

    private String id;
    private String userId;
    private String accountName;
    private AccountType accountType;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private String currency;
    private String description;
    private AccountStatus status;
    private Boolean allowNegative;
    private Instant createdAt;
    private Instant updatedAt;
}


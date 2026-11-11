package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.ContributionType;
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
public class SavingGoalContributionResponse {

    private String id;
    private String savingGoalId;
    private String savingGoalName;
    private String accountId;
    private String accountName;
    private String transactionId;
    private ContributionType contributionType;
    private BigDecimal amount;
    private String note;
    private Instant contributionDate;
}


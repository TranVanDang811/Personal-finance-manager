package com.pfm.personalfinancemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryResponse {

    private BigDecimal totalActiveBalance;
    private long activeAccountCount;
    private long archivedAccountCount;
    private List<AccountResponse> accounts;
}


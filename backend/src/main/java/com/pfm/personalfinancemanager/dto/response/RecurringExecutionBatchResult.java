package com.pfm.personalfinancemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExecutionBatchResult {

    private LocalDate executionDate;
    private int totalRulesScanned;
    private int executedCount;
    private int skippedCount;
    private int failedCount;
}


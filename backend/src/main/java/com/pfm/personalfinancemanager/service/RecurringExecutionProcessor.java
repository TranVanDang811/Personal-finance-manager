package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.response.TransactionResponse;

import java.time.LocalDate;

public interface RecurringExecutionProcessor {

    record OccurrenceExecutionResult(
            boolean executed,
            boolean skipped,
            TransactionResponse transaction,
            LocalDate nextExecutionDate,
            LocalDate endDate
    ) {
        public static OccurrenceExecutionResult ofExecuted(
                TransactionResponse transaction,
                LocalDate nextExecutionDate,
                LocalDate endDate
        ) {
            return new OccurrenceExecutionResult(true, false, transaction, nextExecutionDate, endDate);
        }

        public static OccurrenceExecutionResult ofSkipped() {
            return new OccurrenceExecutionResult(false, true, null, null, null);
        }
    }

    OccurrenceExecutionResult executeSingleOccurrence(String recurringId, LocalDate expectedExecutionDate);

    void notifyExecutionFailureOnce(String recurringId, LocalDate executionDate, String reason);
}


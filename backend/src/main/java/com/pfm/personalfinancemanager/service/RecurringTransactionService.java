package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.CreateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.RecurringExecutionBatchResult;
import com.pfm.personalfinancemanager.dto.response.RecurringTransactionResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.enums.RecurringStatus;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionService {

    RecurringTransactionResponse createRecurringTransaction(String userId, CreateRecurringTransactionRequest request);

    RecurringTransactionResponse updateRecurringTransaction(
            String userId,
            String id,
            UpdateRecurringTransactionRequest request
    );

    RecurringTransactionResponse pauseRecurringTransaction(String userId, String id);

    RecurringTransactionResponse resumeRecurringTransaction(String userId, String id);

    RecurringTransactionResponse cancelRecurringTransaction(String userId, String id);

    void deleteRecurringTransaction(String userId, String id);

    RecurringTransactionResponse getRecurringTransactionById(String userId, String id);

    List<RecurringTransactionResponse> getRecurringTransactions(String userId, RecurringStatus status);

    TransactionResponse executeNow(String userId, String id, boolean allowEarly);

    RecurringExecutionBatchResult processDueRecurringTransactions();

    RecurringExecutionBatchResult processDueRecurringTransactionsForDate(LocalDate executionDate);
}


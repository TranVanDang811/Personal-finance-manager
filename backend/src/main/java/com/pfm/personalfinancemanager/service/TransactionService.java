package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.TransactionFilterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface TransactionService {

    TransactionResponse createTransaction(String userId, CreateTransactionRequest request, String idempotencyKey);

    TransactionResponse executeRecurringTransaction(String userId, CreateTransactionRequest request, String recurringTransactionId);

    TransactionResponse updateTransaction(String userId, String transactionId, UpdateTransactionRequest request);

    void deleteTransaction(String userId, String transactionId);

    TransactionResponse getTransactionById(String userId, String transactionId);

    PageResponse<TransactionResponse> getTransactions(String userId, TransactionFilterRequest filter, Pageable pageable);

    PageResponse<TransactionResponse> getTransactionsByAccount(String userId, String accountId, Pageable pageable);

    TransactionResponse uploadAttachment(String userId, String transactionId, MultipartFile file);
}


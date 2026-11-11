package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateAccountRequest;
import com.pfm.personalfinancemanager.dto.response.AccountResponse;
import com.pfm.personalfinancemanager.dto.response.AccountSummaryResponse;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(String userId, CreateAccountRequest request);

    List<AccountResponse> getAccounts(String userId, AccountStatus status, AccountType accountType);

    AccountSummaryResponse getAccountSummary(String userId);

    AccountResponse getAccountById(String userId, String accountId);

    AccountResponse updateAccount(String userId, String accountId, UpdateAccountRequest request);

    AccountResponse archiveAccount(String userId, String accountId);

    AccountResponse unarchiveAccount(String userId, String accountId);

    void deleteAccount(String userId, String accountId);
}


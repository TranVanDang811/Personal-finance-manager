package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.annotation.EvictUserReports;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateAccountRequest;
import com.pfm.personalfinancemanager.dto.response.AccountResponse;
import com.pfm.personalfinancemanager.dto.response.AccountSummaryResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.DuplicateResourceException;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.AccountMapper;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional
    @EvictUserReports
    public AccountResponse createAccount(String userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String normalizedName = request.getAccountName().trim();
        boolean allowNegative = Boolean.TRUE.equals(request.getAllowNegative());

        validateAllowNegativePolicy(request.getAccountType(), allowNegative);

        if (!allowNegative && request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Số dư ban đầu không được nhỏ hơn 0 khi không cho phép số dư âm");
        }

        if (accountRepository.existsByUserIdAndAccountNameIgnoreCaseAndIsDeletedFalse(userId, normalizedName)) {
            throw new DuplicateResourceException("Tài khoản", "tên", normalizedName);
        }

        String currency = StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : user.getCurrency();

        Account account = Account.builder()
                .user(user)
                .accountName(normalizedName)
                .accountType(request.getAccountType())
                .initialBalance(request.getInitialBalance())
                .currentBalance(request.getInitialBalance())
                .currency(currency)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(AccountStatus.ACTIVE)
                .allowNegative(allowNegative)
                .isDeleted(false)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Created account {} for user {}", savedAccount.getId(), userId);
        return accountMapper.toResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(String userId, AccountStatus status, AccountType accountType) {
        return accountRepository.findAllByUserWithFilters(userId, status, accountType)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountSummaryResponse getAccountSummary(String userId) {
        List<AccountResponse> allAccounts = accountRepository.findAllByUserWithFilters(userId, null, null)
                .stream()
                .map(accountMapper::toResponse)
                .toList();

        BigDecimal totalActiveBalance = accountRepository.sumBalanceByUserIdAndStatus(userId, AccountStatus.ACTIVE);
        long activeCount = accountRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, AccountStatus.ACTIVE);
        long archivedCount = accountRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, AccountStatus.ARCHIVED);

        return AccountSummaryResponse.builder()
                .totalActiveBalance(totalActiveBalance)
                .activeAccountCount(activeCount)
                .archivedAccountCount(archivedCount)
                .accounts(allAccounts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(String userId, String accountId) {
        Account account = findOwnedAccountOrThrow(userId, accountId);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    @EvictUserReports
    public AccountResponse updateAccount(String userId, String accountId, UpdateAccountRequest request) {
        Account account = findOwnedAccountOrThrow(userId, accountId);

        String normalizedName = request.getAccountName().trim();
        boolean allowNegative = Boolean.TRUE.equals(request.getAllowNegative());

        validateAllowNegativePolicy(request.getAccountType(), allowNegative);

        if (!allowNegative && account.getCurrentBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Không thể tắt chế độ số dư âm khi số dư hiện tại đang âm");
        }

        if (accountRepository.existsByUserIdAndAccountNameIgnoreCaseAndIdNotAndIsDeletedFalse(
                userId, normalizedName, accountId)) {
            throw new DuplicateResourceException("Tài khoản", "tên", normalizedName);
        }

        account.setAccountName(normalizedName);
        account.setAccountType(request.getAccountType());
        account.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        account.setAllowNegative(allowNegative);
        account.setStatus(request.getStatus());

        Account updated = accountRepository.save(account);
        return accountMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @EvictUserReports
    public AccountResponse archiveAccount(String userId, String accountId) {
        Account account = findOwnedAccountOrThrow(userId, accountId);
        account.setStatus(AccountStatus.ARCHIVED);
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    @EvictUserReports
    public AccountResponse unarchiveAccount(String userId, String accountId) {
        Account account = findOwnedAccountOrThrow(userId, accountId);
        account.setStatus(AccountStatus.ACTIVE);
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    @EvictUserReports
    public void deleteAccount(String userId, String accountId) {
        Account account = findOwnedAccountOrThrow(userId, accountId);
        account.setIsDeleted(true);
        account.setDeletedAt(Instant.now());
        account.setStatus(AccountStatus.ARCHIVED);
        accountRepository.save(account);
        log.info("Soft deleted account {} for user {}", accountId, userId);
    }

    private Account findOwnedAccountOrThrow(String userId, String accountId) {
        Account account = accountRepository.findById(accountId)
                .filter(acc -> !Boolean.TRUE.equals(acc.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền truy cập tài khoản này");
        }
        return account;
    }

    private void validateAllowNegativePolicy(AccountType accountType, boolean allowNegative) {
        if (allowNegative && accountType != AccountType.BANK_ACCOUNT) {
            throw new BusinessException("Chỉ tài khoản ngân hàng (BANK_ACCOUNT) mới được phép bật chế độ số dư âm");
        }
    }
}


package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateAccountRequest;
import com.pfm.personalfinancemanager.dto.response.AccountResponse;
import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.AccountMapper;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void createAccount_ShouldRejectAllowNegativeForCashAccount() {
        User user = User.builder().id("u1").currency("VND").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        CreateAccountRequest request = CreateAccountRequest.builder()
                .accountName("Ví tiền mặt")
                .accountType(AccountType.CASH)
                .initialBalance(BigDecimal.ZERO)
                .allowNegative(true)
                .build();

        assertThatThrownBy(() -> accountService.createAccount("u1", request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createAccount_ShouldRejectNegativeInitialBalanceWhenAllowNegativeFalse() {
        User user = User.builder().id("u1").currency("VND").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        CreateAccountRequest request = CreateAccountRequest.builder()
                .accountName("Tài khoản VCB")
                .accountType(AccountType.BANK_ACCOUNT)
                .initialBalance(new BigDecimal("-50000"))
                .allowNegative(false)
                .build();

        assertThatThrownBy(() -> accountService.createAccount("u1", request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createAccount_ShouldInitializeCurrentBalanceEqualToInitialBalance() {
        User user = User.builder().id("u1").currency("VND").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(accountRepository.existsByUserIdAndAccountNameIgnoreCaseAndIsDeletedFalse("u1", "Techcombank"))
                .thenReturn(false);

        Account saved = Account.builder()
                .id("acc-1")
                .user(user)
                .accountName("Techcombank")
                .accountType(AccountType.BANK_ACCOUNT)
                .initialBalance(new BigDecimal("5000000"))
                .currentBalance(new BigDecimal("5000000"))
                .currency("VND")
                .status(AccountStatus.ACTIVE)
                .allowNegative(false)
                .isDeleted(false)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(saved);
        when(accountMapper.toResponse(saved)).thenReturn(
                AccountResponse.builder()
                        .id("acc-1")
                        .accountName("Techcombank")
                        .currentBalance(new BigDecimal("5000000"))
                        .build()
        );

        CreateAccountRequest request = CreateAccountRequest.builder()
                .accountName("Techcombank")
                .accountType(AccountType.BANK_ACCOUNT)
                .initialBalance(new BigDecimal("5000000"))
                .build();

        AccountResponse response = accountService.createAccount("u1", request);
        assertThat(response.getCurrentBalance()).isEqualByComparingTo("5000000");
    }

    @Test
    void updateAccount_ShouldRejectDisablingAllowNegativeWhenBalanceIsNegative() {
        User user = User.builder().id("u1").build();
        Account account = Account.builder()
                .id("acc-1")
                .user(user)
                .accountType(AccountType.BANK_ACCOUNT)
                .currentBalance(new BigDecimal("-100000"))
                .allowNegative(true)
                .isDeleted(false)
                .build();

        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));

        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .accountName("VCB")
                .accountType(AccountType.BANK_ACCOUNT)
                .allowNegative(false)
                .status(AccountStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> accountService.updateAccount("u1", "acc-1", request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getAccountById_ShouldThrowUnauthorizedWhenAccessedByAnotherUser() {
        User owner = User.builder().id("owner-1").build();
        Account account = Account.builder().id("acc-1").user(owner).isDeleted(false).build();
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.getAccountById("attacker-2", "acc-1"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }
}


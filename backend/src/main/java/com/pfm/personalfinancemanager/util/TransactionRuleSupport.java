package com.pfm.personalfinancemanager.util;

import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public final class TransactionRuleSupport {

    private TransactionRuleSupport() {
    }

    public static Category validateAndLoadCategory(
            String userId,
            TransactionType transactionType,
            String accountId,
            String toAccountId,
            String categoryId,
            BigDecimal amount,
            CategoryRepository categoryRepository
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_AMOUNT, "Số tiền giao dịch phải lớn hơn 0");
        }
        if (!StringUtils.hasText(accountId)) {
            throw new BusinessException("Tài khoản nguồn không được để trống");
        }
        if (transactionType == null) {
            throw new BusinessException("Loại giao dịch không được để trống");
        }

        if (transactionType == TransactionType.TRANSFER) {
            if (!StringUtils.hasText(toAccountId)) {
                throw new BusinessException("Giao dịch chuyển tiền phải chỉ định tài khoản đích");
            }
            if (accountId.equals(toAccountId)) {
                throw new BusinessException(
                        ErrorCode.SAME_ACCOUNT_TRANSFER,
                        "Tài khoản nguồn và tài khoản đích không được trùng nhau"
                );
            }
            if (StringUtils.hasText(categoryId)) {
                throw new BusinessException("Giao dịch chuyển tiền không sử dụng danh mục thu/chi");
            }
            return null;
        }

        if (StringUtils.hasText(toAccountId)) {
            throw new BusinessException("Giao dịch thu/chi không được chỉ định tài khoản đích");
        }
        if (!StringUtils.hasText(categoryId)) {
            throw new BusinessException("Giao dịch thu/chi bắt buộc phải chọn danh mục");
        }

        Category category = categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", categoryId));

        if (!Boolean.TRUE.equals(category.getIsSystem())
                && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
            throw new UnauthorizedAccessException("Bạn không có quyền sử dụng danh mục này");
        }

        CategoryType expectedType = transactionType == TransactionType.INCOME
                ? CategoryType.INCOME
                : CategoryType.EXPENSE;
        if (category.getCategoryType() != expectedType) {
            throw new BusinessException(
                    ErrorCode.CATEGORY_TYPE_MISMATCH,
                    String.format("Không thể dùng danh mục loại %s cho giao dịch loại %s",
                            category.getCategoryType(), transactionType)
            );
        }

        return category;
    }

    public static Account validateOwnedActiveAccount(
            String userId,
            String accountId,
            AccountRepository accountRepository
    ) {
        Account account = accountRepository.findByIdAndIsDeletedFalse(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", accountId));
        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền thao tác trên tài khoản này");
        }
        ensureAccountsActive(account);
        return account;
    }

    public static void ensureAccountsActive(Account... accounts) {
        for (Account account : accounts) {
            if (account != null && account.getStatus() != AccountStatus.ACTIVE) {
                throw new BusinessException(
                        String.format("Tài khoản '%s' đang ở trạng thái lưu trữ, không thể thực hiện giao dịch",
                                account.getAccountName())
                );
            }
        }
    }
}


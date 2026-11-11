package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateAccountRequest;
import com.pfm.personalfinancemanager.dto.response.AccountResponse;
import com.pfm.personalfinancemanager.dto.response.AccountSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.service.AccountService;
import com.pfm.personalfinancemanager.service.TransactionService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Quản lý tài khoản tiền mặt, ngân hàng và tiết kiệm")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Tạo tài khoản tài chính mới")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        AccountResponse response = accountService.createAccount(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tài khoản thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tài khoản của người dùng")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) AccountType type
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccounts(userId, status, type)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Xem tổng hợp số dư và thống kê tài khoản")
    public ResponseEntity<ApiResponse<AccountSummaryResponse>> getAccountSummary() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountSummary(userId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountById(userId, id)));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Xem lịch sử giao dịch của một tài khoản")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAccountTransactions(
            @PathVariable String id,
            @PageableDefault(size = 20, sort = {"transactionDate", "createdAt"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionsByAccount(userId, id, pageable)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable String id,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        AccountResponse response = accountService.updateAccount(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tài khoản thành công", response));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Lưu trữ tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> archiveAccount(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        AccountResponse response = accountService.archiveAccount(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Lưu trữ tài khoản thành công", response));
    }

    @PatchMapping("/{id}/unarchive")
    @Operation(summary = "Khôi phục tài khoản đang lưu trữ")
    public ResponseEntity<ApiResponse<AccountResponse>> unarchiveAccount(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        AccountResponse response = accountService.unarchiveAccount(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục tài khoản thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm tài khoản")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        accountService.deleteAccount(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài khoản thành công"));
    }
}


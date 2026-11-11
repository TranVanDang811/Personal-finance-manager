package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.request.CreateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.RecurringTransactionResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.enums.RecurringStatus;
import com.pfm.personalfinancemanager.service.RecurringTransactionService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/recurring-transactions")
@RequiredArgsConstructor
@Tag(name = "Recurring Transactions", description = "Quản lý lịch giao dịch định kỳ tự động")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @PostMapping
    @Operation(summary = "Tạo lịch giao dịch định kỳ mới")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> createRecurringTransaction(
            @Valid @RequestBody CreateRecurringTransactionRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        RecurringTransactionResponse response = recurringTransactionService.createRecurringTransaction(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo giao dịch định kỳ thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách giao dịch định kỳ của người dùng")
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponse>>> getRecurringTransactions(
            @RequestParam(required = false) RecurringStatus status
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(recurringTransactionService.getRecurringTransactions(userId, status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một giao dịch định kỳ")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> getRecurringTransactionById(
            @PathVariable String id
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                recurringTransactionService.getRecurringTransactionById(userId, id)
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật cấu hình giao dịch định kỳ")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> updateRecurringTransaction(
            @PathVariable String id,
            @Valid @RequestBody UpdateRecurringTransactionRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        RecurringTransactionResponse response = recurringTransactionService.updateRecurringTransaction(
                userId, id, request
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giao dịch định kỳ thành công", response));
    }

    @PatchMapping("/{id}/pause")
    @Operation(summary = "Tạm dừng giao dịch định kỳ")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> pauseRecurringTransaction(
            @PathVariable String id
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        RecurringTransactionResponse response = recurringTransactionService.pauseRecurringTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã tạm dừng giao dịch định kỳ", response));
    }

    @PatchMapping("/{id}/resume")
    @Operation(summary = "Kích hoạt lại giao dịch định kỳ đang tạm dừng")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> resumeRecurringTransaction(
            @PathVariable String id
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        RecurringTransactionResponse response = recurringTransactionService.resumeRecurringTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt lại giao dịch định kỳ", response));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Hủy giao dịch định kỳ")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> cancelRecurringTransaction(
            @PathVariable String id
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        RecurringTransactionResponse response = recurringTransactionService.cancelRecurringTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy giao dịch định kỳ", response));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Thực thi thủ công kỳ giao dịch định kỳ tiếp theo")
    public ResponseEntity<ApiResponse<TransactionResponse>> executeNow(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean allowEarly
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = recurringTransactionService.executeNow(userId, id, allowEarly);
        return ResponseEntity.ok(ApiResponse.success("Thực thi giao dịch định kỳ thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa lịch giao dịch định kỳ (các giao dịch đã sinh trước đó được giữ nguyên)")
    public ResponseEntity<ApiResponse<Void>> deleteRecurringTransaction(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        recurringTransactionService.deleteRecurringTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa giao dịch định kỳ thành công"));
    }
}


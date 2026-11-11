package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.TransactionFilterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.service.TransactionService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Quản lý giao dịch thu nhập, chi tiêu và chuyển khoản")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Tạo giao dịch mới (hỗ trợ X-Idempotency-Key chống gửi trùng)")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.createTransaction(userId, request, idempotencyKey);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo giao dịch thành công", response));
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm, lọc và phân trang danh sách giao dịch")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = {"transactionDate", "createdAt"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        TransactionFilterRequest filter = TransactionFilterRequest.builder()
                .keyword(keyword)
                .transactionType(transactionType)
                .accountId(accountId)
                .categoryId(categoryId)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactions(userId, filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một giao dịch")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionById(userId, id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật giao dịch (tự động hoàn nguyên số dư cũ và áp dụng số dư mới)")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable String id,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giao dịch thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa giao dịch và hoàn nguyên số dư tài khoản")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa giao dịch thành công"));
    }

    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload hóa đơn hoặc hình ảnh minh chứng cho giao dịch (JPG/PNG/WEBP, tối đa 5MB)")
    public ResponseEntity<ApiResponse<TransactionResponse>> uploadAttachment(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.uploadAttachment(userId, id, file);
        return ResponseEntity.ok(ApiResponse.success("Upload minh chứng giao dịch thành công", response));
    }
}


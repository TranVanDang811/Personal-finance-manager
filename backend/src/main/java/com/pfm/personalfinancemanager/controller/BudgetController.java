package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.request.CreateBudgetRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateBudgetRequest;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.BudgetResponse;
import com.pfm.personalfinancemanager.dto.response.BudgetSummaryResponse;
import com.pfm.personalfinancemanager.service.BudgetService;
import com.pfm.personalfinancemanager.util.DateTimeUtils;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Quản lý ngân sách chi tiêu theo danh mục và tháng")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Thiết lập ngân sách mới cho danh mục chi tiêu")
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody CreateBudgetRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        BudgetResponse response = budgetService.createBudget(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thiết lập ngân sách thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách ngân sách (có thể lọc theo tháng/năm)")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgets(userId, month, year)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Xem tổng hợp tình hình sử dụng ngân sách theo tháng/năm")
    public ResponseEntity<ApiResponse<BudgetSummaryResponse>> getBudgetSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        YearMonth current = DateTimeUtils.yearMonthUtc(Instant.now());
        int targetMonth = month != null ? month : current.getMonthValue();
        int targetYear = year != null ? year : current.getYear();
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgetSummary(userId, targetMonth, targetYear)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một ngân sách")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgetById(userId, id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hạn mức ngân sách")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable String id,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        BudgetResponse response = budgetService.updateBudget(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ngân sách thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa ngân sách")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa ngân sách thành công"));
    }
}


package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.request.CreateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.request.SavingGoalContributionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.SavingGoalContributionResponse;
import com.pfm.personalfinancemanager.dto.response.SavingGoalResponse;
import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
import com.pfm.personalfinancemanager.service.SavingGoalService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/saving-goals")
@RequiredArgsConstructor
@Tag(name = "Saving Goals", description = "Quản lý mục tiêu tiết kiệm và lịch sử đóng góp")
public class SavingGoalController {

    private final SavingGoalService savingGoalService;

    @PostMapping
    @Operation(summary = "Tạo mục tiêu tiết kiệm mới")
    public ResponseEntity<ApiResponse<SavingGoalResponse>> createSavingGoal(
            @Valid @RequestBody CreateSavingGoalRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        SavingGoalResponse response = savingGoalService.createSavingGoal(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mục tiêu tiết kiệm thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách mục tiêu tiết kiệm của người dùng")
    public ResponseEntity<ApiResponse<List<SavingGoalResponse>>> getSavingGoals(
            @RequestParam(required = false) SavingGoalStatus status
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(savingGoalService.getSavingGoals(userId, status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một mục tiêu tiết kiệm")
    public ResponseEntity<ApiResponse<SavingGoalResponse>> getSavingGoalById(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(savingGoalService.getSavingGoalById(userId, id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin mục tiêu tiết kiệm")
    public ResponseEntity<ApiResponse<SavingGoalResponse>> updateSavingGoal(
            @PathVariable String id,
            @Valid @RequestBody UpdateSavingGoalRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        SavingGoalResponse response = savingGoalService.updateSavingGoal(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mục tiêu tiết kiệm thành công", response));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Hủy mục tiêu tiết kiệm")
    public ResponseEntity<ApiResponse<SavingGoalResponse>> cancelSavingGoal(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        SavingGoalResponse response = savingGoalService.cancelSavingGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy mục tiêu tiết kiệm", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mục tiêu tiết kiệm (yêu cầu đã rút hết số dư)")
    public ResponseEntity<ApiResponse<Void>> deleteSavingGoal(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        savingGoalService.deleteSavingGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mục tiêu tiết kiệm thành công"));
    }

    @PostMapping("/{id}/deposit")
    @Operation(summary = "Nạp tiền từ tài khoản vào mục tiêu tiết kiệm")
    public ResponseEntity<ApiResponse<SavingGoalResponse>> deposit(
            @PathVariable String id,
            @Valid @RequestBody SavingGoalContributionRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        SavingGoalResponse response = savingGoalService.deposit(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Nạp tiền vào mục tiêu tiết kiệm thành công", response));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Rút tiền từ mục tiêu tiết kiệm về tài khoản")
    public ResponseEntity<ApiResponse<SavingGoalResponse>> withdraw(
            @PathVariable String id,
            @Valid @RequestBody SavingGoalContributionRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        SavingGoalResponse response = savingGoalService.withdraw(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Rút tiền từ mục tiêu tiết kiệm thành công", response));
    }

    @GetMapping("/{id}/contributions")
    @Operation(summary = "Xem lịch sử nạp/rút của một mục tiêu tiết kiệm")
    public ResponseEntity<ApiResponse<PageResponse<SavingGoalContributionResponse>>> getContributions(
            @PathVariable String id,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(savingGoalService.getContributions(userId, id, pageable)));
    }
}


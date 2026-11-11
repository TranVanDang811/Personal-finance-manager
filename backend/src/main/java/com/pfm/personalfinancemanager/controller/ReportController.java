package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.CashflowTrendResponse;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownResponse;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.PeriodSummaryResponse;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.service.ReportService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports & Analytics", description = "Báo cáo tổng quan tài chính, phân bổ danh mục và xu hướng dòng tiền")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Lấy tổng hợp Dashboard (tài sản, thu chi tháng, ngân sách, mục tiêu, giao dịch gần đây)")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(reportService.getDashboardSummary(userId, month, year)));
    }

    @GetMapping("/category-breakdown")
    @Operation(summary = "Thống kê tỷ trọng thu/chi theo danh mục")
    public ResponseEntity<ApiResponse<CategoryBreakdownResponse>> getCategoryBreakdown(
            @RequestParam(defaultValue = "EXPENSE") TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getCategoryBreakdown(userId, type, startDate, endDate, month, year)
        ));
    }

    @GetMapping("/cashflow-trend")
    @Operation(summary = "Thống kê xu hướng thu/chi/tỷ lệ tiết kiệm theo nhiều tháng")
    public ResponseEntity<ApiResponse<CashflowTrendResponse>> getCashflowTrend(
            @RequestParam(required = false) Integer months,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(reportService.getCashflowTrend(userId, months, month, year)));
    }

    @GetMapping("/period-summary")
    @Operation(summary = "Tổng hợp dòng tiền thu/chi/chuyển khoản và biến động theo từng ngày trong kỳ")
    public ResponseEntity<ApiResponse<PeriodSummaryResponse>> getPeriodSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getPeriodSummary(userId, startDate, endDate, month, year)
        ));
    }
}


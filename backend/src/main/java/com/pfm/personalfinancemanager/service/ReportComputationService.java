package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.response.CashflowTrendResponse;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownResponse;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.PeriodSummaryResponse;
import com.pfm.personalfinancemanager.enums.TransactionType;

import java.time.LocalDate;

public interface ReportComputationService {

    DashboardSummaryResponse computeDashboardSummary(String userId, String cacheVersion, int month, int year);

    CategoryBreakdownResponse computeCategoryBreakdown(
            String userId,
            String cacheVersion,
            TransactionType type,
            LocalDate startDate,
            LocalDate endDate
    );

    CashflowTrendResponse computeCashflowTrend(
            String userId,
            String cacheVersion,
            int months,
            int endMonth,
            int endYear
    );

    PeriodSummaryResponse computePeriodSummary(
            String userId,
            String cacheVersion,
            LocalDate startDate,
            LocalDate endDate
    );
}


package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.response.CashflowTrendResponse;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownResponse;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.PeriodSummaryResponse;
import com.pfm.personalfinancemanager.enums.TransactionType;

import java.time.LocalDate;

public interface ReportService {

    DashboardSummaryResponse getDashboardSummary(String userId, Integer month, Integer year);

    CategoryBreakdownResponse getCategoryBreakdown(
            String userId,
            TransactionType type,
            LocalDate startDate,
            LocalDate endDate,
            Integer month,
            Integer year
    );

    CashflowTrendResponse getCashflowTrend(String userId, Integer months, Integer month, Integer year);

    PeriodSummaryResponse getPeriodSummary(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            Integer month,
            Integer year
    );
}


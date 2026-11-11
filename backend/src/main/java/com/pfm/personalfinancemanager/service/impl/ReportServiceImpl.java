package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.response.CashflowTrendResponse;
import com.pfm.personalfinancemanager.dto.response.CategoryBreakdownResponse;
import com.pfm.personalfinancemanager.dto.response.DashboardSummaryResponse;
import com.pfm.personalfinancemanager.dto.response.PeriodSummaryResponse;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.service.ReportCacheService;
import com.pfm.personalfinancemanager.service.ReportComputationService;
import com.pfm.personalfinancemanager.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int DEFAULT_TREND_MONTHS = 6;
    private static final int MAX_TREND_MONTHS = 24;
    private static final long MAX_DAILY_PERIOD_DAYS = 366;

    private final ReportComputationService computationService;
    private final ReportCacheService reportCacheService;
    private final Clock clock;

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(String userId, Integer month, Integer year) {
        YearMonth ym = resolveYearMonth(month, year);
        String cacheVersion = reportCacheService.getUserCacheVersion(userId);
        return computationService.computeDashboardSummary(userId, cacheVersion, ym.getMonthValue(), ym.getYear());
    }

    @Override
    public CategoryBreakdownResponse getCategoryBreakdown(
            String userId,
            TransactionType type,
            LocalDate startDate,
            LocalDate endDate,
            Integer month,
            Integer year
    ) {
        TransactionType effectiveType = type != null ? type : TransactionType.EXPENSE;
        if (effectiveType == TransactionType.TRANSFER) {
            throw new BusinessException("Thống kê theo danh mục chỉ áp dụng cho thu nhập (INCOME) hoặc chi tiêu (EXPENSE)");
        }

        DateRange range = resolveDateRange(startDate, endDate, month, year);
        String cacheVersion = reportCacheService.getUserCacheVersion(userId);
        return computationService.computeCategoryBreakdown(
                userId, cacheVersion, effectiveType, range.startDate(), range.endDate()
        );
    }

    @Override
    public CashflowTrendResponse getCashflowTrend(String userId, Integer months, Integer month, Integer year) {
        int effectiveMonths = months != null ? months : DEFAULT_TREND_MONTHS;
        if (effectiveMonths < 1 || effectiveMonths > MAX_TREND_MONTHS) {
            throw new BusinessException("Số tháng xu hướng phải nằm trong khoảng từ 1 đến " + MAX_TREND_MONTHS);
        }

        YearMonth endYm = resolveYearMonth(month, year);
        String cacheVersion = reportCacheService.getUserCacheVersion(userId);
        return computationService.computeCashflowTrend(
                userId, cacheVersion, effectiveMonths, endYm.getMonthValue(), endYm.getYear()
        );
    }

    @Override
    public PeriodSummaryResponse getPeriodSummary(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            Integer month,
            Integer year
    ) {
        DateRange range = resolveDateRange(startDate, endDate, month, year);
        long days = ChronoUnit.DAYS.between(range.startDate(), range.endDate()) + 1;
        if (days > MAX_DAILY_PERIOD_DAYS) {
            throw new BusinessException("Khoảng thời gian thống kê theo ngày tối đa là 366 ngày");
        }

        String cacheVersion = reportCacheService.getUserCacheVersion(userId);
        return computationService.computePeriodSummary(userId, cacheVersion, range.startDate(), range.endDate());
    }

    private YearMonth resolveYearMonth(Integer month, Integer year) {
        LocalDate now = LocalDate.now(clock);
        int resolvedMonth = month != null ? month : now.getMonthValue();
        int resolvedYear = year != null ? year : now.getYear();

        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new BusinessException("Tháng phải nằm trong khoảng từ 1 đến 12");
        }
        if (resolvedYear < 2000 || resolvedYear > 2100) {
            throw new BusinessException("Năm phải nằm trong khoảng từ 2000 đến 2100");
        }
        return YearMonth.of(resolvedYear, resolvedMonth);
    }

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate, Integer month, Integer year) {
        if (startDate != null || endDate != null) {
            LocalDate now = LocalDate.now(clock);
            LocalDate start = startDate != null ? startDate : now.withDayOfMonth(1);
            LocalDate end = endDate != null ? endDate : now;
            if (end.isBefore(start)) {
                throw new BusinessException("Ngày kết thúc không được nhỏ hơn ngày bắt đầu");
            }
            return new DateRange(start, end);
        }

        YearMonth ym = resolveYearMonth(month, year);
        return new DateRange(ym.atDay(1), ym.atEndOfMonth());
    }
}


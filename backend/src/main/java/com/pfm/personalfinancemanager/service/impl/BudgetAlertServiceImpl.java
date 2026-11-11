package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.entity.Budget;
import com.pfm.personalfinancemanager.enums.BudgetAlertLevel;
import com.pfm.personalfinancemanager.enums.NotificationType;
import com.pfm.personalfinancemanager.repository.BudgetRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.service.BudgetAlertService;
import com.pfm.personalfinancemanager.service.NotificationService;
import com.pfm.personalfinancemanager.util.BudgetMetricsCalculator;
import com.pfm.personalfinancemanager.util.BudgetMetricsCalculator.BudgetMetrics;
import com.pfm.personalfinancemanager.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BudgetAlertServiceImpl implements BudgetAlertService {

    private static final String REF_TYPE_WARNING_80 = "BUDGET_WARNING_80";
    private static final String REF_TYPE_REACHED_100 = "BUDGET_REACHED_100";
    private static final String REF_TYPE_EXCEEDED = "BUDGET_EXCEEDED";

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void evaluateBudgetAlertForTransaction(String userId, String categoryId, Instant transactionDate) {
        if (userId == null || categoryId == null || transactionDate == null) {
            return;
        }

        YearMonth ym = DateTimeUtils.yearMonthUtc(transactionDate);
        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                userId, categoryId, ym.getMonthValue(), ym.getYear()
        ).ifPresent(budget -> {
            BigDecimal spent = transactionRepository.sumExpenseAmountByUserAndCategoryAndPeriod(
                    userId,
                    categoryId,
                    DateTimeUtils.startOfMonthUtc(ym.getYear(), ym.getMonthValue()),
                    DateTimeUtils.startOfNextMonthUtc(ym.getYear(), ym.getMonthValue())
            );
            evaluateBudgetAlert(budget, spent);
        });
    }

    @Override
    @Transactional
    public void evaluateBudgetAlert(Budget budget, BigDecimal spentAmount) {
        BudgetMetrics metrics = BudgetMetricsCalculator.calculate(budget.getLimitAmount(), spentAmount);
        if (metrics.alertLevel() == BudgetAlertLevel.NORMAL) {
            return;
        }

        String userId = budget.getUser().getId();
        String categoryName = budget.getCategory().getName();
        String spentFormatted = formatAmount(metrics.spentAmount());
        String limitFormatted = formatAmount(budget.getLimitAmount());
        String periodText = String.format("%02d/%d", budget.getMonth(), budget.getYear());

        NotificationType notificationType;
        String referenceType;
        String title;
        String message;

        switch (metrics.alertLevel()) {
            case WARNING -> {
                notificationType = NotificationType.BUDGET_WARNING;
                referenceType = REF_TYPE_WARNING_80;
                title = String.format("Ngân sách %s đã đạt %s%%", categoryName, metrics.usagePercentage());
                message = String.format(
                        "Chi tiêu danh mục %s tháng %s đã đạt %s (%s%% trên hạn mức %s).",
                        categoryName, periodText, spentFormatted, metrics.usagePercentage(), limitFormatted
                );
            }
            case REACHED -> {
                notificationType = NotificationType.BUDGET_EXCEEDED;
                referenceType = REF_TYPE_REACHED_100;
                title = String.format("Đã sử dụng 100%% ngân sách: %s", categoryName);
                message = String.format(
                        "Bạn đã chi đúng 100%% hạn mức ngân sách %s tháng %s (%s / %s).",
                        categoryName, periodText, spentFormatted, limitFormatted
                );
            }
            case EXCEEDED -> {
                notificationType = NotificationType.BUDGET_EXCEEDED;
                referenceType = REF_TYPE_EXCEEDED;
                String overFormatted = formatAmount(metrics.spentAmount().subtract(budget.getLimitAmount()));
                title = String.format("Vượt giới hạn ngân sách: %s", categoryName);
                message = String.format(
                        "Chi tiêu danh mục %s tháng %s đã vượt ngân sách %s (%s / %s, đạt %s%%).",
                        categoryName, periodText, overFormatted, spentFormatted, limitFormatted, metrics.usagePercentage()
                );
            }
            default -> {
                return;
            }
        }

        if (!notificationService.existsAlert(userId, budget.getId(), referenceType)) {
            notificationService.createNotification(
                    budget.getUser(),
                    title,
                    message,
                    notificationType,
                    budget.getId(),
                    referenceType
            );
        }
    }

    private String formatAmount(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(amount);
    }
}


package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.entity.Budget;

import java.math.BigDecimal;
import java.time.Instant;

public interface BudgetAlertService {

    void evaluateBudgetAlertForTransaction(String userId, String categoryId, Instant transactionDate);

    void evaluateBudgetAlert(Budget budget, BigDecimal spentAmount);
}


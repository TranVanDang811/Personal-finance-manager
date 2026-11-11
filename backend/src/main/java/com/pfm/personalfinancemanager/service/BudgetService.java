package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.CreateBudgetRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateBudgetRequest;
import com.pfm.personalfinancemanager.dto.response.BudgetResponse;
import com.pfm.personalfinancemanager.dto.response.BudgetSummaryResponse;

import java.util.List;

public interface BudgetService {

    BudgetResponse createBudget(String userId, CreateBudgetRequest request);

    BudgetResponse updateBudget(String userId, String budgetId, UpdateBudgetRequest request);

    void deleteBudget(String userId, String budgetId);

    BudgetResponse getBudgetById(String userId, String budgetId);

    List<BudgetResponse> getBudgets(String userId, Integer month, Integer year);

    BudgetSummaryResponse getBudgetSummary(String userId, int month, int year);
}


package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.CreateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.request.SavingGoalContributionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.SavingGoalContributionResponse;
import com.pfm.personalfinancemanager.dto.response.SavingGoalResponse;
import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SavingGoalService {

    SavingGoalResponse createSavingGoal(String userId, CreateSavingGoalRequest request);

    SavingGoalResponse updateSavingGoal(String userId, String goalId, UpdateSavingGoalRequest request);

    SavingGoalResponse cancelSavingGoal(String userId, String goalId);

    void deleteSavingGoal(String userId, String goalId);

    SavingGoalResponse getSavingGoalById(String userId, String goalId);

    List<SavingGoalResponse> getSavingGoals(String userId, SavingGoalStatus status);

    SavingGoalResponse deposit(String userId, String goalId, SavingGoalContributionRequest request);

    SavingGoalResponse withdraw(String userId, String goalId, SavingGoalContributionRequest request);

    PageResponse<SavingGoalContributionResponse> getContributions(String userId, String goalId, Pageable pageable);
}


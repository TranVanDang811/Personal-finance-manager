package com.pfm.personalfinancemanager.mapper;

import com.pfm.personalfinancemanager.dto.response.SavingGoalContributionResponse;
import com.pfm.personalfinancemanager.entity.SavingGoalContribution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SavingGoalContributionMapper {

    @Mapping(target = "savingGoalId", source = "savingGoal.id")
    @Mapping(target = "savingGoalName", source = "savingGoal.name")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountName", source = "account.accountName")
    @Mapping(target = "transactionId", source = "transaction.id")
    SavingGoalContributionResponse toResponse(SavingGoalContribution contribution);
}


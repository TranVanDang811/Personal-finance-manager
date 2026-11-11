package com.pfm.personalfinancemanager.mapper;

import com.pfm.personalfinancemanager.dto.response.SavingGoalResponse;
import com.pfm.personalfinancemanager.entity.SavingGoal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface SavingGoalMapper {

    @Mapping(target = "userId", source = "goal.user.id")
    @Mapping(target = "remainingAmount", source = "remainingAmount")
    @Mapping(target = "progressPercentage", source = "progressPercentage")
    SavingGoalResponse toResponse(
            SavingGoal goal,
            BigDecimal remainingAmount,
            BigDecimal progressPercentage
    );
}


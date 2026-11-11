package com.pfm.personalfinancemanager.mapper;

import com.pfm.personalfinancemanager.dto.response.BudgetResponse;
import com.pfm.personalfinancemanager.entity.Budget;
import com.pfm.personalfinancemanager.enums.BudgetAlertLevel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface BudgetMapper {

    @Mapping(target = "id", source = "budget.id")
    @Mapping(target = "userId", source = "budget.user.id")
    @Mapping(target = "categoryId", source = "budget.category.id")
    @Mapping(target = "categoryName", source = "budget.category.name")
    @Mapping(target = "categoryIcon", source = "budget.category.icon")
    @Mapping(target = "categoryColor", source = "budget.category.color")
    @Mapping(target = "month", source = "budget.month")
    @Mapping(target = "year", source = "budget.year")
    @Mapping(target = "limitAmount", source = "budget.limitAmount")
    @Mapping(target = "createdAt", source = "budget.createdAt")
    @Mapping(target = "updatedAt", source = "budget.updatedAt")
    @Mapping(target = "spentAmount", source = "spentAmount")
    @Mapping(target = "remainingAmount", source = "remainingAmount")
    @Mapping(target = "usagePercentage", source = "usagePercentage")
    @Mapping(target = "alertLevel", source = "alertLevel")
    BudgetResponse toResponse(
            Budget budget,
            BigDecimal spentAmount,
            BigDecimal remainingAmount,
            BigDecimal usagePercentage,
            BudgetAlertLevel alertLevel
    );
}


package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoalResponse {

    private String id;
    private String userId;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal progressPercentage;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String description;
    private String color;
    private SavingGoalStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}


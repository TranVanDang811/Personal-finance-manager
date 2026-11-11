package com.pfm.personalfinancemanager.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBudgetRequest {

    @NotNull(message = "Hạn mức ngân sách không được để trống")
    @DecimalMin(value = "0.0001", message = "Hạn mức ngân sách phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Số tiền tối đa 15 chữ số phần nguyên và 4 chữ số thập phân")
    private BigDecimal limitAmount;
}


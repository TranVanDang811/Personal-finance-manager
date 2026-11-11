package com.pfm.personalfinancemanager.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoalContributionRequest {

    @NotBlank(message = "Vui lòng chọn tài khoản nguồn/đích")
    private String accountId;

    @NotNull(message = "Số tiền giao dịch không được để trống")
    @DecimalMin(value = "0.0001", message = "Số tiền giao dịch phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Số tiền tối đa 15 chữ số phần nguyên và 4 chữ số thập phân")
    private BigDecimal amount;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String note;

    private Instant contributionDate;
}


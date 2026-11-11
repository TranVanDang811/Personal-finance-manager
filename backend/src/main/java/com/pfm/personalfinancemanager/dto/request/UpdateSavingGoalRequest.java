package com.pfm.personalfinancemanager.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSavingGoalRequest {

    @NotBlank(message = "Tên mục tiêu tiết kiệm không được để trống")
    @Size(max = 150, message = "Tên mục tiêu tối đa 150 ký tự")
    private String name;

    @NotNull(message = "Số tiền mục tiêu không được để trống")
    @DecimalMin(value = "0.0001", message = "Số tiền mục tiêu phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Số tiền tối đa 15 chữ số phần nguyên và 4 chữ số thập phân")
    private BigDecimal targetAmount;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày dự kiến hoàn thành không được để trống")
    private LocalDate targetDate;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Mã màu phải có định dạng hex (#RRGGBB)")
    private String color;
}


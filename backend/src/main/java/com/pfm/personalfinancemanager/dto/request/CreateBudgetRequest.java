package com.pfm.personalfinancemanager.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class CreateBudgetRequest {

    @NotBlank(message = "Danh mục chi tiêu không được để trống")
    private String categoryId;

    @NotNull(message = "Tháng áp dụng không được để trống")
    @Min(value = 1, message = "Tháng phải từ 1 đến 12")
    @Max(value = 12, message = "Tháng phải từ 1 đến 12")
    private Integer month;

    @NotNull(message = "Năm áp dụng không được để trống")
    @Min(value = 2000, message = "Năm không hợp lệ")
    @Max(value = 2100, message = "Năm không hợp lệ")
    private Integer year;

    @NotNull(message = "Hạn mức ngân sách không được để trống")
    @DecimalMin(value = "0.0001", message = "Hạn mức ngân sách phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Số tiền tối đa 15 chữ số phần nguyên và 4 chữ số thập phân")
    private BigDecimal limitAmount;
}


package com.pfm.personalfinancemanager.dto.request;

import com.pfm.personalfinancemanager.enums.AccountType;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(min = 2, max = 100, message = "Tên tài khoản phải từ 2 đến 100 ký tự")
    private String accountName;

    @NotNull(message = "Loại tài khoản không được để trống")
    private AccountType accountType;

    @NotNull(message = "Số dư ban đầu không được để trống")
    @Digits(integer = 15, fraction = 4, message = "Số dư ban đầu không hợp lệ (tối đa 15 chữ số nguyên và 4 chữ số thập phân)")
    private BigDecimal initialBalance;

    @Pattern(regexp = "^(VND|USD|EUR)$", message = "Đơn vị tiền tệ hỗ trợ: VND, USD, EUR")
    @Builder.Default
    private String currency = "VND";

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @Builder.Default
    private Boolean allowNegative = false;
}


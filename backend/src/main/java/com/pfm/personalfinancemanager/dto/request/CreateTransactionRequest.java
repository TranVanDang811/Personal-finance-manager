package com.pfm.personalfinancemanager.dto.request;

import com.pfm.personalfinancemanager.enums.TransactionType;
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
public class CreateTransactionRequest {

    @NotBlank(message = "Tài khoản nguồn không được để trống")
    private String accountId;

    private String toAccountId;

    private String categoryId;

    @NotNull(message = "Loại giao dịch không được để trống")
    private TransactionType transactionType;

    @NotNull(message = "Số tiền giao dịch không được để trống")
    @DecimalMin(value = "0.0001", message = "Số tiền giao dịch phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Số tiền tối đa 15 chữ số phần nguyên và 4 chữ số thập phân")
    private BigDecimal amount;

    @NotBlank(message = "Mô tả giao dịch không được để trống")
    @Size(max = 255, message = "Mô tả giao dịch tối đa 255 ký tự")
    private String description;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    private String note;

    @NotNull(message = "Ngày giao dịch không được để trống")
    private Instant transactionDate;
}


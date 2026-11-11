package com.pfm.personalfinancemanager.dto.request;

import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(min = 2, max = 100, message = "Tên tài khoản phải từ 2 đến 100 ký tự")
    private String accountName;

    @NotNull(message = "Loại tài khoản không được để trống")
    private AccountType accountType;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @NotNull(message = "Trạng thái cho phép số dư âm không được để trống")
    private Boolean allowNegative;

    @NotNull(message = "Trạng thái tài khoản không được để trống")
    private AccountStatus status;
}


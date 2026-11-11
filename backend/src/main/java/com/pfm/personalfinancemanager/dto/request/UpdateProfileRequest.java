package com.pfm.personalfinancemanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự")
    private String fullName;

    @NotBlank(message = "Đơn vị tiền tệ không được để trống")
    @Pattern(regexp = "^(VND|USD|EUR)$", message = "Đơn vị tiền tệ hỗ trợ: VND, USD, EUR")
    private String currency;
}

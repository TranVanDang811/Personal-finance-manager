package com.pfm.personalfinancemanager.dto.request;

import com.pfm.personalfinancemanager.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 2, max = 100, message = "Tên danh mục phải từ 2 đến 100 ký tự")
    private String name;

    @NotNull(message = "Loại danh mục không được để trống")
    private CategoryType categoryType;

    @Size(max = 50, message = "Tên icon tối đa 50 ký tự")
    @Builder.Default
    private String icon = "tag";

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Mã màu phải theo định dạng Hex (ví dụ: #10B981)")
    @Builder.Default
    private String color = "#6B7280";
}


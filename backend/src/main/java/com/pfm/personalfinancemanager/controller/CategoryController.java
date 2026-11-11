package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.request.CreateCategoryRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateCategoryRequest;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.CategoryResponse;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.service.CategoryService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Quản lý danh mục thu nhập và chi tiêu (Hệ thống & Cá nhân)")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lấy danh sách danh mục khả dụng (Hệ thống + Cá nhân)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(required = false) CategoryType type
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategories(userId, type)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một danh mục")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(userId, id)));
    }

    @PostMapping
    @Operation(summary = "Tạo danh mục cá nhân mới")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        CategoryResponse response = categoryService.createCategory(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo danh mục thành công", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật danh mục cá nhân")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        CategoryResponse response = categoryService.updateCategory(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa danh mục cá nhân")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        categoryService.deleteCategory(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công"));
    }
}


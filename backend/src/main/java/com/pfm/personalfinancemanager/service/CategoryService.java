package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.CreateCategoryRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateCategoryRequest;
import com.pfm.personalfinancemanager.dto.response.CategoryResponse;
import com.pfm.personalfinancemanager.enums.CategoryType;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getCategories(String userId, CategoryType type);

    CategoryResponse getCategoryById(String userId, String categoryId);

    CategoryResponse createCategory(String userId, CreateCategoryRequest request);

    CategoryResponse updateCategory(String userId, String categoryId, UpdateCategoryRequest request);

    void deleteCategory(String userId, String categoryId);
}


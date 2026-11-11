package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.annotation.EvictUserReports;
import com.pfm.personalfinancemanager.dto.request.CreateCategoryRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateCategoryRequest;
import com.pfm.personalfinancemanager.dto.response.CategoryResponse;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.exception.DuplicateResourceException;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.CategoryMapper;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#userId + ':' + (#type != null ? #type.name() : 'ALL')")
    public List<CategoryResponse> getCategories(String userId, CategoryType type) {
        return categoryRepository.findAllAvailableForUser(userId, type)
                .stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(String userId, String categoryId) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", categoryId));

        if (!Boolean.TRUE.equals(category.getIsSystem())) {
            if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
                throw new UnauthorizedAccessException("Bạn không có quyền truy cập danh mục này");
            }
        }
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categories", key = "#userId + ':ALL'"),
            @CacheEvict(value = "categories", key = "#userId + ':' + #request.categoryType.name()")
    })
    public CategoryResponse createCategory(String userId, CreateCategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String normalizedName = request.getName().trim();
        if (categoryRepository.existsDuplicateNameForUser(userId, request.getCategoryType(), normalizedName)) {
            throw new DuplicateResourceException("Danh mục", "tên", normalizedName);
        }

        Category category = Category.builder()
                .user(user)
                .name(normalizedName)
                .categoryType(request.getCategoryType())
                .icon(StringUtils.hasText(request.getIcon()) ? request.getIcon().trim() : "tag")
                .color(StringUtils.hasText(request.getColor()) ? request.getColor().trim() : "#6B7280")
                .isSystem(false)
                .isDeleted(false)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Created custom category {} ({}) for user {}", saved.getId(), saved.getCategoryType(), userId);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @EvictUserReports
    @Caching(evict = {
            @CacheEvict(value = "categories", key = "#userId + ':ALL'"),
            @CacheEvict(value = "categories", key = "#userId + ':INCOME'"),
            @CacheEvict(value = "categories", key = "#userId + ':EXPENSE'")
    })
    public CategoryResponse updateCategory(String userId, String categoryId, UpdateCategoryRequest request) {
        Category category = findOwnedMutableCategoryOrThrow(userId, categoryId);

        String normalizedName = request.getName().trim();
        if (categoryRepository.existsDuplicateNameForUserExcludingId(
                userId, category.getCategoryType(), normalizedName, categoryId)) {
            throw new DuplicateResourceException("Danh mục", "tên", normalizedName);
        }

        category.setName(normalizedName);
        if (StringUtils.hasText(request.getIcon())) {
            category.setIcon(request.getIcon().trim());
        }
        if (StringUtils.hasText(request.getColor())) {
            category.setColor(request.getColor().trim());
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @EvictUserReports
    @Caching(evict = {
            @CacheEvict(value = "categories", key = "#userId + ':ALL'"),
            @CacheEvict(value = "categories", key = "#userId + ':INCOME'"),
            @CacheEvict(value = "categories", key = "#userId + ':EXPENSE'")
    })
    public void deleteCategory(String userId, String categoryId) {
        Category category = findOwnedMutableCategoryOrThrow(userId, categoryId);
        category.setIsDeleted(true);
        category.setDeletedAt(Instant.now());
        categoryRepository.save(category);
        log.info("Soft deleted custom category {} for user {}", categoryId, userId);
    }

    private Category findOwnedMutableCategoryOrThrow(String userId, String categoryId) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", categoryId));

        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw new UnauthorizedAccessException("Không được phép chỉnh sửa hoặc xóa danh mục hệ thống");
        }
        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền thao tác trên danh mục này");
        }
        return category;
    }
}


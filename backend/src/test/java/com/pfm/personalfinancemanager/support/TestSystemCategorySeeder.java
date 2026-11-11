package com.pfm.personalfinancemanager.support;

import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestSystemCategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<Category> systemCategories = List.of(
                systemCategory("00000000-0000-0000-0001-000000000001", "Salary", CategoryType.INCOME, "banknotes", "#10B981"),
                systemCategory("00000000-0000-0000-0001-000000000002", "Bonus", CategoryType.INCOME, "gift", "#059669"),
                systemCategory("00000000-0000-0000-0001-000000000003", "Investment", CategoryType.INCOME, "chart-bar", "#047857"),
                systemCategory("00000000-0000-0000-0001-000000000004", "Business", CategoryType.INCOME, "building-office-2", "#065F46"),
                systemCategory("00000000-0000-0000-0001-000000000005", "Other Income", CategoryType.INCOME, "plus-circle", "#34D399"),

                systemCategory("00000000-0000-0000-0002-000000000001", "Food & Drink", CategoryType.EXPENSE, "cake", "#EF4444"),
                systemCategory("00000000-0000-0000-0002-000000000002", "Transportation", CategoryType.EXPENSE, "truck", "#F97316"),
                systemCategory("00000000-0000-0000-0002-000000000003", "Shopping", CategoryType.EXPENSE, "shopping-bag", "#EC4899"),
                systemCategory("00000000-0000-0000-0002-000000000004", "Entertainment", CategoryType.EXPENSE, "film", "#8B5CF6"),
                systemCategory("00000000-0000-0000-0002-000000000005", "Healthcare", CategoryType.EXPENSE, "heart", "#06B6D4"),
                systemCategory("00000000-0000-0000-0002-000000000006", "Education", CategoryType.EXPENSE, "academic-cap", "#3B82F6"),
                systemCategory("00000000-0000-0000-0002-000000000007", "Housing", CategoryType.EXPENSE, "home", "#6366F1"),
                systemCategory("00000000-0000-0000-0002-000000000008", "Utilities", CategoryType.EXPENSE, "bolt", "#F59E0B"),
                systemCategory("00000000-0000-0000-0002-000000000009", "Travel", CategoryType.EXPENSE, "globe-americas", "#14B8A6"),
                systemCategory("00000000-0000-0000-0002-000000000010", "Other Expense", CategoryType.EXPENSE, "minus-circle", "#6B7280")
        );

        categoryRepository.saveAll(systemCategories);
    }

    private Category systemCategory(String id, String name, CategoryType type, String icon, String color) {
        return Category.builder()
                .id(id)
                .user(null)
                .name(name)
                .categoryType(type)
                .icon(icon)
                .color(color)
                .isSystem(true)
                .isDeleted(false)
                .build();
    }
}


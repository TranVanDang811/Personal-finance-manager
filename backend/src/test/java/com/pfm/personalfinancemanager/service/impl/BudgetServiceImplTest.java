package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.CreateBudgetRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateBudgetRequest;
import com.pfm.personalfinancemanager.dto.response.BudgetResponse;
import com.pfm.personalfinancemanager.dto.response.BudgetSummaryResponse;
import com.pfm.personalfinancemanager.entity.Budget;
import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.BudgetAlertLevel;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.DuplicateResourceException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.BudgetMapper;
import com.pfm.personalfinancemanager.repository.BudgetRepository;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository.CategoryExpenseSum;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.BudgetAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BudgetAlertService budgetAlertService;

    private BudgetServiceImpl budgetService;

    private User userA;
    private User userB;
    private Category foodCategory;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        BudgetMapper mapper = Mappers.getMapper(BudgetMapper.class);
        budgetService = new BudgetServiceImpl(
                budgetRepository,
                categoryRepository,
                transactionRepository,
                userRepository,
                mapper,
                budgetAlertService
        );

        userA = User.builder().id("user-a").email("a@pfm.local").fullName("Alice").password("hash").build();
        userB = User.builder().id("user-b").email("b@pfm.local").fullName("Bob").password("hash").build();

        foodCategory = Category.builder()
                .id("cat-food")
                .name("Food & Drink")
                .categoryType(CategoryType.EXPENSE)
                .icon("cake")
                .color("#EF4444")
                .isSystem(true)
                .isDeleted(false)
                .build();

        salaryCategory = Category.builder()
                .id("cat-salary")
                .name("Salary")
                .categoryType(CategoryType.INCOME)
                .isSystem(true)
                .isDeleted(false)
                .build();
    }

    @Test
    void createBudget_WithExistingSpendingAtWarningThreshold_ShouldEvaluateAlertAndComputeMetrics() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));
        when(categoryRepository.findByIdAndIsDeletedFalse("cat-food")).thenReturn(Optional.of(foodCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear("user-a", "cat-food", 11, 2026))
                .thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId("budget-1");
            return b;
        });
        when(transactionRepository.sumExpenseAmountByUserAndCategoryAndPeriod(
                eq("user-a"), eq("cat-food"), any(), any()
        )).thenReturn(new BigDecimal("850000.0000"));

        CreateBudgetRequest req = CreateBudgetRequest.builder()
                .categoryId("cat-food")
                .month(11)
                .year(2026)
                .limitAmount(new BigDecimal("1000000.0000"))
                .build();

        BudgetResponse resp = budgetService.createBudget("user-a", req);

        assertThat(resp.getId()).isEqualTo("budget-1");
        assertThat(resp.getSpentAmount()).isEqualByComparingTo("850000.0000");
        assertThat(resp.getRemainingAmount()).isEqualByComparingTo("150000.0000");
        assertThat(resp.getUsagePercentage()).isEqualByComparingTo("85.00");
        assertThat(resp.getAlertLevel()).isEqualTo(BudgetAlertLevel.WARNING);
        verify(budgetAlertService).evaluateBudgetAlert(any(Budget.class), eq(new BigDecimal("850000.0000")));
    }

    @Test
    void createBudget_IncomeCategory_ShouldThrowCategoryTypeMismatch() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));
        when(categoryRepository.findByIdAndIsDeletedFalse("cat-salary")).thenReturn(Optional.of(salaryCategory));

        CreateBudgetRequest req = CreateBudgetRequest.builder()
                .categoryId("cat-salary")
                .month(11)
                .year(2026)
                .limitAmount(new BigDecimal("1000000"))
                .build();

        assertThatThrownBy(() -> budgetService.createBudget("user-a", req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_TYPE_MISMATCH);
    }

    @Test
    void createBudget_DuplicatePeriodAndCategory_ShouldThrowDuplicateResource() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));
        when(categoryRepository.findByIdAndIsDeletedFalse("cat-food")).thenReturn(Optional.of(foodCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear("user-a", "cat-food", 11, 2026))
                .thenReturn(true);

        CreateBudgetRequest req = CreateBudgetRequest.builder()
                .categoryId("cat-food")
                .month(11)
                .year(2026)
                .limitAmount(new BigDecimal("1000000"))
                .build();

        assertThatThrownBy(() -> budgetService.createBudget("user-a", req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateBudget_LoweringLimitBelowSpent_ShouldTriggerExceededAlert() {
        Budget existing = Budget.builder()
                .id("budget-1")
                .user(userA)
                .category(foodCategory)
                .month(11)
                .year(2026)
                .limitAmount(new BigDecimal("2000000.0000"))
                .build();

        when(budgetRepository.findByIdWithDetails("budget-1")).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.sumExpenseAmountByUserAndCategoryAndPeriod(
                eq("user-a"), eq("cat-food"), any(), any()
        )).thenReturn(new BigDecimal("1200000.0000"));

        BudgetResponse updated = budgetService.updateBudget(
                "user-a", "budget-1",
                UpdateBudgetRequest.builder().limitAmount(new BigDecimal("1000000.0000")).build()
        );

        assertThat(updated.getAlertLevel()).isEqualTo(BudgetAlertLevel.EXCEEDED);
        assertThat(updated.getUsagePercentage()).isEqualByComparingTo("120.00");
        assertThat(updated.getRemainingAmount()).isEqualByComparingTo("-200000.0000");
        verify(budgetAlertService).evaluateBudgetAlert(existing, new BigDecimal("1200000.0000"));
    }

    @Test
    void getBudgetSummary_ShouldAggregateAllBudgetsAndAlertCounts() {
        Category transport = Category.builder()
                .id("cat-trans")
                .name("Transportation")
                .categoryType(CategoryType.EXPENSE)
                .build();

        Budget bFood = Budget.builder()
                .id("b1").user(userA).category(foodCategory).month(11).year(2026)
                .limitAmount(new BigDecimal("1000000")).build();
        Budget bTrans = Budget.builder()
                .id("b2").user(userA).category(transport).month(11).year(2026)
                .limitAmount(new BigDecimal("500000")).build();

        when(budgetRepository.findAllByUserIdAndPeriod("user-a", 11, 2026)).thenReturn(List.of(bFood, bTrans));

        CategoryExpenseSum foodSum = projection("cat-food", new BigDecimal("850000"));
        CategoryExpenseSum transSum = projection("cat-trans", new BigDecimal("500000"));
        when(transactionRepository.sumExpensesGroupedByCategoryForPeriod(eq("user-a"), any(), any()))
                .thenReturn(List.of(foodSum, transSum));

        BudgetSummaryResponse summary = budgetService.getBudgetSummary("user-a", 11, 2026);

        assertThat(summary.getTotalLimitAmount()).isEqualByComparingTo("1500000");
        assertThat(summary.getTotalSpentAmount()).isEqualByComparingTo("1350000");
        assertThat(summary.getTotalRemainingAmount()).isEqualByComparingTo("150000");
        assertThat(summary.getOverallUsagePercentage()).isEqualByComparingTo("90.00");
        assertThat(summary.getWarningCount()).isEqualTo(1);
        assertThat(summary.getReachedCount()).isEqualTo(1);
        assertThat(summary.getExceededCount()).isEqualTo(0);
    }

    @Test
    void getBudgetById_ForeignUser_ShouldThrowUnauthorized() {
        Budget existing = Budget.builder().id("b1").user(userA).category(foodCategory).month(11).year(2026)
                .limitAmount(new BigDecimal("1000000")).build();
        when(budgetRepository.findByIdWithDetails("b1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> budgetService.getBudgetById(userB.getId(), "b1"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    private CategoryExpenseSum projection(String categoryId, BigDecimal amount) {
        return new CategoryExpenseSum() {
            @Override
            public String getCategoryId() {
                return categoryId;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return amount;
            }
        };
    }
}


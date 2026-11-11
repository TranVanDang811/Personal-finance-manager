package com.pfm.personalfinancemanager.repository;

import com.pfm.personalfinancemanager.entity.Transaction;
import com.pfm.personalfinancemanager.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String>, JpaSpecificationExecutor<Transaction> {

    @Override
    @EntityGraph(attributePaths = {"account", "toAccount", "category", "user"})
    Page<Transaction> findAll(@Nullable Specification<Transaction> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "toAccount", "category", "user"})
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithDetails(@Param("id") String id);

    @EntityGraph(attributePaths = {"account", "toAccount", "category", "user"})
    @Query("""
            SELECT t FROM Transaction t
            LEFT JOIN t.toAccount ta
            WHERE t.user.id = :userId
              AND (t.account.id = :accountId OR ta.id = :accountId)
            """)
    Page<Transaction> findAllByUserIdAndInvolvingAccountId(
            @Param("userId") String userId,
            @Param("accountId") String accountId,
            Pageable pageable
    );

    boolean existsByCategoryId(String categoryId);

    boolean existsByAccountIdOrToAccountId(String accountId, String toAccountId);

    @Query("""
            SELECT COUNT(t) > 0
            FROM Transaction t
            WHERE t.recurringTransactionId = :recurringId
              AND t.transactionDate >= :dayStart
              AND t.transactionDate < :nextDayStart
            """)
    boolean existsRecurringExecutionOnDay(
            @Param("recurringId") String recurringId,
            @Param("dayStart") Instant dayStart,
            @Param("nextDayStart") Instant nextDayStart
    );

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Transaction t SET t.recurringTransactionId = NULL WHERE t.recurringTransactionId = :recurringId")
    void clearRecurringTransactionReference(@Param("recurringId") String recurringId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.category.id = :categoryId
              AND t.transactionType = com.pfm.personalfinancemanager.enums.TransactionType.EXPENSE
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
            """)
    BigDecimal sumExpenseAmountByUserAndCategoryAndPeriod(
            @Param("userId") String userId,
            @Param("categoryId") String categoryId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    interface CategoryExpenseSum {
        String getCategoryId();
        BigDecimal getTotalAmount();
    }

    @Query("""
            SELECT t.category.id AS categoryId, COALESCE(SUM(t.amount), 0) AS totalAmount
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.category IS NOT NULL
              AND t.transactionType = com.pfm.personalfinancemanager.enums.TransactionType.EXPENSE
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
            GROUP BY t.category.id
            """)
    List<CategoryExpenseSum> sumExpensesGroupedByCategoryForPeriod(
            @Param("userId") String userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    interface TransactionTypeSum {
        TransactionType getTransactionType();
        BigDecimal getTotalAmount();
    }

    @Query("""
            SELECT t.transactionType AS transactionType, COALESCE(SUM(t.amount), 0) AS totalAmount
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
            GROUP BY t.transactionType
            """)
    List<TransactionTypeSum> sumAmountsByTypeForPeriod(
            @Param("userId") String userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    interface CategoryBreakdownProjection {
        String getCategoryId();
        String getCategoryName();
        String getCategoryIcon();
        String getCategoryColor();
        BigDecimal getTotalAmount();
        Long getTransactionCount();
    }

    @Query("""
            SELECT t.category.id AS categoryId,
                   t.category.name AS categoryName,
                   t.category.icon AS categoryIcon,
                   t.category.color AS categoryColor,
                   COALESCE(SUM(t.amount), 0) AS totalAmount,
                   COUNT(t) AS transactionCount
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.category IS NOT NULL
              AND t.transactionType = :transactionType
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
            GROUP BY t.category.id, t.category.name, t.category.icon, t.category.color
            ORDER BY COALESCE(SUM(t.amount), 0) DESC, t.category.name ASC
            """)
    List<CategoryBreakdownProjection> sumCategoryBreakdownForPeriod(
            @Param("userId") String userId,
            @Param("transactionType") TransactionType transactionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    interface CashflowEntryProjection {
        TransactionType getTransactionType();
        BigDecimal getAmount();
        Instant getTransactionDate();
    }

    @Query("""
            SELECT t.transactionType AS transactionType,
                   t.amount AS amount,
                   t.transactionDate AS transactionDate
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.transactionType IN (
                  com.pfm.personalfinancemanager.enums.TransactionType.INCOME,
                  com.pfm.personalfinancemanager.enums.TransactionType.EXPENSE
              )
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
            ORDER BY t.transactionDate ASC
            """)
    List<CashflowEntryProjection> findCashflowEntriesForPeriod(
            @Param("userId") String userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}

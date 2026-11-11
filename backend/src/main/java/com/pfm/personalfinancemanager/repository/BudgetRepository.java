package com.pfm.personalfinancemanager.repository;

import com.pfm.personalfinancemanager.entity.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {

    @EntityGraph(attributePaths = {"category", "user"})
    @Query("SELECT b FROM Budget b WHERE b.id = :id")
    Optional<Budget> findByIdWithDetails(@Param("id") String id);

    @EntityGraph(attributePaths = {"category", "user"})
    @Query("""
            SELECT b FROM Budget b
            WHERE b.user.id = :userId
              AND (:month IS NULL OR b.month = :month)
              AND (:year IS NULL OR b.year = :year)
            ORDER BY b.year DESC, b.month DESC, b.createdAt DESC
            """)
    List<Budget> findAllByUserIdAndPeriod(
            @Param("userId") String userId,
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    @EntityGraph(attributePaths = {"category", "user"})
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(
            String userId,
            String categoryId,
            Integer month,
            Integer year
    );

    boolean existsByUserIdAndCategoryIdAndMonthAndYear(
            String userId,
            String categoryId,
            Integer month,
            Integer year
    );
}


package com.pfm.personalfinancemanager.repository;

import com.pfm.personalfinancemanager.entity.SavingGoal;
import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavingGoalRepository extends JpaRepository<SavingGoal, String> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT g FROM SavingGoal g WHERE g.id = :id")
    Optional<SavingGoal> findByIdWithUser(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM SavingGoal g WHERE g.id = :id")
    Optional<SavingGoal> findByIdForUpdate(@Param("id") String id);

    @Query("""
            SELECT g FROM SavingGoal g
            WHERE g.user.id = :userId
              AND (:status IS NULL OR g.status = :status)
            ORDER BY g.status ASC, g.targetDate ASC, g.createdAt DESC
            """)
    List<SavingGoal> findAllByUserIdAndStatus(
            @Param("userId") String userId,
            @Param("status") SavingGoalStatus status
    );

    interface SavingGoalAggregateProjection {
        SavingGoalStatus getStatus();
        BigDecimal getTotalCurrentAmount();
        BigDecimal getTotalTargetAmount();
        Long getGoalCount();
    }

    @Query("""
            SELECT g.status AS status,
                   COALESCE(SUM(g.currentAmount), 0) AS totalCurrentAmount,
                   COALESCE(SUM(g.targetAmount), 0) AS totalTargetAmount,
                   COUNT(g) AS goalCount
            FROM SavingGoal g
            WHERE g.user.id = :userId
            GROUP BY g.status
            """)
    List<SavingGoalAggregateProjection> aggregateGoalsByStatusForUser(@Param("userId") String userId);
}


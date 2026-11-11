package com.pfm.personalfinancemanager.repository;

import com.pfm.personalfinancemanager.entity.RecurringTransaction;
import com.pfm.personalfinancemanager.enums.RecurringStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, String> {

    @EntityGraph(attributePaths = {"user", "account", "toAccount", "category"})
    @Query("SELECT r FROM RecurringTransaction r WHERE r.id = :id")
    Optional<RecurringTransaction> findByIdWithDetails(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecurringTransaction r WHERE r.id = :id")
    Optional<RecurringTransaction> findByIdForUpdate(@Param("id") String id);

    @EntityGraph(attributePaths = {"user", "account", "toAccount", "category"})
    @Query("""
            SELECT r FROM RecurringTransaction r
            WHERE r.user.id = :userId
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.status ASC, r.nextExecutionDate ASC, r.createdAt DESC
            """)
    List<RecurringTransaction> findAllByUserIdAndStatus(
            @Param("userId") String userId,
            @Param("status") RecurringStatus status
    );

    @Query("""
            SELECT r.id FROM RecurringTransaction r
            WHERE r.status = com.pfm.personalfinancemanager.enums.RecurringStatus.ACTIVE
              AND r.startDate <= :executionDate
              AND r.nextExecutionDate <= :executionDate
              AND (r.endDate IS NULL OR r.nextExecutionDate <= r.endDate)
              AND (r.endDate IS NULL OR r.endDate >= :executionDate)
            ORDER BY r.nextExecutionDate ASC, r.createdAt ASC
            """)
    List<String> findDueRecurringIds(@Param("executionDate") LocalDate executionDate);
}


package com.pfm.personalfinancemanager.repository;

import com.pfm.personalfinancemanager.entity.SavingGoalContribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SavingGoalContributionRepository extends JpaRepository<SavingGoalContribution, String> {

    @EntityGraph(attributePaths = {"savingGoal", "account", "transaction"})
    @Query("""
            SELECT c FROM SavingGoalContribution c
            WHERE c.savingGoal.id = :savingGoalId
              AND c.user.id = :userId
            ORDER BY c.contributionDate DESC
            """)
    Page<SavingGoalContribution> findBySavingGoalIdAndUserId(
            @Param("savingGoalId") String savingGoalId,
            @Param("userId") String userId,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM SavingGoalContribution c WHERE c.savingGoal.id = :savingGoalId")
    void deleteAllBySavingGoalId(@Param("savingGoalId") String savingGoalId);
}

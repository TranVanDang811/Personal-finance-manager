package com.pfm.personalfinancemanager.repository;

import com.pfm.personalfinancemanager.entity.Account;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
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
public interface AccountRepository extends JpaRepository<Account, String> {

    @Query("""
            SELECT a FROM Account a
            WHERE a.user.id = :userId
              AND a.isDeleted = false
              AND (:status IS NULL OR a.status = :status)
              AND (:accountType IS NULL OR a.accountType = :accountType)
            ORDER BY a.createdAt DESC
            """)
    List<Account> findAllByUserWithFilters(
            @Param("userId") String userId,
            @Param("status") AccountStatus status,
            @Param("accountType") AccountType accountType
    );

    @EntityGraph(attributePaths = {"user"})
    Optional<Account> findByIdAndIsDeletedFalse(String id);

    Optional<Account> findByIdAndUserIdAndIsDeletedFalse(String id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.user.id = :userId AND a.isDeleted = false")
    Optional<Account> findByIdAndUserIdForUpdate(@Param("id") String id, @Param("userId") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.isDeleted = false")
    Optional<Account> findByIdForUpdate(@Param("id") String id);

    boolean existsByUserIdAndAccountNameIgnoreCaseAndIsDeletedFalse(String userId, String accountName);

    boolean existsByUserIdAndAccountNameIgnoreCaseAndIdNotAndIsDeletedFalse(
            String userId,
            String accountName,
            String id
    );

    @Query("""
            SELECT COALESCE(SUM(a.currentBalance), 0)
            FROM Account a
            WHERE a.user.id = :userId
              AND a.isDeleted = false
              AND a.status = :status
            """)
    BigDecimal sumBalanceByUserIdAndStatus(
            @Param("userId") String userId,
            @Param("status") AccountStatus status
    );

    long countByUserIdAndStatusAndIsDeletedFalse(String userId, AccountStatus status);
}


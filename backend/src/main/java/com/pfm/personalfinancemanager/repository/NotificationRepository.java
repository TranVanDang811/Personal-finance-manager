package com.pfm.personalfinancemanager.repository;

import com.pfm.personalfinancemanager.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    @Query("SELECT n FROM Notification n JOIN FETCH n.user WHERE n.id = :id")
    Optional<Notification> findByIdWithUser(@Param("id") String id);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(String userId, Boolean isRead, Pageable pageable);

    long countByUserIdAndIsReadFalse(String userId);

    boolean existsByUserIdAndReferenceIdAndReferenceType(String userId, String referenceId, String referenceType);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") String userId);
}


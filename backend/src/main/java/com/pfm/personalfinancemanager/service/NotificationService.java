package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.response.NotificationResponse;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.UnreadCountResponse;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse createNotification(
            User user,
            String title,
            String message,
            NotificationType type,
            String referenceId,
            String referenceType
    );

    boolean existsAlert(String userId, String referenceId, String referenceType);

    PageResponse<NotificationResponse> getNotifications(String userId, Boolean isRead, Pageable pageable);

    UnreadCountResponse getUnreadCount(String userId);

    NotificationResponse markAsRead(String userId, String notificationId);

    void markAllAsRead(String userId);

    void deleteNotification(String userId, String notificationId);
}


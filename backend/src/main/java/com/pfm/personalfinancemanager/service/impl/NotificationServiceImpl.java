package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.response.NotificationResponse;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.UnreadCountResponse;
import com.pfm.personalfinancemanager.entity.Notification;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.NotificationType;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.NotificationMapper;
import com.pfm.personalfinancemanager.repository.NotificationRepository;
import com.pfm.personalfinancemanager.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public NotificationResponse createNotification(
            User user,
            String title,
            String message,
            NotificationType type,
            String referenceId,
            String referenceType
    ) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created {} notification {} for user {}", type, saved.getId(), user.getId());
        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAlert(String userId, String referenceId, String referenceType) {
        return notificationRepository.existsByUserIdAndReferenceIdAndReferenceType(userId, referenceId, referenceType);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String userId, Boolean isRead, Pageable pageable) {
        Page<Notification> page = (isRead == null)
                ? notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        return PageResponse.from(page.map(notificationMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(String userId) {
        return UnreadCountResponse.builder()
                .unreadCount(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String userId, String notificationId) {
        Notification notification = findOwnedNotificationOrThrow(userId, notificationId);
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification = notificationRepository.save(notification);
        }
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for user {}", updated, userId);
    }

    @Override
    @Transactional
    public void deleteNotification(String userId, String notificationId) {
        Notification notification = findOwnedNotificationOrThrow(userId, notificationId);
        notificationRepository.delete(notification);
    }

    private Notification findOwnedNotificationOrThrow(String userId, String notificationId) {
        Notification notification = notificationRepository.findByIdWithUser(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền truy cập thông báo này");
        }
        return notification;
    }
}


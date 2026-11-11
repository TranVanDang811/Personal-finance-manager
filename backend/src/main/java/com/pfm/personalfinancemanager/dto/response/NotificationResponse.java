package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType notificationType;
    private String referenceId;
    private String referenceType;
    private Boolean isRead;
    private Instant createdAt;
}


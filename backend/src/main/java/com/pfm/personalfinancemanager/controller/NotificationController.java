package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.NotificationResponse;
import com.pfm.personalfinancemanager.dto.response.PageResponse;
import com.pfm.personalfinancemanager.dto.response.UnreadCountResponse;
import com.pfm.personalfinancemanager.service.NotificationService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Quản lý thông báo và cảnh báo tài chính")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo có phân trang (hỗ trợ lọc theo trạng thái đã đọc)")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(userId, isRead, pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Đếm số thông báo chưa đọc")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo là đã đọc")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Đã đánh dấu thông báo đã đọc",
                notificationService.markAsRead(userId, id)
        ));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo của người dùng là đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        String userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo đã đọc"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thông báo")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        notificationService.deleteNotification(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thông báo thành công"));
    }
}


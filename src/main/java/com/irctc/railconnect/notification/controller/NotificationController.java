package com.irctc.railconnect.notification.controller;

import com.irctc.railconnect.common.response.ApiResponse;
import com.irctc.railconnect.notification.dto.NotificationResponse;
import com.irctc.railconnect.notification.service.NotificationService;
import com.irctc.railconnect.user.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        List<NotificationResponse> notifications = notificationService.getUserNotifications(user.userId());
        return ResponseEntity.ok(ApiResponse.ok("User notifications retrieved successfully", notifications));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String notificationId
    ) {
        NotificationResponse response = notificationService.markAsRead(user.userId(), notificationId);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", response));
    }
}

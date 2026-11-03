package org.group3.tutorlink.features.notification.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.notification.dto.response.NotificationResponse;
import org.group3.tutorlink.features.notification.enums.NotificationResponseCode;
import org.group3.tutorlink.features.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // GET /v1/notifications?cursor=&limit= - đã đăng nhập
    @GetMapping
    public ApiResponse<CursorResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.<CursorResponse<NotificationResponse>>builder()
                .code(NotificationResponseCode.GET_NOTIFICATIONS.getCode())
                .message(NotificationResponseCode.GET_NOTIFICATIONS.getMessage())
                .data(notificationService.getMyNotifications(cursor, limit))
                .build();
    }

    // PATCH /v1/notifications/{id}/read - đã đăng nhập
    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable UUID id) {
        return ApiResponse.<NotificationResponse>builder()
                .code(NotificationResponseCode.NOTIFICATION_MARKED_READ.getCode())
                .message(NotificationResponseCode.NOTIFICATION_MARKED_READ.getMessage())
                .data(notificationService.markAsRead(id))
                .build();
    }

    // PATCH /v1/notifications/read-all - đã đăng nhập
    @PatchMapping("/read-all")
    public ApiResponse<Integer> markAllAsRead() {
        return ApiResponse.<Integer>builder()
                .code(NotificationResponseCode.ALL_NOTIFICATIONS_MARKED_READ.getCode())
                .message(NotificationResponseCode.ALL_NOTIFICATIONS_MARKED_READ.getMessage())
                .data(notificationService.markAllAsRead())
                .build();
    }
}

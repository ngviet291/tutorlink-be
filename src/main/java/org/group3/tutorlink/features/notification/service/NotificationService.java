package org.group3.tutorlink.features.notification.service;

import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.notification.dto.response.NotificationResponse;

import java.util.UUID;

public interface NotificationService {
    CursorResponse<NotificationResponse> getMyNotifications(UUID cursor, int limit);

    NotificationResponse markAsRead(UUID notificationId);

    int markAllAsRead();
}

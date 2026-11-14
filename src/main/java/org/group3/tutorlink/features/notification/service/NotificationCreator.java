package org.group3.tutorlink.features.notification.service;

import org.group3.tutorlink.features.notification.dto.response.NotificationResponse;
import org.group3.tutorlink.features.notification.enums.NotificationType;

import java.util.UUID;

/**
 * Chỉ dành cho service khác gọi. Không được inject vào controller.
 */
public interface NotificationCreator {

    NotificationResponse createNotification(
            UUID recipientId,
            NotificationType notificationType,
            String title,
            String content
    );
}
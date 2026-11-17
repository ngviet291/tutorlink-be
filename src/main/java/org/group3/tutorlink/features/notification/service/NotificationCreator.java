package org.group3.tutorlink.features.notification.service;

import org.group3.tutorlink.features.notification.dto.response.NotificationResponse;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
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

    /**
     * Tạo thông báo có đích đến để FE điều hướng khi click.
     *
     * @param targetType loại đích (null = NONE)
     * @param targetId   id thực thể đích, có thể null
     */
    NotificationResponse createNotification(
            UUID recipientId,
            NotificationType notificationType,
            String title,
            String content,
            NotificationTarget targetType,
            UUID targetId
    );
}

package org.group3.tutorlink.features.notification.enums;

import lombok.Getter;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
public enum NotificationResponseCode implements ResponseCode {
    GET_NOTIFICATIONS("NOTI_200", "Notifications retrieved successfully"),
    NOTIFICATION_MARKED_READ("NOTI_201", "Notification marked as read"),
    ALL_NOTIFICATIONS_MARKED_READ("NOTI_202", "All notifications marked as read");

    private final String code;
    private final String message;

    NotificationResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}

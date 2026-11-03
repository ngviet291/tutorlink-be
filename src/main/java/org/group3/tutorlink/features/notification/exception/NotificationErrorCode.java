package org.group3.tutorlink.features.notification.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode implements BaseErrorCode {
    NOTIFICATION_NOT_FOUND("NOTI_404", "Notification not found", HttpStatus.NOT_FOUND),
    NOTIFICATION_ACCESS_DENIED("NOTI_403", "You do not have permission to access this notification", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    NotificationErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

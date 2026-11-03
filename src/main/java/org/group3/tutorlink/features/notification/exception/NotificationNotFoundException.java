package org.group3.tutorlink.features.notification.exception;

import org.group3.tutorlink.common.exception.AppException;

public class NotificationNotFoundException extends AppException {
    public NotificationNotFoundException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }
}

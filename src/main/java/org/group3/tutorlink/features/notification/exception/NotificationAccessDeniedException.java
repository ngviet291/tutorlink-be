package org.group3.tutorlink.features.notification.exception;

import org.group3.tutorlink.common.exception.AppException;

public class NotificationAccessDeniedException extends AppException {
    public NotificationAccessDeniedException() {
        super(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
    }
}

package org.group3.tutorlink.features.application.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ApplicationInvalidStatusException extends AppException {
    public ApplicationInvalidStatusException() {
        super(ErrorCodeApplication.INVALID_APPLICATION_STATUS);
    }
}

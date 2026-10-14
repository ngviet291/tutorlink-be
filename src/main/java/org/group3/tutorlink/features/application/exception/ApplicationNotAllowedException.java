package org.group3.tutorlink.features.application.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ApplicationNotAllowedException extends AppException {

    public ApplicationNotAllowedException() {
        super(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
    }
}
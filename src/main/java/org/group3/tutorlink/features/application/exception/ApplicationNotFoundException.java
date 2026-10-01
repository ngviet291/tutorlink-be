package org.group3.tutorlink.features.application.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ApplicationNotFoundException extends AppException {
    public ApplicationNotFoundException() {
        super(ApplicationStatusException.APPLICATION_NOT_FOUND);
    }
}

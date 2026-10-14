package org.group3.tutorlink.common.exception;

public class AccessDeniedException extends AppException{
    public AccessDeniedException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

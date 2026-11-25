package org.group3.tutorlink.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final BaseErrorCode errorCode;
    private final String field;

    public AppException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.field = null;
    }

    public AppException(BaseErrorCode errorCode, String field) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.field = field;
    }
}
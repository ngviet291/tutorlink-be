package org.group3.tutorlink.features.application.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum ApplicationStatusException implements BaseErrorCode {

    APPLICATION_NOT_FOUND("APPLICATION_001", "Application not found", HttpStatus.NOT_FOUND),
    APPLICATION_ALREADY_EXISTS("APPLICATION_002", "You have already applied to this post", HttpStatus.CONFLICT),
    APPLICATION_NOT_ALLOWED("APPLICATION_003", "You are not allowed to perform this action", HttpStatus.FORBIDDEN),
    APPLICATION_DEADLINE_EXPIRED("APPLICATION_004", "The application deadline has expired", HttpStatus.BAD_REQUEST),
    INVALID_APPLICATION_STATUS("APPLICATION_005", "Invalid application status for this action", HttpStatus.BAD_REQUEST)
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ApplicationStatusException(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

package org.group3.tutorlink.features.user.exception;

import lombok.*;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum UserErrorCode implements BaseErrorCode {
    USER_IS_NOT_TUTOR("USER_ERR_001", "Only users with the Tutor role can have their profile reviewed", HttpStatus.BAD_REQUEST),
    PUBLIC_PROFILE_NOT_AVAILABLE("USER_ERR_002",  "The tutor profile has not been approved or is not publicly visible", HttpStatus.BAD_REQUEST),
    CANNOT_LOCK_ADMIN("USER_ERR_003",  "Admin accounts cannot be locked", HttpStatus.BAD_REQUEST),
    USER_VALIDATION_FAILED("USER_ERR_004",  "Invalid data", HttpStatus.BAD_REQUEST),
    ;
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    UserErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

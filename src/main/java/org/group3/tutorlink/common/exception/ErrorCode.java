package org.group3.tutorlink.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    UNAUTHENTICATED("AUTH_001", "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("AUTH_002", "You do not have permission to perform this action", HttpStatus.FORBIDDEN),
    GENERATE_TOKEN_FAILED("AUTH_003", "Failed to generate token", HttpStatus.INTERNAL_SERVER_ERROR),
    INTROSPECT_FAILED("AUTH_004", "Invalid or expired token", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("USER_001", "User not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("USER_002", "Email is already taken", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("USER_003", "Email or password is incorrect", HttpStatus.UNAUTHORIZED),
    ACCOUNT_BANNED("USER_004", "Your account has been banned", HttpStatus.FORBIDDEN),
    ROLE_NOT_FOUND("ROLE_001", "Role not found", HttpStatus.NOT_FOUND),
    SUBJECT_NOT_FOUND("SUBJECT_001", "Subject not found", HttpStatus.NOT_FOUND),
    INVALID_REFRESH_TOKEN("AUTH_005", "Refresh token is invalid or has expired", HttpStatus.UNAUTHORIZED);
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
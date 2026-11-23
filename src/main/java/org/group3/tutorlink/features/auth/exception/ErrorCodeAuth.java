package org.group3.tutorlink.features.auth.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCodeAuth implements BaseErrorCode {

    ACCOUNT_BANNED("AUTH_403", "Your account has been banned", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_FOUND("AUTH_404", "Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_LOCKED("AUTH_423", "Your account is locked", HttpStatus.FORBIDDEN),
    WRONG_CURRENT_PASSWORD("AUTH_400", "Wrong current password", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_INVALID("AUTH_VERIFY_001", "Verification code is incorrect", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_EXPIRED("AUTH_VERIFY_002", "Verification code has expired or was not requested", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOO_MANY_ATTEMPTS("AUTH_VERIFY_003", "Too many incorrect attempts, please request a new code", HttpStatus.TOO_MANY_REQUESTS),
    VERIFICATION_RESEND_TOO_SOON("AUTH_VERIFY_004", "Please wait before requesting another code", HttpStatus.TOO_MANY_REQUESTS)
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    ErrorCodeAuth(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

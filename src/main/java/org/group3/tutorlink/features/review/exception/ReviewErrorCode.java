package org.group3.tutorlink.features.review.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum ReviewErrorCode implements BaseErrorCode {
    NOT_FOUND("REVIEW_001", "Review not found", HttpStatus.NOT_FOUND),
    APPLICATION_NOT_FOUND("REVIEW_002", "Application not found", HttpStatus.NOT_FOUND),
    NOT_ALLOWED("REVIEW_003", "You are not allowed to perform this action", HttpStatus.FORBIDDEN),
    INVALID_APPLICATION_STATUS("REVIEW_004", "Application must be finished before reviewing", HttpStatus.BAD_REQUEST),
    ALREADY_EXISTS("REVIEW_005", "This application has already been reviewed", HttpStatus.CONFLICT),
    INVALID_TUTOR_ID("REVIEW_006", "Tutor id is required", HttpStatus.BAD_REQUEST),
    INVALID_PAGINATION("REVIEW_007", "Limit must be between 1 and 100", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ReviewErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

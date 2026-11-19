package org.group3.tutorlink.features.statistics.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum StatisticsErrorCode implements BaseErrorCode {

    INVALID_DATE_RANGE("STATISTICS_001", "Both 'from' and 'to' are required (yyyy-MM-dd) and 'from' must not be after 'to'!", HttpStatus.BAD_REQUEST),
    ADMIN_NOT_FOUND("STATISTICS_002", "Admin account performing this action was not found!", HttpStatus.NOT_FOUND),
    RANGE_TOO_LARGE("STATISTICS_003", "Daily statistics only support a range of up to 366 days!", HttpStatus.BAD_REQUEST)
    ;
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    StatisticsErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

package org.group3.tutorlink.features.payment.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum PaymentErrorCode implements BaseErrorCode {

    TUTOR_NOT_FOUND("PAYMENT_404", "Tutor not found!", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    PaymentErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

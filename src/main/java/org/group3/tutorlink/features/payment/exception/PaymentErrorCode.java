package org.group3.tutorlink.features.payment.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum PaymentErrorCode implements BaseErrorCode {

    TUTOR_NOT_FOUND("PAYMENT_404", "Tutor not found!", HttpStatus.NOT_FOUND),
    APPLICATION_NOT_FOUND("PAYMENT_404", "Application not found!", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND("PAYMENT_404", "Transaction not found!", HttpStatus.NOT_FOUND),
    NOT_APPLICATION_TUTOR("PAYMENT_403", "You are not the tutor of this application!", HttpStatus.FORBIDDEN),
    APPLICATION_NOT_PAYABLE("PAYMENT_409", "Commission can only be paid for an accepted or finished application!", HttpStatus.CONFLICT),
    APPLICATION_ALREADY_PAID("PAYMENT_409", "Commission of this application has already been paid!", HttpStatus.CONFLICT),
    TRANSACTION_NOT_REFUNDABLE("PAYMENT_409", "Only a successful commission transaction can be refunded!", HttpStatus.CONFLICT),
    POST_BUDGET_MISSING("PAYMENT_400", "Post budget is missing, cannot calculate commission!", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE("PAYMENT_400", "Wallet balance is not enough to pay the commission!", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_FILTER("PAYMENT_400", "Status must be one of: success, failed, refund!", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE("PAYMENT_400", "Both 'from' and 'to' are required (yyyy-MM-dd) and 'from' must not be after 'to'!", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    PaymentErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

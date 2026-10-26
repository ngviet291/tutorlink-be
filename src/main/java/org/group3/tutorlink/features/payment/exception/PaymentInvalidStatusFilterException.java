package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentInvalidStatusFilterException extends AppException {
    public PaymentInvalidStatusFilterException() {
        super(PaymentErrorCode.INVALID_STATUS_FILTER);
    }
}

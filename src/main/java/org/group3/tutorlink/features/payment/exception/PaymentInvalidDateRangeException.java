package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentInvalidDateRangeException extends AppException {
    public PaymentInvalidDateRangeException() {
        super(PaymentErrorCode.INVALID_DATE_RANGE);
    }
}

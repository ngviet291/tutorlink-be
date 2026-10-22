package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentApplicationNotFoundException extends AppException {
    public PaymentApplicationNotFoundException() {
        super(PaymentErrorCode.APPLICATION_NOT_FOUND);
    }
}

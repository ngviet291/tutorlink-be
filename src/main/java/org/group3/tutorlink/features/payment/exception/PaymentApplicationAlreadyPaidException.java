package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentApplicationAlreadyPaidException extends AppException {
    public PaymentApplicationAlreadyPaidException() {
        super(PaymentErrorCode.APPLICATION_ALREADY_PAID);
    }
}

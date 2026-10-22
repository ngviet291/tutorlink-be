package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentApplicationNotPayableException extends AppException {
    public PaymentApplicationNotPayableException() {
        super(PaymentErrorCode.APPLICATION_NOT_PAYABLE);
    }
}

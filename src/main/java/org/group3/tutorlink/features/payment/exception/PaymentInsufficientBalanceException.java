package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentInsufficientBalanceException extends AppException {
    public PaymentInsufficientBalanceException() {
        super(PaymentErrorCode.INSUFFICIENT_BALANCE);
    }
}

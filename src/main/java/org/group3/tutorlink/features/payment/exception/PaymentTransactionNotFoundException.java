package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentTransactionNotFoundException extends AppException {
    public PaymentTransactionNotFoundException() {
        super(PaymentErrorCode.TRANSACTION_NOT_FOUND);
    }
}

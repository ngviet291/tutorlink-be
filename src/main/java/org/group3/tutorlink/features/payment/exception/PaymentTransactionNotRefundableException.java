package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentTransactionNotRefundableException extends AppException {
    public PaymentTransactionNotRefundableException() {
        super(PaymentErrorCode.TRANSACTION_NOT_REFUNDABLE);
    }
}

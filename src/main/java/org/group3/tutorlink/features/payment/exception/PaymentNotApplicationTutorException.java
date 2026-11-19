package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentNotApplicationTutorException extends AppException {
    public PaymentNotApplicationTutorException() {
        super(PaymentErrorCode.NOT_APPLICATION_TUTOR);
    }
}

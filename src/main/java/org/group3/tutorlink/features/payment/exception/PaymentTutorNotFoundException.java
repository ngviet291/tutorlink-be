package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentTutorNotFoundException extends AppException {
    public PaymentTutorNotFoundException() {
        super(PaymentErrorCode.TUTOR_NOT_FOUND);
    }
}

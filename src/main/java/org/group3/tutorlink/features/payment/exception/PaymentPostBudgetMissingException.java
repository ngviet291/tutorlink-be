package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PaymentPostBudgetMissingException extends AppException {
    public PaymentPostBudgetMissingException() {
        super(PaymentErrorCode.POST_BUDGET_MISSING);
    }
}

package org.group3.tutorlink.features.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
@RequiredArgsConstructor
public enum PaymentResponseCode implements ResponseCode {

    GET_WALLET_BALANCE("PAYMENT_200", "Wallet balance retrieved successfully!"),
    TOP_UP_SUCCESS("PAYMENT_201", "Wallet topped up successfully!"),
    PAY_COMMISSION_SUCCESS("PAYMENT_201", "Commission paid successfully!"),
    GET_PAYMENT_HISTORY("PAYMENT_200", "Payment history retrieved successfully!"),
    GET_PAYMENTS("PAYMENT_200", "Payments retrieved successfully!"),
    REFUND_SUCCESS("PAYMENT_200", "Payment refunded successfully!"),
    GET_REVENUE_STATISTICS("PAYMENT_200", "Revenue statistics retrieved successfully!");

    private final String code;
    private final String message;
}

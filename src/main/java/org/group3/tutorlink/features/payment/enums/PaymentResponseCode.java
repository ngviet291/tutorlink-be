package org.group3.tutorlink.features.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
@RequiredArgsConstructor
public enum PaymentResponseCode implements ResponseCode {

    GET_WALLET_BALANCE("PAYMENT_200", "Wallet balance retrieved successfully!");

    private final String code;
    private final String message;
}

package org.group3.tutorlink.features.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VietQrCallbackCode implements BaseErrorCode {

    TOKEN_GENERATED(HttpStatus.OK, "Token generated successfully"),
    TRANSACTION_PROCESSED(HttpStatus.OK, "Transaction processed successfully"),
    IGNORED_NOT_CREDIT(HttpStatus.OK, "Ignored: not a credit transaction"),

    CREDENTIALS_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE,
            "VietQR callback credentials are not configured"),
    INVALID_AUTH_HEADER(HttpStatus.BAD_REQUEST,
            "Authorization header is missing or invalid"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired token"),
    INVALID_PAYLOAD(HttpStatus.BAD_REQUEST, "Request body is not valid JSON"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND,  "Order not found"),
    TRANSACTION_FAILED(HttpStatus.BAD_REQUEST, "Transaction failed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,  "Internal error");

    private final HttpStatus httpStatus;
    private final String message;
}
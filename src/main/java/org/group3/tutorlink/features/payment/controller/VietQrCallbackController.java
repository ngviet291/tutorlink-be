package org.group3.tutorlink.features.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.constant.VietQrConstants;
import org.group3.tutorlink.features.payment.dto.response.VietQrCallbackResponse;
import org.group3.tutorlink.features.payment.dto.response.VietQrTokenResponse;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.PaymentResponseCode;
import org.group3.tutorlink.features.payment.enums.VietQrCallbackCode;
import org.group3.tutorlink.features.payment.exception.VietQrCallbackException;
import org.group3.tutorlink.features.payment.gateway.GatewayHttp;
import org.group3.tutorlink.features.payment.gateway.vietqr.VietQrCallbackAuth;
import org.group3.tutorlink.features.payment.gateway.vietqr.VietQrGateway;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Hai API do VietQR gọi vào (server-to-server, không có JWT người dùng).
 * Tách khỏi SecurityConfig chính bằng VietQrSecurityConfig; tự xác thực bằng Basic/Bearer.
 */
@Slf4j
@RestController
@RequestMapping("/vqr")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.vietqr.enabled", havingValue = "true")
public class VietQrCallbackController {

    private final VietQrCallbackAuth auth;
    private final PaymentService paymentService;

    // 1. VietQR xin token: Basic auth -> JWT ngắn hạn
    @PostMapping("/api/token_generate")
    public VietQrTokenResponse generateToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (!auth.isConfigured()) {
            throw new VietQrCallbackException(VietQrCallbackCode.CREDENTIALS_NOT_CONFIGURED);
        }
        if (authHeader == null || !authHeader.startsWith(VietQrConstants.BASIC_PREFIX)) {
            throw new VietQrCallbackException(VietQrCallbackCode.INVALID_AUTH_HEADER);
        }
        if (!auth.isBasicAuthValid(authHeader)) {
            throw new VietQrCallbackException(VietQrCallbackCode.INVALID_CREDENTIALS);
        }

        // VietQR đọc access_token/token_type/expires_in ở cấp cao nhất, không bọc ApiResponse.
        return VietQrTokenResponse.builder()
                .accessToken(auth.issueToken())
                .tokenType(VietQrConstants.TOKEN_TYPE_BEARER)
                .expiresIn(auth.getTokenTtlSeconds())
                .build();
    }

    // 2. VietQR báo biến động số dư -> cộng ví cho giao dịch nạp tương ứng
    @PostMapping("/bank/api/transaction-sync")
    public VietQrCallbackResponse transactionSync(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody String rawBody) {
        if (!auth.isBearerTokenValid(authHeader)) {
            throw new VietQrCallbackException(VietQrCallbackCode.INVALID_TOKEN);
        }

        Map<String, Object> payload = parsePayload(rawBody);
        String orderId = VietQrGateway.field(payload,
                VietQrConstants.ORDER_ID_KEY, VietQrConstants.ORDER_ID_KEY_LOWER);

        if (!VietQrGateway.isCredit(payload)) {
            // Tiền ra khỏi tài khoản: không liên quan nạp ví.
            log.info("Ignore non-credit VietQR notification, orderId={}", orderId);
            return VietQrCallbackResponse.success(
                    PaymentResponseCode.VIETQR_IGNORED_NOT_CREDIT.getMessage(), orderId);
        }

        // Luồng chung: khóa giao dịch, đối chiếu số tiền, cộng ví một lần, bỏ qua nếu đã xử lý.
        paymentService.handleGatewayCallback(PaymentMethod.VIETQR, rawBody);
        return VietQrCallbackResponse.success(
                PaymentResponseCode.VIETQR_TRANSACTION_PROCESSED.getMessage(), orderId);
    }

    private Map<String, Object> parsePayload(String rawBody) {
        try {
            return GatewayHttp.parse(rawBody);
        } catch (AppException ex) {
            throw new VietQrCallbackException(VietQrCallbackCode.INTERNAL_ERROR);
        }
    }

    /** Lỗi của hai API này trả đúng định dạng VietQR {error, errorReason, toastMessage, object}. */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<VietQrCallbackResponse> handleError(AppException ex) {
        log.warn("VietQR callback error: {}", ex.getErrorCode().getMessage());
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                .body(VietQrCallbackResponse.failure(
                        ex.getErrorCode().getMessage(), ex.getErrorCode().getMessage()));
    }
}

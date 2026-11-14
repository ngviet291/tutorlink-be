package org.group3.tutorlink.features.payment.gateway.momo;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.gateway.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tích hợp MoMo Payment Gateway - "Thanh toán một lần" (captureWallet) - API v2.
 * 
 * Luồng thanh toán:
 * 1. Backend tạo đơn hàng với MoMo -> nhận payUrl
 * 2. Frontend redirect user sang payUrl để thanh toán
 * 3. User thanh toán trên MoMo (ATM/Visa/MoMo wallet)
 * 4. MoMo gọi IPN (server-to-server callback) để thông báo kết quả
 * 5. Backend verify chữ ký + cộng tiền vào ví user
 * 6. MoMo redirect user về frontend với kết quả
 * 
 * Tài liệu: https://developers.momo.vn
 */
@Slf4j
@Component
// Giữ gateway QR MoMo làm mặc định; cờ demo không thay profile của ứng dụng.
@ConditionalOnProperty(name = "payment.demo.enabled", havingValue = "false", matchIfMissing = true)
public class MomoGateway implements PaymentGateway {

    // One-time MoMo wallet payment.
    private static final String REQUEST_TYPE = "captureWallet";
    
    private static final String IPN_PATH = "/v1/payments/momo/ipn";

    // MoMo result codes that indicate payment is still in progress
    // 1000: initialized, 7000/7002: processing, 9000: authorized -> not final result yet
    private static final Set<Integer> PENDING_CODES = Set.of(1000, 7000, 7002, 9000);

    @Value("${MOMO_BASE_URL:}")
    private String baseUrl;

    @Value("${MOMO_ENDPOINT_CREATE:}")
    private String endpointCreate;

    @Value("${MOMO_ENDPOINT_QUERY:}")
    private String endpointQuery;

    @Value("${MOMO_PARTNER_CODE:}")
    private String partnerCode;

    @Value("${MOMO_ACCESS_KEY:}")
    private String accessKey;

    @Value("${MOMO_SECRET_KEY:}")
    private String secretKey;

    @Value("${MOMO_IPN_BASE_URL:}")
    private String ipnBaseUrl;

    @Value("${MOMO_REDIRECT_URL:}")
    private String redirectUrl;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOMO;
    }

    @Override
    public String newProviderOrderId() {
        // Generate unique order ID with "TL" prefix (TutorLink)
        return "TL" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public GatewayPaymentResult createPayment(GatewayPaymentRequest request) {
        validateConfiguration();
        String requestId = UUID.randomUUID().toString();
        String amount = request.amount().setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        String orderInfo = request.description();
        String extraData = "";
        String ipnUrl = ipnBaseUrl.replaceAll("/+$", "") + IPN_PATH;

        // Build signature - fields must be in alphabetical order as per MoMo documentation
        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + request.providerOrderId()
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + REQUEST_TYPE;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestId", requestId);
        body.put("amount", Long.parseLong(amount));
        body.put("orderId", request.providerOrderId());
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("requestType", REQUEST_TYPE);
        body.put("extraData", extraData);
        body.put("autoCapture", true);
        String signature = HmacUtil.hmacSha256Hex(secretKey, rawSignature);
        body.put("lang", "vi");
        body.put("signature", signature);

        Map<String, Object> response = GatewayHttp.postJson(createEndpoint(), body);

        Integer resultCode = GatewayHttp.intValue(response, "resultCode");
        String payUrl = GatewayHttp.str(response, "payUrl");
        if (resultCode == null) {
            throw GatewayHttp.diagnostic(GatewayHttp.Failure.INVALID_RESPONSE, null, null);
        }
        if (resultCode != 0) {
            throw GatewayHttp.diagnostic(GatewayHttp.Failure.MOMO_REJECTED, null, resultCode);
        }
        if (payUrl.isBlank()) {
            throw GatewayHttp.diagnostic(GatewayHttp.Failure.MISSING_PAYMENT_LINK, null, resultCode);
        }

        return new GatewayPaymentResult(
                payUrl,
                emptyToNull(GatewayHttp.str(response, "qrCodeUrl")),
                emptyToNull(GatewayHttp.str(response, "deeplink")));
    }

    @Override
    public GatewayCallbackResult verifyCallback(String rawBody) {
        validateConfiguration();
        Map<String, Object> ipn = GatewayHttp.parse(rawBody);

        // Verify signature - fields must be in alphabetical order
        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + GatewayHttp.str(ipn, "amount")
                + "&extraData=" + GatewayHttp.str(ipn, "extraData")
                + "&message=" + GatewayHttp.str(ipn, "message")
                + "&orderId=" + GatewayHttp.str(ipn, "orderId")
                + "&orderInfo=" + GatewayHttp.str(ipn, "orderInfo")
                + "&orderType=" + GatewayHttp.str(ipn, "orderType")
                + "&partnerCode=" + GatewayHttp.str(ipn, "partnerCode")
                + "&payType=" + GatewayHttp.str(ipn, "payType")
                + "&requestId=" + GatewayHttp.str(ipn, "requestId")
                + "&responseTime=" + GatewayHttp.str(ipn, "responseTime")
                + "&resultCode=" + GatewayHttp.str(ipn, "resultCode")
                + "&transId=" + GatewayHttp.str(ipn, "transId");

        String expected = HmacUtil.hmacSha256Hex(secretKey, rawSignature);
        if (!HmacUtil.secureEquals(expected, GatewayHttp.str(ipn, "signature"))) {
            log.warn("MoMo IPN invalid signature, orderId={}", GatewayHttp.str(ipn, "orderId"));
            throw new AppException(PaymentErrorCode.INVALID_GATEWAY_SIGNATURE);
        }

        validateResult(ipn, null);
        return new GatewayCallbackResult(
                GatewayHttp.str(ipn, "orderId"),
                GatewayHttp.str(ipn, "transId"),
                GatewayHttp.decimal(ipn, "amount"),
                toState(GatewayHttp.intValue(ipn, "resultCode")));
    }

    @Override
    public GatewayCallbackResult queryOrder(String providerOrderId) {
        validateConfiguration();
        String requestId = UUID.randomUUID().toString();
        
        // Build signature for query API
        String rawSignature = "accessKey=" + accessKey
                + "&orderId=" + providerOrderId
                + "&partnerCode=" + partnerCode
                + "&requestId=" + requestId;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestId", requestId);
        body.put("orderId", providerOrderId);
        body.put("lang", "vi");
        body.put("signature", HmacUtil.hmacSha256Hex(secretKey, rawSignature));

        Map<String, Object> response = GatewayHttp.postJson(queryEndpoint(), body);

        validateResult(response, providerOrderId);
        return new GatewayCallbackResult(
                providerOrderId,
                GatewayHttp.str(response, "transId"),
                GatewayHttp.decimal(response, "amount"),
                toState(GatewayHttp.intValue(response, "resultCode")));
    }

    private void validateConfiguration() {
        if (partnerCode == null || partnerCode.isBlank()
                || accessKey == null || accessKey.isBlank()
                || secretKey == null || secretKey.isBlank()) {
            throw new AppException(PaymentErrorCode.GATEWAY_NOT_CONFIGURED);
        }
        validateHttpsUrl(createEndpoint(), PaymentErrorCode.GATEWAY_ENDPOINT_INVALID);
        validateHttpsUrl(queryEndpoint(), PaymentErrorCode.GATEWAY_ENDPOINT_INVALID);
        validateHttpsUrl(ipnBaseUrl, PaymentErrorCode.GATEWAY_IPN_INVALID);
        try {
            java.net.URI redirect = java.net.URI.create(redirectUrl);
            boolean localHttp = "http".equalsIgnoreCase(redirect.getScheme())
                    && ("localhost".equalsIgnoreCase(redirect.getHost())
                        || "127.0.0.1".equals(redirect.getHost()));
            if ((!"https".equalsIgnoreCase(redirect.getScheme()) && !localHttp)
                    || redirect.getHost() == null || redirect.getUserInfo() != null
                    || redirect.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new AppException(PaymentErrorCode.GATEWAY_REDIRECT_INVALID);
        }
    }

    private String createEndpoint() {
        return endpointCreate == null || endpointCreate.isBlank()
                ? baseUrl.replaceAll("/+$", "") + "/v2/gateway/api/create" : endpointCreate;
    }

    private String queryEndpoint() {
        return endpointQuery == null || endpointQuery.isBlank()
                ? baseUrl.replaceAll("/+$", "") + "/v2/gateway/api/query" : endpointQuery;
    }

    private void validateHttpsUrl(String url, PaymentErrorCode errorCode) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new AppException(errorCode);
        }
    }

    private void validateResult(Map<String, Object> result, String expectedOrderId) {
        String orderId = GatewayHttp.str(result, "orderId");
        java.math.BigDecimal amount = GatewayHttp.decimal(result, "amount");
        if (!partnerCode.equals(GatewayHttp.str(result, "partnerCode"))
                || orderId.isBlank()
                || (expectedOrderId != null && !expectedOrderId.equals(orderId))
                || GatewayHttp.intValue(result, "resultCode") == null
                || amount == null || amount.signum() <= 0
                || (GatewayHttp.intValue(result, "resultCode") == 0
                    && GatewayHttp.str(result, "transId").isBlank())) {
            throw GatewayHttp.diagnostic(GatewayHttp.Failure.INVALID_RESPONSE, null, null);
        }
    }

    private GatewayPaymentState toState(Integer resultCode) {
        if (resultCode == null) {
            return GatewayPaymentState.PENDING;
        }
        // MoMo result code: 0 = success, others = pending or failed
        if (resultCode == 0) {
            return GatewayPaymentState.SUCCESS;
        }
        return PENDING_CODES.contains(resultCode) ? GatewayPaymentState.PENDING : GatewayPaymentState.FAILED;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}


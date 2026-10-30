package org.group3.tutorlink.features.payment.gateway.momo;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.gateway.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tích hợp MoMo Payment Gateway - "Thanh toán một lần" (payWithATM) - API v2.
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
public class MomoGateway implements PaymentGateway {

    // Payment request type - ATM/Visa payment method
    // For QR code payment: use "captureWallet" (requires MoMo test app)
    private static final String REQUEST_TYPE = "payWithATM";
    
    private static final String IPN_PATH = "/v1/payments/momo/ipn";

    // MoMo result codes that indicate payment is still in progress
    // 1000: initialized, 7000/7002: processing, 9000: authorized -> not final result yet
    private static final Set<Integer> PENDING_CODES = Set.of(1000, 7000, 7002, 9000);

    @Value("${payment.momo.base-url:https://payment.momo.vn}")
    private String baseUrl;

    @Value("${payment.momo.endpoint-create:https://payment.momo.vn/v2/gateway/api/create}")
    private String endpointCreate;

    @Value("${payment.momo.endpoint-query:https://payment.momo.vn/v2/gateway/api/query}")
    private String endpointQuery;

    @Value("${payment.momo.partner-code:}")
    private String partnerCode;

    @Value("${payment.momo.access-key:}")
    private String accessKey;

    @Value("${payment.momo.secret-key:}")
    private String secretKey;

    @Value("${payment.ipn-base-url:http://localhost:8080/api}")
    private String ipnBaseUrl;

    @Value("${payment.redirect-url:http://localhost:5173/wallet/top-up/result}")
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
        String requestId = UUID.randomUUID().toString();
        String amount = request.amount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        String orderInfo = request.description();
        String extraData = "";
        String ipnUrl = ipnBaseUrl + IPN_PATH;

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

        log.info("🔵 MoMo Request URL: {}", endpointCreate);
        log.info("🔵 MoMo Raw Signature String: {}", rawSignature);
        log.info("🔵 MoMo Signature: {}", signature);
        log.info("🔵 MoMo Request Body: {}", body);
        
        Map<String, Object> response = GatewayHttp.postJson(endpointCreate, body);
        
        log.info("🔵 MoMo Response: {}", response);

        Integer resultCode = GatewayHttp.intValue(response, "resultCode");
        String payUrl = GatewayHttp.str(response, "payUrl");
        if (resultCode == null || resultCode != 0 || payUrl.isBlank()) {
            log.error("❌ MoMo create order failed: resultCode={}, message={}, localMessage={}", 
                resultCode, 
                GatewayHttp.str(response, "message"),
                GatewayHttp.str(response, "localMessage"));
            throw new AppException(PaymentErrorCode.GATEWAY_ERROR);
        }

        return new GatewayPaymentResult(
                payUrl,
                emptyToNull(GatewayHttp.str(response, "qrCodeUrl")),
                emptyToNull(GatewayHttp.str(response, "deeplink")));
    }

    @Override
    public GatewayCallbackResult verifyCallback(String rawBody) {
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

        return new GatewayCallbackResult(
                GatewayHttp.str(ipn, "orderId"),
                GatewayHttp.str(ipn, "transId"),
                GatewayHttp.decimal(ipn, "amount"),
                toState(GatewayHttp.intValue(ipn, "resultCode")));
    }

    @Override
    public GatewayCallbackResult queryOrder(String providerOrderId) {
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

        Map<String, Object> response = GatewayHttp.postJson(endpointQuery, body);

        return new GatewayCallbackResult(
                providerOrderId,
                GatewayHttp.str(response, "transId"),
                GatewayHttp.decimal(response, "amount"),
                toState(GatewayHttp.intValue(response, "resultCode")));
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

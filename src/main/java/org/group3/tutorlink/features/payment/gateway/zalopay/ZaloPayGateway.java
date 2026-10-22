package org.group3.tutorlink.features.payment.gateway.zalopay;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.gateway.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tích hợp ZaloPay Gateway (v2/create).
 * Luồng: tạo đơn -> redirect user sang order_url -> ZaloPay gọi callback (server-to-server, ký bằng key2) -> cộng tiền vào ví.
 */
@Slf4j
@Component
public class ZaloPayGateway implements PaymentGateway {

    private static final String CALLBACK_PATH = "/v1/payments/zalopay/callback";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyMMdd");

    @Value("${payment.zalopay.base-url:https://sb-openapi.zalopay.vn}")
    private String baseUrl;

    @Value("${payment.zalopay.app-id:}")
    private String appId;

    @Value("${payment.zalopay.key1:}")
    private String key1;

    @Value("${payment.zalopay.key2:}")
    private String key2;

    @Value("${payment.ipn-base-url:http://localhost:8080/api}")
    private String ipnBaseUrl;

    @Value("${payment.redirect-url:http://localhost:5173/wallet/top-up/result}")
    private String redirectUrl;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.ZALOPAY;
    }

    @Override
    public String newProviderOrderId() {
        return LocalDate.now(VN_ZONE).format(DATE_PREFIX) + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Override
    public GatewayPaymentResult createPayment(GatewayPaymentRequest request) {
        String amount = request.amount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        String appUser = request.tutorId().toString();
        String appTime = String.valueOf(System.currentTimeMillis());
        String embedData = "{\"redirecturl\":\"" + redirectUrl + "\"}";
        String item = "[]";

        // mac = HMAC_SHA256(key1, app_id|app_trans_id|app_user|amount|app_time|embed_data|item)
        String macInput = appId + "|" + request.providerOrderId() + "|" + appUser + "|" + amount
                + "|" + appTime + "|" + embedData + "|" + item;

        log.info("ZaloPay createPayment macInput: {}", macInput);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("app_id", appId);
        form.put("app_user", appUser);
        form.put("app_time", appTime);
        form.put("amount", amount);
        form.put("app_trans_id", request.providerOrderId());
        form.put("embed_data", embedData);
        form.put("item", item);
        form.put("description", request.description());
        form.put("bank_code", "");
        form.put("callback_url", ipnBaseUrl + CALLBACK_PATH);
        form.put("mac", HmacUtil.hmacSha256Hex(key1, macInput));

        Map<String, Object> response = GatewayHttp.postForm(baseUrl + "/v2/create", form);

        Integer returnCode = GatewayHttp.intValue(response, "return_code");
        String orderUrl = GatewayHttp.str(response, "order_url");
        if (returnCode == null || returnCode != 1 || orderUrl.isBlank()) {
            log.error("ZaloPay create order failed: return_code={}, message={}, sub={}",
                    returnCode, GatewayHttp.str(response, "return_message"), GatewayHttp.str(response, "sub_return_message"));
            throw new AppException(PaymentErrorCode.GATEWAY_ERROR);
        }

        // order_url dùng để chuyển hướng user hoặc để FE tự render QR; ZaloPay không trả ảnh QR riêng
        return new GatewayPaymentResult(orderUrl, null, null);
    }

    @Override
    public GatewayCallbackResult verifyCallback(String rawBody) {
        Map<String, Object> envelope = GatewayHttp.parse(rawBody);
        String data = GatewayHttp.str(envelope, "data");

        // Callback ký bằng key2: mac = HMAC_SHA256(key2, data)
        String expected = HmacUtil.hmacSha256Hex(key2, data);
        if (data.isBlank() || !HmacUtil.secureEquals(expected, GatewayHttp.str(envelope, "mac"))) {
            log.warn("ZaloPay callback invalid mac");
            throw new AppException(PaymentErrorCode.INVALID_GATEWAY_SIGNATURE);
        }

        Map<String, Object> payload = GatewayHttp.parse(data);

        // ZaloPay chỉ gọi callback khi thanh toán thành công
        return new GatewayCallbackResult(
                GatewayHttp.str(payload, "app_trans_id"),
                GatewayHttp.str(payload, "zp_trans_id"),
                GatewayHttp.decimal(payload, "amount"),
                GatewayPaymentState.SUCCESS);
    }

    @Override
    public GatewayCallbackResult queryOrder(String providerOrderId) {
        // mac = HMAC_SHA256(key1, app_id|app_trans_id|key1)
        Map<String, String> form = new LinkedHashMap<>();
        form.put("app_id", appId);
        form.put("app_trans_id", providerOrderId);
        form.put("mac", HmacUtil.hmacSha256Hex(key1, appId + "|" + providerOrderId + "|" + key1));

        Map<String, Object> response = GatewayHttp.postForm(baseUrl + "/v2/query", form);

        Integer returnCode = GatewayHttp.intValue(response, "return_code");

        // Chỉ chốt SUCCESS; các trường hợp còn lại coi là chưa có kết quả cuối để tránh đánh FAILED nhầm
        // khi user vẫn còn thanh toán được (đơn tự hết hạn phía ZaloPay).
        GatewayPaymentState state = returnCode != null && returnCode == 1
                ? GatewayPaymentState.SUCCESS
                : GatewayPaymentState.PENDING;

        return new GatewayCallbackResult(
                providerOrderId,
                GatewayHttp.str(response, "zp_trans_id"),
                GatewayHttp.decimal(response, "amount"),
                state);
    }
}

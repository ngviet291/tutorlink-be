package org.group3.tutorlink.features.payment.gateway.vietqr;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.gateway.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tích hợp VietQR - nạp ví bằng chuyển khoản QR động.
 *
 * Luồng:
 * 1. Backend lấy token (Basic auth bằng tài khoản VietQR cấp) rồi gọi generate-customer với orderId + amount.
 * 2. Giao diện hiển thị QR; người dùng quét và chuyển khoản bằng app ngân hàng.
 * 3. VietQR gọi POST /vqr/bank/api/transaction-sync (xem VietQrCallbackController) khi tiền về tài khoản.
 *    Người gọi được xác thực bằng Bearer token do chính backend cấp (VietQrCallbackAuth), nên
 *    verifyCallback ở đây chỉ đọc và kiểm tra dữ liệu, không kiểm tra chữ ký.
 * 4. PaymentServiceImpl.settleTopUp đối chiếu số tiền rồi cộng ví đúng một lần.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.vietqr.enabled", havingValue = "true")
public class VietQrGateway implements PaymentGateway {

    // VietQR giới hạn orderId và content tối đa 19 ký tự, chữ Latin không dấu, không ký tự đặc biệt.
    private static final String ORDER_PREFIX = "TL";
    private static final int ORDER_ID_LENGTH = 19;
    private static final char[] ORDER_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String TOKEN_PATH = "/api/token_generate";
    private static final String GENERATE_QR_PATH = "/api/qr/generate-customer";
    private static final String CHECK_ORDER_PATH = "/api/ecommerce-transactions/check-order";
    // check-order: status 1 = đã thanh toán; type 0 = giao dịch VietQR (loại 6 là hoàn tiền).
    private static final int CHECK_ORDER_STATUS_PAID = 1;
    private static final int CHECK_ORDER_TYPE_VIETQR = 0;

    private static final long DEFAULT_TOKEN_TTL_SECONDS = 240;
    private static final long TOKEN_EARLY_REFRESH_SECONDS = 30;

    @Value("${payment.vietqr.base-url:}")
    private String baseUrl;

    @Value("${payment.vietqr.username:}")
    private String username;

    @Value("${payment.vietqr.password:}")
    private String password;

    @Value("${payment.vietqr.bank-code:}")
    private String bankCode;

    @Value("${payment.vietqr.bank-account:}")
    private String bankAccount;

    @Value("${payment.vietqr.account-name:}")
    private String accountName;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.VIETQR;
    }

    @Override
    public String newProviderOrderId() {
        StringBuilder orderId = new StringBuilder(ORDER_ID_LENGTH).append(ORDER_PREFIX);
        while (orderId.length() < ORDER_ID_LENGTH) {
            orderId.append(ORDER_ALPHABET[RANDOM.nextInt(ORDER_ALPHABET.length)]);
        }
        return orderId.toString();
    }

    @Override
    public GatewayPaymentResult createPayment(GatewayPaymentRequest request) {
        validateConfiguration();
        long amount = request.getAmount().setScale(0, RoundingMode.UNNECESSARY).longValueExact();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bankCode", bankCode);
        body.put("bankAccount", bankAccount);
        body.put("userBankName", accountName);
        body.put("content", request.getProviderOrderId());
        body.put("qrType", 0); // QR động: gắn với 1 số tiền và 1 orderId
        body.put("amount", amount);
        body.put("orderId", request.getProviderOrderId());
        body.put("transType", "C");

        Map<String, Object> response = postAuthorized(baseUrl() + GENERATE_QR_PATH, body);

        String qrCode = GatewayHttp.str(response, "qrCode");
        if (qrCode.isBlank()) {
            throw GatewayHttp.diagnostic(GatewayHttp.Failure.MISSING_PAYMENT_LINK, null, null);
        }
        // payUrl: trang/ảnh QR do VietQR lưu; qrCodeUrl: nội dung QR để FE tự vẽ.
        return new GatewayPaymentResult(emptyToNull(GatewayHttp.str(response, "qrLink")), qrCode, null);
    }

    /**
     * Đọc thông báo biến động số dư của VietQR. Người gọi đã được xác thực ở controller bằng Bearer token.
     * Chỉ nhận giao dịch ghi có (transType = C) và phải có đủ orderId, mã giao dịch, số tiền.
     */
    @Override
    public GatewayCallbackResult verifyCallback(String rawBody) {
        Map<String, Object> payload = GatewayHttp.parse(rawBody);

        String orderId = field(payload, "orderId", "orderid");
        String transId = field(payload, "transactionid", "transactionId");
        BigDecimal amount = toAmount(field(payload, "amount"));

        if (!isCredit(payload) || orderId.isBlank() || transId.isBlank()
                || amount == null || amount.signum() <= 0) {
            throw new AppException(PaymentErrorCode.INVALID_GATEWAY_CALLBACK);
        }
        return new GatewayCallbackResult(orderId, transId, amount, GatewayPaymentState.SUCCESS);
    }

    /**
     * Chủ động hỏi VietQR (API check-order) khi callback chưa tới hoặc bị mất.
     * Chỉ coi là SUCCESS khi có giao dịch ghi có đã thanh toán (status = 1) đúng orderId;
     * mọi trường hợp khác (chưa trả, hết hạn, lỗi mạng/cấu hình) giữ PENDING để FE tiếp tục chờ.
     * Việc đối chiếu số tiền và cộng ví một lần vẫn do PaymentServiceImpl.settleTopUp đảm nhiệm.
     */
    @Override
    public GatewayCallbackResult queryOrder(String providerOrderId) {
        GatewayCallbackResult pending =
                new GatewayCallbackResult(providerOrderId, null, null, GatewayPaymentState.PENDING);
        try {
            validateConfiguration();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("bankAccount", bankAccount);
            body.put("type", "0"); // 0 = tra theo orderId
            body.put("value", providerOrderId);
            body.put("checkSum", md5Hex(bankAccount + username));

            List<Map<String, Object>> transactions = GatewayHttp.postJsonArray(
                    baseUrl() + CHECK_ORDER_PATH,
                    Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + token()),
                    body);

            // Nhật ký tạm để đối chiếu phản hồi check-order (không chứa thông tin xác thực).
            log.info("VietQR check-order {} -> {} record(s): {}", providerOrderId, transactions.size(), transactions);
            for (Map<String, Object> tx : transactions) {
                Integer status = GatewayHttp.intValue(tx, "status");
                Integer type = GatewayHttp.intValue(tx, "type");
                if (status == null || status != CHECK_ORDER_STATUS_PAID
                        || (type != null && type != CHECK_ORDER_TYPE_VIETQR)
                        || !providerOrderId.equals(field(tx, "orderId"))
                        || (!field(tx, "transType", "transtype").isBlank() && !isCredit(tx))) {
                    continue;
                }
                String transId = field(tx, "referenceNumber");
                BigDecimal amount = toAmount(field(tx, "amount"));
                if (transId.isBlank() || amount == null || amount.signum() <= 0) {
                    continue;
                }
                return new GatewayCallbackResult(providerOrderId, transId, amount, GatewayPaymentState.SUCCESS);
            }
            return pending;
        } catch (AppException ex) {
            // Không làm hỏng polling của FE: lần sau hỏi lại (token hết hạn được xin mới ở postAuthorized/token()).
            invalidateToken();
            log.warn("VietQR check-order failed for {}: {}", providerOrderId, ex.getMessage());
            return pending;
        }
    }

    /** true nếu thông báo là giao dịch ghi có (tiền vào). */
    public static boolean isCredit(Map<String, Object> payload) {
        return "C".equalsIgnoreCase(field(payload, "transType", "transtype"));
    }

    /** Lấy giá trị theo tên trường đầu tiên có mặt (tài liệu VietQR không nhất quán hoa/thường). */
    public static String field(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            String value = GatewayHttp.str(payload, key);
            if (!value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static BigDecimal toAmount(String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ---- token & HTTP ----

    private Map<String, Object> postAuthorized(String url, Map<String, Object> body) {
        try {
            return GatewayHttp.postJson(url, Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + token()), body);
        } catch (AppException ex) {
            // Token có thể đã hết hạn phía VietQR: bỏ cache để lần gọi sau xin token mới.
            invalidateToken();
            throw ex;
        }
    }

    private synchronized String token() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }
        String basic = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> response = GatewayHttp.postJson(
                baseUrl() + TOKEN_PATH, Map.of(HttpHeaders.AUTHORIZATION, "Basic " + basic), null);

        String token = GatewayHttp.str(response, "access_token");
        if (token.isBlank()) {
            throw GatewayHttp.diagnostic(GatewayHttp.Failure.VIETQR_REJECTED, null, null);
        }
        Integer expiresIn = GatewayHttp.intValue(response, "expires_in");
        long ttl = expiresIn == null
                ? DEFAULT_TOKEN_TTL_SECONDS
                : Math.max(1, expiresIn - TOKEN_EARLY_REFRESH_SECONDS);

        accessToken = token;
        tokenExpiresAt = Instant.now().plusSeconds(ttl);
        return accessToken;
    }

    private static String md5Hex(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("MD5")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private synchronized void invalidateToken() {
        accessToken = null;
        tokenExpiresAt = Instant.EPOCH;
    }

    private String baseUrl() {
        return baseUrl.replaceAll("/+$", "");
    }

    private void validateConfiguration() {
        if (isBlank(username) || isBlank(password) || isBlank(bankCode)
                || isBlank(bankAccount) || isBlank(accountName)) {
            throw new AppException(PaymentErrorCode.GATEWAY_NOT_CONFIGURED);
        }
        try {
            java.net.URI uri = java.net.URI.create(baseUrl());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new AppException(PaymentErrorCode.GATEWAY_ENDPOINT_INVALID);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

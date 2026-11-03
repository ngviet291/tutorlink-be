package org.group3.tutorlink.features.payment.gateway;

import java.math.BigDecimal;

/**
 * Kết quả đã được xác thực chữ ký (từ IPN/callback hoặc từ API truy vấn đơn hàng).
 * amount có thể null (một số API truy vấn không trả số tiền).
 */
public record GatewayCallbackResult(
        String providerOrderId,
        String providerTransId,
        BigDecimal amount,
        GatewayPaymentState state
) {
}

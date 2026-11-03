package org.group3.tutorlink.features.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dữ liệu service gửi cho cổng thanh toán để tạo đơn nạp tiền.
 */
public record GatewayPaymentRequest(
        String providerOrderId,
        BigDecimal amount,
        String description,
        UUID tutorId
) {
}

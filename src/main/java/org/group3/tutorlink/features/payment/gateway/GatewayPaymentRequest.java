package org.group3.tutorlink.features.payment.gateway;

import lombok.Value;
import java.math.BigDecimal;
import java.util.UUID;

/** Dữ liệu nghiệp vụ gửi tới cổng thanh toán. */
@Value
public class GatewayPaymentRequest {
    String providerOrderId;
    BigDecimal amount;
    String description;
    UUID tutorId;
}

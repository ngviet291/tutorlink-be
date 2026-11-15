package org.group3.tutorlink.features.payment.gateway;

import lombok.Value;
import java.math.BigDecimal;

/** Kết quả đã xác thực từ thông báo hoặc truy vấn trạng thái thanh toán. */
@Value
public class GatewayCallbackResult {
    String providerOrderId;
    String providerTransId;
    BigDecimal amount;
    GatewayPaymentState state;
}

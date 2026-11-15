package org.group3.tutorlink.features.payment.gateway;

import lombok.Value;

/** Các liên kết do cổng thanh toán trả về cho giao diện. */
@Value
public class GatewayPaymentResult {
    String payUrl;
    String qrCodeUrl;
    String deeplink;
}

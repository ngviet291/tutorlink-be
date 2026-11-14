package org.group3.tutorlink.features.payment.gateway;

/**
 * Kết quả tạo đơn: client dùng payUrl để chuyển sang trang thanh toán MoMo.
 * qrCodeUrl và deeplink có thể null tùy cổng.
 */
public record GatewayPaymentResult(
        String payUrl,
        String qrCodeUrl,
        String deeplink
) {
}

package org.group3.tutorlink.features.payment.gateway;

import org.group3.tutorlink.features.payment.enums.PaymentMethod;

public interface PaymentGateway {

    PaymentMethod method();

    // Mã đơn hàng gửi sang cổng (MoMo: orderId, ZaloPay: app_trans_id)
    String newProviderOrderId();

    GatewayPaymentResult createPayment(GatewayPaymentRequest request);

    // Xác thực chữ ký IPN/callback rồi trả kết quả; sai chữ ký -> AppException(INVALID_GATEWAY_SIGNATURE)
    GatewayCallbackResult verifyCallback(String rawBody);

    // Chủ động hỏi cổng về trạng thái đơn (dùng khi chưa nhận được callback)
    GatewayCallbackResult queryOrder(String providerOrderId);
}

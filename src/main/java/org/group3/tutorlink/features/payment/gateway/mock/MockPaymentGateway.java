package org.group3.tutorlink.features.payment.gateway.mock;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.gateway.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock gateway để test payment flow không cần test app hay ngrok.
 * Transaction luôn SUCCESS ngay lập tức (giả lập thanh toán thành công).
 */
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOCK;  // Payment method mới
    }

    @Override
    public String newProviderOrderId() {
        return "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public GatewayPaymentResult createPayment(GatewayPaymentRequest request) {
        log.info("Mock payment created: orderId={}, amount={}", request.providerOrderId(), request.amount());
        
        // Mock: luôn trả success ngay lập tức (không cần redirect)
        return new GatewayPaymentResult(
                "mock://payment-success",  // payUrl (không dùng)
                null,
                null
        );
    }

    @Override
    public GatewayCallbackResult verifyCallback(String rawBody) {
        // Mock: không cần verify signature, chấp nhận tất cả
        log.info("Mock callback accepted");
        return new GatewayCallbackResult(
                "MOCK-ORDER",
                "MOCK-TRANS-" + UUID.randomUUID().toString().substring(0, 8),
                null,
                GatewayPaymentState.SUCCESS
        );
    }

    @Override
    public GatewayCallbackResult queryOrder(String providerOrderId) {
        log.info("Mock query: {}", providerOrderId);
        
        // Mock: luôn trả success
        return new GatewayCallbackResult(
                providerOrderId,
                "MOCK-TRANS-" + UUID.randomUUID().toString().substring(0, 8),
                null,
                GatewayPaymentState.SUCCESS
        );
    }
}

package org.group3.tutorlink.features.payment.gateway.mock;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.gateway.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Payment Gateway for development/testing without real payment provider.
 * 
 * Simulates payment flow:
 * 1. createPayment() returns a mock payment URL
 * 2. verifyCallback() always returns SUCCESS (simulates IPN)
 * 3. queryOrder() returns stored payment state
 * 
 * Enable by setting profile: dev-mock or add @Profile("dev-mock")
 */
@Slf4j
@Component
@Profile("dev-mock") // Only enable when explicitly set to dev-mock
public class MockPaymentGateway implements PaymentGateway {

    // In-memory storage for mock orders
    private final Map<String, MockOrder> orders = new ConcurrentHashMap<>();

    @Value("${payment.redirect-url:http://localhost:5173/wallet/top-up/result}")
    private String redirectUrl;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOMO; // Pretend to be MoMo
    }

    @Override
    public String newProviderOrderId() {
        return "MOCK" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public GatewayPaymentResult createPayment(GatewayPaymentRequest request) {
        String orderId = request.providerOrderId();
        
        // Store order in memory
        MockOrder order = new MockOrder(
                orderId,
                "MOCK_TRANS_" + System.currentTimeMillis(),
                request.amount(),
                GatewayPaymentState.PENDING
        );
        orders.put(orderId, order);

        log.info("🧪 MOCK: Created payment order={}, amount={}", orderId, request.amount());

        // Return mock payment URL
        String mockPayUrl = "http://localhost:8080/api/v1/payments/mock/pay?orderId=" + orderId;
        String mockQrUrl = "http://localhost:8080/api/v1/payments/mock/qr?orderId=" + orderId;
        
        return new GatewayPaymentResult(mockPayUrl, mockQrUrl, null);
    }

    @Override
    public GatewayCallbackResult verifyCallback(String rawBody) {
        // Parse mock IPN (assuming JSON with orderId field)
        Map<String, Object> ipn = GatewayHttp.parse(rawBody);
        String orderId = GatewayHttp.str(ipn, "orderId");
        
        MockOrder order = orders.get(orderId);
        if (order == null) {
            log.warn("🧪 MOCK: Unknown order in IPN: {}", orderId);
            return new GatewayCallbackResult(orderId, null, null, GatewayPaymentState.FAILED);
        }

        // Simulate successful payment
        order.state = GatewayPaymentState.SUCCESS;
        orders.put(orderId, order);

        log.info("🧪 MOCK: IPN callback - order={} -> SUCCESS", orderId);

        return new GatewayCallbackResult(
                orderId,
                order.transId,
                order.amount,
                GatewayPaymentState.SUCCESS
        );
    }

    @Override
    public GatewayCallbackResult queryOrder(String providerOrderId) {
        MockOrder order = orders.get(providerOrderId);
        
        if (order == null) {
            log.warn("🧪 MOCK: Query unknown order: {}", providerOrderId);
            return new GatewayCallbackResult(providerOrderId, null, null, GatewayPaymentState.FAILED);
        }

        // Auto-complete pending orders after query (simulate payment completion)
        if (order.state == GatewayPaymentState.PENDING) {
            order.state = GatewayPaymentState.SUCCESS;
            orders.put(providerOrderId, order);
            log.info("🧪 MOCK: Query order={} -> AUTO SUCCESS", providerOrderId);
        }

        return new GatewayCallbackResult(
                providerOrderId,
                order.transId,
                order.amount,
                order.state
        );
    }

    private static class MockOrder {
        String orderId;
        String transId;
        java.math.BigDecimal amount;
        GatewayPaymentState state;

        MockOrder(String orderId, String transId, java.math.BigDecimal amount, GatewayPaymentState state) {
            this.orderId = orderId;
            this.transId = transId;
            this.amount = amount;
            this.state = state;
        }
    }
}

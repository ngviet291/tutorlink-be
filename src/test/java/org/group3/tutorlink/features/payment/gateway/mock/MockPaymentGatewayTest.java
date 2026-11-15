package org.group3.tutorlink.features.payment.gateway.mock;

import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.gateway.GatewayPaymentRequest;
import org.group3.tutorlink.features.payment.gateway.GatewayPaymentState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentGatewayTest {
    @Test
    void pollingDoesNotPayAndExplicitConfirmationIsRepeatable() {
        var gateway = new MockPaymentGateway();
        String orderId = gateway.newProviderOrderId();
        var result = gateway.createPayment(new GatewayPaymentRequest(
                orderId, new BigDecimal("100000"), "Demo", UUID.randomUUID()));
        assertNull(result.getPayUrl());
        assertEquals(GatewayPaymentState.PENDING, gateway.queryOrder(orderId).getState());
        assertEquals(GatewayPaymentState.PENDING, gateway.queryOrder(orderId).getState());
        gateway.confirmDemo(orderId);
        var paid = gateway.queryOrder(orderId);
        gateway.confirmDemo(orderId);
        assertEquals(GatewayPaymentState.SUCCESS, paid.getState());
        assertEquals(paid, gateway.queryOrder(orderId));
        assertEquals(new BigDecimal("100000"), paid.getAmount());
    }

    @Test
    void publicCallbackCannotConfirmDemo() {
        var gateway = new MockPaymentGateway();
        String orderId = gateway.newProviderOrderId();
        gateway.createPayment(new GatewayPaymentRequest(
                orderId, new BigDecimal("100000"), "Demo", UUID.randomUUID()));
        assertThrows(AppException.class,
                () -> gateway.verifyCallback("{\"orderId\":\"" + orderId + "\"}"));
        assertEquals(GatewayPaymentState.PENDING, gateway.queryOrder(orderId).getState());
    }

    @Test
    void unknownOrderCannotBeConfirmed() {
        assertThrows(AppException.class, () -> new MockPaymentGateway().confirmDemo("unknown"));
    }
}
